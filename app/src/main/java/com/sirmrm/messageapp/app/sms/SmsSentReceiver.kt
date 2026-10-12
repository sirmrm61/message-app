package com.sirmrm.messageapp.app.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sirmrm.messageapp.data.sms.SmsSender

/** Handles SmsManager sent/delivered result broadcasts. */
class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Delivery status is surfaced via message type in a later iteration;
        // for now we only need the broadcast registered for PendingIntents.
        when (intent.action) {
            SmsSender.ACTION_SMS_SENT,
            SmsSender.ACTION_SMS_DELIVERED -> {
                if (resultCode != Activity.RESULT_OK) {
                    // Could mark the message failed in Room here.
                }
            }
        }
    }
}
