package com.sirmrm.messageapp.app.sms

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sirmrm.messageapp.app.MainActivity

/**
 * Handles ACTION_SENDTO (sms:/smsto:/mms:/mmsto:) and plain-text share intents
 * by routing to MainActivity's composer with the address/body prefilled.
 */
class SendToActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val address = intent?.data?.schemeSpecificPart?.substringBefore('?') ?: ""
        val bodyFromUri = intent?.data?.let { uri ->
            uri.getQueryParameter("body") ?: uri.query
                ?.split("&")
                ?.firstOrNull { it.startsWith("body=", true) }
                ?.substringAfter('=')
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
        }
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: bodyFromUri ?: ""

        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_COMPOSE_ADDRESS, address)
                .putExtra(MainActivity.EXTRA_COMPOSE_BODY, body)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }
}
