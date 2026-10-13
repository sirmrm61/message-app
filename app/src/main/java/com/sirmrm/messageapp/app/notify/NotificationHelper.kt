package com.sirmrm.messageapp.app.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.sirmrm.messageapp.app.MainActivity
import com.sirmrm.messageapp.app.MessageApp
import com.sirmrm.messageapp.app.R
import com.sirmrm.messageapp.classifier.SmsCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private fun channelFor(category: SmsCategory): String = when (category) {
        SmsCategory.OTP -> MessageApp.CHANNEL_OTP
        SmsCategory.TRANSACTIONAL -> MessageApp.CHANNEL_TX
        SmsCategory.PROMO -> MessageApp.CHANNEL_PROMO
        else -> MessageApp.CHANNEL_PERSONAL
    }

    private fun broadcastPi(
        action: String, requestCode: Int, mutable: Boolean = false,
        configure: Intent.() -> Unit = {},
    ): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action).apply(configure),
            flags,
        )
    }

    /**
     * Heads-up notification for a new message — MessagingStyle, grouped per sender,
     * with mark-read / quick-reply / copy-OTP actions.
     */
    fun showMessageNotification(
        threadId: Long,
        address: String,
        body: String,
        displayName: String?,
        category: SmsCategory,
        messageId: Long,
        date: Long = System.currentTimeMillis(),
        hideContent: Boolean = false,
        ttsAvailable: Boolean = false,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.areNotificationsEnabled()) return

        val channel = channelFor(category)
        val isOtp = category == SmsCategory.OTP
        val shownBody = if (hideContent) context.getString(R.string.notif_hidden_content) else body
        val shownTitle = displayName ?: address
        val groupKey = "sender_" + (displayName ?: address)

        val openIntent = PendingIntent.getActivity(
            context, threadId.toInt(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_THREAD_ID, threadId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val sender = Person.Builder().setName(shownTitle).setKey(address).build()
        val style = NotificationCompat.MessagingStyle(sender)
            .addMessage(shownBody, date, sender)

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(style)
            .setContentTitle(shownTitle)
            .setContentText(shownBody)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(groupKey)

        if (isOtp && hideContent) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        }

        // «خوانده شد» — marks both system provider and internal DB, cancels notification.
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                context.getString(R.string.action_mark_read),
                broadcastPi(NotificationActionReceiver.ACTION_MARK_READ, threadId.toInt() * 31 + 2) {
                    putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                    putExtra(NotificationActionReceiver.EXTRA_MESSAGE_ID, messageId)
                },
            ).build()
        )

        // «پاسخ سریع» — RemoteInput needs a MUTABLE pending intent.
        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY).build()
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                context.getString(R.string.action_reply),
                broadcastPi(NotificationActionReceiver.ACTION_REPLY, threadId.toInt(), mutable = true) {
                    putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                    putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
                },
            ).addRemoteInput(remoteInput).build()
        )

        // «کپی رمز» — OTP only.
        if (isOtp) {
            OTP_REGEX.find(body)?.value?.let { code ->
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_notification,
                        context.getString(R.string.action_copy_code),
                        broadcastPi(NotificationActionReceiver.ACTION_COPY_CODE, threadId.toInt() * 31 + 1) {
                            putExtra(NotificationActionReceiver.EXTRA_CODE, code)
                        },
                    ).build()
                )
            }
        }

        // «خواندن» — only when the TTS feature is enabled.
        if (ttsAvailable) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notification,
                    context.getString(R.string.action_read_aloud),
                    broadcastPi(NotificationActionReceiver.ACTION_READ_ALOUD, threadId.toInt() * 31 + 3) {
                        putExtra(NotificationActionReceiver.EXTRA_BODY, body)
                        putExtra(NotificationActionReceiver.EXTRA_ADDRESS, shownTitle)
                    },
                ).build()
            )
        }

        val id = (threadId.toInt() and 0x7fffffff)
        nm.notify(id, builder.build())

        // Per-sender group summary (required so grouped notifications collapse properly).
        val summary = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(shownTitle)
            .setContentText(shownBody)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        nm.notify(GROUP_SUMMARY_BASE + (id % 1000), summary)
    }

    fun cancel(threadId: Long) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val id = threadId.toInt() and 0x7fffffff
        nm.cancel(id)
        nm.cancel(GROUP_SUMMARY_BASE + (id % 1000))
    }

    companion object {
        private val OTP_REGEX = Regex("""\b\d{4,8}\b""")
        private const val GROUP_SUMMARY_BASE = 900_000
    }
}
