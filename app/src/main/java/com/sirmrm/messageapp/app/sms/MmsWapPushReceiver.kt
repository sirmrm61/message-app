package com.sirmrm.messageapp.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * WAP_PUSH_DELIVER receiver required of the default SMS app.
 * MMS download is deliberately minimal in this version: the PDU is
 * acknowledged so the platform stops redelivering; full MMS fetching is
 * listed as a known limitation in docs/ARCHITECTURE.md.
 */
class MmsWapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        // Acknowledge the push; MMS content download happens on demand.
        resultCode = android.app.Activity.RESULT_OK
    }
}
