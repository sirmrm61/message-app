package com.sirmrm.messageapp.app.notify

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.sirmrm.messageapp.app.R
import com.sirmrm.messageapp.data.db.ConversationDao
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.provider.SmsProvider
import com.sirmrm.messageapp.data.sms.SmsSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handles quick-reply, copy-code, mark-read and read-aloud notification actions. */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var smsSender: SmsSender
    @Inject lateinit var smsProvider: SmsProvider
    @Inject lateinit var messageDao: MessageDao
    @Inject lateinit var conversationDao: ConversationDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_REPLY)?.toString() ?: return
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
                val pending = goAsync()
                scope.launch {
                    try {
                        smsSender.send(address, text, -1)
                        intent.getLongExtra(EXTRA_THREAD_ID, -1)
                            .takeIf { it > 0 }
                            ?.let { markRead(context, it) }
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_COPY_CODE -> {
                val code = intent.getStringExtra(EXTRA_CODE) ?: return
                val cm = context.getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("otp", code))
                Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
            }
            ACTION_MARK_READ -> {
                val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
                if (threadId <= 0) return
                val pending = goAsync()
                scope.launch {
                    try {
                        markRead(context, threadId)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_READ_ALOUD -> {
                val body = intent.getStringExtra(EXTRA_BODY) ?: return
                // Hand off to the TTS service; no-op when the feature is off.
                context.startService(
                    Intent(context, com.sirmrm.messageapp.tts.TtsService::class.java)
                        .setAction(com.sirmrm.messageapp.tts.TtsService.ACTION_SPEAK)
                        .putExtra(com.sirmrm.messageapp.tts.TtsService.EXTRA_TEXT, body)
                        .putExtra(com.sirmrm.messageapp.tts.TtsService.EXTRA_LABEL, intent.getStringExtra(EXTRA_ADDRESS))
                )
            }
        }
    }

    /** read=1 in both the Telephony provider and Room, then cancel + zero unread. */
    private suspend fun markRead(context: Context, threadId: Long) {
        smsProvider.markThreadRead(threadId)
        runCatching {
            messageDao.markThreadRead(threadId)
            conversationDao.markRead(threadId)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(threadId.toInt() and 0x7fffffff)
    }

    companion object {
        const val ACTION_REPLY = "com.sirmrm.messageapp.action.REPLY"
        const val ACTION_COPY_CODE = "com.sirmrm.messageapp.action.COPY_CODE"
        const val ACTION_MARK_READ = "com.sirmrm.messageapp.action.MARK_READ"
        const val ACTION_READ_ALOUD = "com.sirmrm.messageapp.action.READ_ALOUD"
        const val KEY_REPLY = "key_reply"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_CODE = "code"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_BODY = "body"
    }
}
