package com.sirmrm.messageapp.app.ui

import android.app.Notification
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

/** In-app alert for SMS arriving while the UI is in the foreground. */
object InAppAlerts {
    const val ACTION_INCOMING_SMS = "com.sirmrm.messageapp.action.INCOMING_SMS"
    const val EXTRA_SENDER = "sender"
    const val EXTRA_BODY = "body"

    /** Built-in alert sounds, selectable in Settings (index = AppSettings.alertSoundIndex). */
    val SOUNDS: List<Uri> = listOf(
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
    )

    fun playAlert(context: Context, soundIndex: Int) {
        // Respect silent / DND — the stream volume check keeps us quiet when the
        // user asked for silence; we never try to bypass it.
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        if (am.ringerMode == android.media.AudioManager.RINGER_MODE_SILENT ||
            am.ringerMode == android.media.AudioManager.RINGER_MODE_VIBRATE
        ) return
        runCatching {
            val uri = SOUNDS.getOrElse(soundIndex) { SOUNDS[0] }
            RingtoneManager.getRingtone(context, uri)?.play()
        }
    }
}
