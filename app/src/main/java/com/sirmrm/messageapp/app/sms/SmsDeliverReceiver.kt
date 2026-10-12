package com.sirmrm.messageapp.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sirmrm.messageapp.app.notify.NotificationHelper
import com.sirmrm.messageapp.classifier.Decision
import com.sirmrm.messageapp.classifier.SmsCategory
import com.sirmrm.messageapp.data.classifier.ClassifierManager
import com.sirmrm.messageapp.data.db.ConversationDao
import com.sirmrm.messageapp.data.db.ConversationEntity
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.MessageEntity
import com.sirmrm.messageapp.data.provider.SmsProvider
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.MessageType
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives SMS_DELIVER (only delivered to the default SMS app): persists the
 * message, classifies it, updates folders and fires a notification.
 */
@AndroidEntryPoint
class SmsDeliverReceiver : BroadcastReceiver() {

    @Inject lateinit var smsProvider: SmsProvider
    @Inject lateinit var classifierManager: ClassifierManager
    @Inject lateinit var messageDao: MessageDao
    @Inject lateinit var conversationDao: ConversationDao
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val pending = goAsync()
        scope.launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return@launch
                if (messages.isEmpty()) return@launch

                // Multi-part messages arrive as several SmsMessage parts.
                val address = messages[0].displayOriginatingAddress ?: messages[0].originatingAddress ?: ""
                val body = messages.joinToString("") { it.messageBody ?: "" }
                val date = messages[0].timestampMillis
                val subId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)

                if (address.isBlank() || body.isBlank()) return@launch

                val settings = settingsRepository.settings.first()
                if (settings.blockedSenders.any { address.contains(it) }) return@launch

                // 1. Persist to the system provider (required of the default app).
                val providerId = smsProvider.insertReceived(address, body, date, subId)
                val threadId = Telephony.Threads.getOrCreateThreadId(context, address)

                // 2. Classify on-device.
                val result = classifierManager.classify(address, body)

                val folder = when (result.decision) {
                    Decision.AUTO_MOVE -> MessageFolder.PROMOTIONS
                    else -> MessageFolder.INBOX
                }

                // 3. Mirror into Room with classification metadata.
                messageDao.upsert(
                    MessageEntity(
                        id = providerId ?: -(date),
                        threadId = threadId,
                        address = address,
                        body = body,
                        date = date,
                        type = MessageType.INBOX.name,
                        read = false,
                        seen = false,
                        subscriptionId = subId,
                        folder = folder.name,
                        customFolderId = null,
                        category = result.category.name,
                        promoScore = result.score,
                        reasonCodes = result.reasons.joinToString(";") { it.code },
                        deletedAt = null,
                    )
                )

                val existing = conversationDao.get(threadId)
                conversationDao.upsert(
                    ConversationEntity(
                        threadId = threadId,
                        address = address,
                        displayName = existing?.displayName ?: address,
                        snippet = body.take(140),
                        lastMessageDate = date,
                        unreadCount = (existing?.unreadCount ?: 0) + 1,
                        pinned = existing?.pinned ?: false,
                        muted = existing?.muted ?: false,
                        blocked = existing?.blocked ?: false,
                        folder = existing?.folder?.takeIf { it != "INBOX" } ?: folder.name,
                        customFolderId = existing?.customFolderId,
                        promoScore = result.score,
                        maybePromo = result.decision == Decision.SUGGEST,
                        draft = existing?.draft,
                    )
                )

                // 4. Notify (unless auto-moved promo or muted thread).
                if (result.decision != Decision.AUTO_MOVE && existing?.muted != true) {
                    notificationHelper.showMessageNotification(
                        threadId = threadId,
                        address = address,
                        body = body,
                        isOtp = result.category == SmsCategory.OTP,
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
