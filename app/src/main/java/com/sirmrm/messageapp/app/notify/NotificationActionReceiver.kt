package com.sirmrm.messageapp.app.notify

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.sirmrm.messageapp.app.R
import com.sirmrm.messageapp.data.sms.SmsSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handles quick-reply, copy-code and mark-read actions from notifications. */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var smsSender: SmsSender

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
            ACTION_MARK_READ -> Unit
        }
    }

    companion object {
        const val ACTION_REPLY = "com.sirmrm.messageapp.action.REPLY"
        const val ACTION_COPY_CODE = "com.sirmrm.messageapp.action.COPY_CODE"
        const val ACTION_MARK_READ = "com.sirmrm.messageapp.action.MARK_READ"
        const val KEY_REPLY = "key_reply"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_CODE = "code"
    }
}
