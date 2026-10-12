package com.sirmrm.messageapp.data.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.sirmrm.messageapp.data.db.ConversationDao
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.MessageEntity
import com.sirmrm.messageapp.data.provider.SmsProvider
import com.sirmrm.messageapp.domain.model.MessageType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sends SMS (multi-part, dual-SIM aware) and records them in the provider + Room.
 */
@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsProvider: SmsProvider,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
) {

    suspend fun send(address: String, body: String, subscriptionId: Int): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(smsProvider.isDefaultSmsApp()) {
                    "App must be the default SMS app to send"
                }
                val smsManager = if (subscriptionId >= 0) {
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(body)
                val sentIntents = parts.map {
                    PendingIntent.getBroadcast(
                        context, 0,
                        Intent(ACTION_SMS_SENT).setClassName(
                            context.packageName,
                            "com.sirmrm.messageapp.app.sms.SmsSentReceiver",
                        ).setPackage(context.packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                }
                val deliveredIntents = parts.map {
                    PendingIntent.getBroadcast(
                        context, 0,
                        Intent(ACTION_SMS_DELIVERED).setClassName(
                            context.packageName,
                            "com.sirmrm.messageapp.app.sms.SmsSentReceiver",
                        ).setPackage(context.packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                }

                smsManager.sendMultipartTextMessage(
                    address, null, ArrayList(parts),
                    ArrayList(sentIntents), ArrayList(deliveredIntents),
                )

                val now = System.currentTimeMillis()
                val providerId = smsProvider.insertSent(address, body, now, subscriptionId)
                    ?: -now // negative local id when provider write isn't possible
                val threadId = android.provider.Telephony.Threads.getOrCreateThreadId(context, address)

                messageDao.upsert(
                    MessageEntity(
                        id = providerId,
                        threadId = threadId,
                        address = address,
                        body = body,
                        date = now,
                        type = MessageType.SENT.name,
                        read = true,
                        seen = true,
                        subscriptionId = subscriptionId,
                        folder = "INBOX",
                        customFolderId = null,
                        category = "UNKNOWN",
                        promoScore = 0f,
                        reasonCodes = "",
                        deletedAt = null,
                        txSubtype = null, txAmountRials = null, txBalanceRials = null,
                        bankName = null, suspicious = false, maybeTransactional = false,
                    )
                )
                conversationDao.updateForNewMessage(threadId, body.take(140), now, 0, 0f, false)
                providerId
            }
        }

    companion object {
        const val ACTION_SMS_SENT = "com.sirmrm.messageapp.action.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.sirmrm.messageapp.action.SMS_DELIVERED"
    }
}
