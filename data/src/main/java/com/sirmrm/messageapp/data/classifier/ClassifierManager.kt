package com.sirmrm.messageapp.data.classifier

import android.provider.Telephony
import com.sirmrm.messageapp.classifier.ClassificationResult
import com.sirmrm.messageapp.classifier.ClassifierConfig
import com.sirmrm.messageapp.classifier.NaiveBayesLearner
import com.sirmrm.messageapp.classifier.SmsClassifier
import com.sirmrm.messageapp.classifier.SmsInput
import com.sirmrm.messageapp.classifier.SmsCategory
import com.sirmrm.messageapp.classifier.SenderRule
import com.sirmrm.messageapp.data.contacts.ContactsResolver
import com.sirmrm.messageapp.data.db.ConversationDao
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.NbDao
import com.sirmrm.messageapp.data.db.NbMetaEntity
import com.sirmrm.messageapp.data.db.NbTokenEntity
import com.sirmrm.messageapp.classifier.BankingPatterns
import com.sirmrm.messageapp.data.db.CustomBankPatternDao
import com.sirmrm.messageapp.data.db.SenderRuleDao
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wraps the pure-Kotlin [SmsClassifier] with:
 *  - settings-driven config (editable service-sender prefixes, sensitivity)
 *  - per-sender rules from Room
 *  - Naive Bayes persistence so on-device learning survives restarts
 */
@Singleton
class ClassifierManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val settingsRepository: SettingsRepository,
    private val senderRuleDao: SenderRuleDao,
    private val nbDao: NbDao,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val contactsResolver: ContactsResolver,
    private val customBankPatternDao: CustomBankPatternDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    @Volatile
    private var classifier: SmsClassifier = SmsClassifier()

    init {
        scope.launch { reload() }
    }

    /** Rebuild the classifier from current settings + persisted model. */
    suspend fun reload() = mutex.withLock {
        val settings = settingsRepository.settings.first()
        val patterns = settings.serviceSenderPatterns
            .ifEmpty { ClassifierConfig.DEFAULT_SERVICE_SENDER_PATTERNS }

        // Sensitivity slider shifts both thresholds together.
        val shift = (0.5f - settings.sensitivity) * 0.4f
        val config = ClassifierConfig(
            autoMoveThreshold = (ClassifierConfig().autoMoveThreshold + shift).coerceIn(0.5f, 0.95f),
            suggestThreshold = (ClassifierConfig().suggestThreshold + shift).coerceIn(0.15f, 0.6f),
            serviceSenderPatterns = patterns,
        )

        val bankingPatterns = loadBankingPatterns()
        val c = SmsClassifier(config, bankingPatterns = bankingPatterns)
        val meta = nbDao.meta()
        val tokens = nbDao.allTokens()
        if (meta != null || tokens.isNotEmpty()) {
            c.restore(
                NaiveBayesLearner.Snapshot(
                    promoTokenCounts = tokens.associate { it.token to it.promoCount },
                    hamTokenCounts = tokens.associate { it.token to it.hamCount },
                    promoDocs = meta?.promoDocs ?: 0,
                    hamDocs = meta?.hamDocs ?: 0,
                )
            )
        }
        classifier = c
    }

    suspend fun classify(address: String, body: String): ClassificationResult {
        val rule = senderRuleDao.get(normalizeAddress(address))
        val input = SmsInput(
            body = body,
            sender = address,
            isContact = isInContacts(address),
            hasConversation = hasReplies(address),
            senderRule = rule?.let { e ->
                when (e.decision) {
                    "ALWAYS_PROMO" -> SenderRule.ALWAYS_PROMO
                    "NEVER_PROMO" -> SenderRule.NEVER_PROMO
                    "ALWAYS_TX" -> SenderRule.ALWAYS_TX
                    "NEVER_TX" -> SenderRule.NEVER_TX
                    else -> if (e.alwaysPromo) SenderRule.ALWAYS_PROMO else SenderRule.NEVER_PROMO
                }
            },
            knownBankSender = isLearnedBankSender(address),
            customBankSender = isCustomBankSender(address),
        )
        return classifier.classify(input)
    }

    /** Loads the versioned bank patterns JSON from app assets. */
    private fun loadBankingPatterns(): BankingPatterns = runCatching {
        context.assets.open("patterns/banking_fa.json")
            .bufferedReader().use { it.readText() }
    }.mapCatching { BankingPatterns.load(it) }
        .getOrElse { BankingPatterns.fallback() }

    /**
     * Auto-learned bank senders: an address that already produced at least
     * `learned_sender_min_structured_messages` transactional messages counts
     * as a trusted bank sender for future scoring (weight, not a decision).
     */
    private suspend fun isLearnedBankSender(address: String): Boolean = runCatching {
        val normalized = normalizeAddress(address)
        messageDao.transactionalSenders()
            .firstOrNull { normalizeAddress(it.address) == normalized }
            ?.let { it.cnt >= 3 } ?: false
    }.getOrDefault(false)

    private suspend fun isCustomBankSender(address: String): Boolean = runCatching {
        val normalized = normalizeAddress(address)
        customBankPatternDao.all().any { e ->
            runCatching { Regex(e.pattern).containsMatchIn(normalized) }
                .getOrElse { normalized.contains(e.pattern) }
        }
    }.getOrDefault(false)

    /**
     * Classifies every message still marked UNKNOWN (e.g. freshly imported
     * provider rows) and applies the category folder to the message and its
     * conversation. Idempotent; safe to run on every app start.
     */
    suspend fun classifyUnclassified(): Int {
        // Oldest first so each conversation ends up reflecting its newest
        // classified message.
        val pending = messageDao.unclassifiedMessages().sortedBy { it.date }
        var moved = applyClassification(pending)

        // Protected re-check: messages previously marked PERSONAL may have
        // landed there only because the sender was a contact (e.g. a bank's
        // number saved in contacts). Re-evaluate and only move them when the
        // engine now proves a protected class — normal personal texts stay.
        val reCheck = messageDao.personalMessages()
        for (m in reCheck) {
            val result = classify(m.address, m.body)
            if (result.category.name != "OTP" && result.category.name != "TRANSACTIONAL") continue
            applyClassification(listOf(m))
            moved++
        }
        return moved
    }

    private suspend fun applyClassification(pending: List<com.sirmrm.messageapp.data.db.MessageEntity>): Int {
        for (m in pending) {
            val result = classify(m.address, m.body)
            val target = folderFor(result)
            val merged = mergeConversationFolder(m.folder, target)
            messageDao.upsert(
                m.copy(
                    folder = merged,
                    category = result.category.name,
                    promoScore = result.score,
                    reasonCodes = result.reasons.joinToString(";") { it.code },
                    txSubtype = result.bank?.subtype?.name,
                    txAmountRials = result.bank?.amountRials,
                    txBalanceRials = result.bank?.balanceRials,
                    bankName = result.bank?.bankName,
                    suspicious = result.suspicious,
                    maybeTransactional = result.maybeTransactional,
                )
            )
            conversationDao.get(m.threadId)?.let { conv ->
                val contact = contactsResolver.lookup(conv.address)
                conversationDao.upsert(
                    conv.copy(
                        displayName = contact?.displayName ?: conv.displayName,
                        photoUri = contact?.photoUri ?: conv.photoUri,
                        folder = mergeConversationFolder(conv.folder, target),
                        promoScore = result.score,
                        maybePromo = result.decision == com.sirmrm.messageapp.classifier.Decision.SUGGEST,
                    )
                )
            }
        }
        return pending.size
    }

    /** User feedback: learn + persist. */
    suspend fun learn(address: String, body: String, isPromo: Boolean) {
        mutex.withLock {
            classifier.learn(SmsInput(body = body, sender = address), isPromo)
            persist()
        }
    }

    private suspend fun persist() {
        val snap = classifier.snapshot()
        val entities = (snap.promoTokenCounts.keys + snap.hamTokenCounts.keys).map { t ->
            NbTokenEntity(t, snap.promoTokenCounts[t] ?: 0, snap.hamTokenCounts[t] ?: 0)
        }
        nbDao.upsertTokens(entities)
        nbDao.upsertMeta(NbMetaEntity(0, snap.promoDocs, snap.hamDocs))
    }

    private fun normalizeAddress(address: String): String {
        val key = com.sirmrm.messageapp.classifier.SenderKey.of(address)
        return key.ifBlank { address.trim() }
    }

    private fun isInContacts(address: String): Boolean {
        return try {
            val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon().appendPath(address).build()
            context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    /** Two-way conversation: any message the user sent to this address. */
    private fun hasReplies(address: String): Boolean {
        return try {
            context.contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.ADDRESS} = ?",
                arrayOf(address),
                null,
            )?.use { it.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
