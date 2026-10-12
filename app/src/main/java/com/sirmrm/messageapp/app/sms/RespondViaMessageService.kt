package com.sirmrm.messageapp.app.sms

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager
import android.telephony.TelephonyManager

/**
 * RESPOND_VIA_MESSAGE service required of the default SMS app — lets the
 * system send a canned quick-reply (e.g. "Can't talk now") from the
 * incoming-call screen.
 */
class RespondViaMessageService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri: Uri? = intent?.data
        val address = uri?.schemeSpecificPart
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (!address.isNullOrBlank() && !body.isNullOrBlank() &&
            SmsRoleHelper.isDefaultSmsApp(this)
        ) {
            val subId = intent.getIntExtra(
                TelephonyManager.EXTRA_SUBSCRIPTION_ID, -1,
            )
            val manager = if (subId >= 0) {
                SmsManager.getSmsManagerForSubscriptionId(subId)
            } else {
                @Suppress("DEPRECATION") SmsManager.getDefault()
            }
            manager.sendTextMessage(address, null, body, null, null)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
