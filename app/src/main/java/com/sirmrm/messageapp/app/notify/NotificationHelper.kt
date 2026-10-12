package com.sirmrm.messageapp.app.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.sirmrm.messageapp.app.MainActivity
import com.sirmrm.messageapp.app.MessageApp
import com.sirmrm.messageapp.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun showMessageNotification(threadId: Long, address: String, body: String, isOtp: Boolean) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val openIntent = PendingIntent.getActivity(
            context, threadId.toInt(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_THREAD_ID, threadId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, MessageApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(address)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup("messages_group")

        // Quick reply
        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY).build()
        val replyIntent = PendingIntent.getBroadcast(
            context, threadId.toInt(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_REPLY)
                .putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                .putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                context.getString(R.string.action_reply),
                replyIntent,
            ).addRemoteInput(remoteInput).build()
        )

        // "Copy code" on OTPs
        if (isOtp) {
            OTP_REGEX.find(body)?.value?.let { code ->
                val copyIntent = PendingIntent.getBroadcast(
                    context, (threadId.toInt() * 31 + 1),
                    Intent(context, NotificationActionReceiver::class.java)
                        .setAction(NotificationActionReceiver.ACTION_COPY_CODE)
                        .putExtra(NotificationActionReceiver.EXTRA_CODE, code),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(
                    R.drawable.ic_notification,
                    context.getString(R.string.action_copy_code),
                    copyIntent,
                )
            }
        }

        nm.notify(threadId.toInt(), builder.build())
    }

    companion object {
        private val OTP_REGEX = Regex("""\b\d{4,8}\b""")
    }
}
