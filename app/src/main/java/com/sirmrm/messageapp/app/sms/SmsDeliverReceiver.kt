package com.sirmrm.messageapp.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sirmrm.messageapp.app.notify.NotificationHelper
import com.sirmrm.messageapp.classifier.Decision
import com.sirmrm.messageapp.classifier.SmsCategory
import com.sirmrm.messageapp.data.classifier.ClassifierManager
import com.sirmrm.messageapp.data.classifier.folderFor
import com.sirmrm.messageapp.data.classifier.mergeConversationFolder
import com.sirmrm.messageapp.data.contacts.ContactsResolver
import com.sirmrm.messageapp.data.db.ConversationDao
import com.sirmrm.messageapp.data.db.ConversationEntity
import com.sirmrm.messageapp.data.db.KeyContactDao
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
    @Inject lateinit var contactsResolver: ContactsResolver
    @Inject lateinit var keyContactDao: KeyContactDao

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

                val folder = folderFor(result)

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
                        txSubtype = result.bank?.subtype?.name,
                        txAmountRials = result.bank?.amountRials,
                        txBalanceRials = result.bank?.balanceRials,
                        bankName = result.bank?.bankName,
                        suspicious = result.suspicious,
                        maybeTransactional = result.maybeTransactional,
                    )
                )

                val existing = conversationDao.get(threadId)
                val contact = contactsResolver.lookup(address)
                conversationDao.upsert(
                    ConversationEntity(
                        threadId = threadId,
                        address = address,
                        displayName = contact?.displayName ?: existing?.displayName ?: address,
                        photoUri = contact?.photoUri ?: existing?.photoUri,
                        snippet = body.take(140),
                        lastMessageDate = date,
                        unreadCount = (existing?.unreadCount ?: 0) + 1,
                        pinned = existing?.pinned ?: false,
                        muted = existing?.muted ?: false,
                        blocked = existing?.blocked ?: false,
                        folder = mergeConversationFolder(existing?.folder, folder),
                        customFolderId = existing?.customFolderId,
                        promoScore = result.score,
                        maybePromo = result.decision == Decision.SUGGEST,
                        draft = existing?.draft,
                    )
                )

                // 4. Notify — in-app toast while foreground, heads-up otherwise.
                //    (never for auto-moved promos or muted threads)
                // Auto-read for key contacts — privacy gates live in AutoReadDecider.
                run {
                    val senderKey = com.sirmrm.messageapp.classifier.SenderKey.of(address)
                        .ifBlank { address.trim() }
                    val keyContact = runCatching { keyContactDao.get(senderKey) }.getOrNull()
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                    val audioDevices = runCatching {
                        am.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                            .any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                                   it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                   it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
                    }.getOrDefault(false)
                    val envBlocked =
                        am.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL ||
                        (settings.ttsAutoReadHeadphoneOnly && !audioDevices) ||
                        (settings.ttsAutoReadScreenOnOnly && !pm.isInteractive)
                    val autoRead = com.sirmrm.messageapp.classifier.tts.AutoReadDecider.shouldAutoRead(
                        ttsEnabled = settings.ttsEnabled,
                        isKeyContact = keyContact != null,
                        keyAutoRead = keyContact?.autoRead ?: false,
                        category = result.category,
                        allowOtp = settings.ttsAutoReadOtp,
                        allowTransactional = settings.ttsAutoReadTransactional,
                        environmentBlocks = envBlocked,
                    )
                    if (autoRead) {
                        if (com.sirmrm.messageapp.app.MessageApp.isInForeground) {
                            // Foreground: read directly, no service needed.
                            com.sirmrm.messageapp.tts.TtsEngineHolder.speak(
                                context.applicationContext, body,
                                keyContact?.voiceId ?: settings.ttsVoiceId,
                                settings.ttsSpeed, settings.ttsVolume)
                        } else {
                            // App closed: try the mediaPlayback foreground service; when the
                            // platform blocks it (Android 12+), the «خواندن» notification
                            // action below is the user-driven fallback.
                            com.sirmrm.messageapp.tts.TtsService.tryStart(
                                context, body, contact?.displayName ?: address)
                        }
                    }
                }

                if (result.decision != Decision.AUTO_MOVE && existing?.muted != true) {
                    if (com.sirmrm.messageapp.app.MessageApp.isInForeground) {
                        context.sendBroadcast(
                            Intent(com.sirmrm.messageapp.app.ui.InAppAlerts.ACTION_INCOMING_SMS)
                                .setPackage(context.packageName)
                                .putExtra(com.sirmrm.messageapp.app.ui.InAppAlerts.EXTRA_SENDER, contact?.displayName ?: address)
                                .putExtra(com.sirmrm.messageapp.app.ui.InAppAlerts.EXTRA_BODY, body.take(100))
                        )
                    } else {
                        notificationHelper.showMessageNotification(
                            threadId = threadId,
                            address = address,
                            body = body,
                            displayName = contact?.displayName,
                            category = result.category,
                            messageId = providerId ?: 0L,
                            hideContent = result.category == SmsCategory.OTP && settings.hideOtpContent,
                            ttsAvailable = settings.ttsEnabled,
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
