package com.sirmrm.messageapp.tts

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sirmrm.messageapp.tts.R
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground (mediaPlayback) service that reads SMS text aloud with the offline
 * Persian TTS engine. At most 3 queued messages play consecutively; later ones
 * just get a normal notification.
 */
@AndroidEntryPoint
class TtsService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queue = ConcurrentLinkedQueue<Pair<String, String?>>()
    @Volatile private var speaking = false

    private val callListener = object : android.telephony.PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (state != android.telephony.TelephonyManager.CALL_STATE_IDLE) {
                TtsEngineHolder.stop()
                queue.clear()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        if (Build.VERSION.SDK_INT < 31) {
            runCatching {
                getSystemService(android.telephony.TelephonyManager::class.java)
                    ?.listen(callListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SPEAK -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                val label = intent.getStringExtra(EXTRA_LABEL)
                if (text.isBlank()) return START_NOT_STICKY
                startForeground(NOTIF_ID, buildNotif(label, text))
                queue.offer(text to label)
                if (!speaking) drainQueue()
            }
            ACTION_STOP -> {
                queue.clear()
                TtsEngineHolder.stop()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun drainQueue() {
        if (speaking) return
        val next = queue.poll() ?: run {
            stopSelf()
            return
        }
        speaking = true
        scope.launch {
            val s = settingsRepository.settings.first()
            if (s.ttsEnabled) {
                // Respect silent mode — never play over it.
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (am.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    TtsEngineHolder.speak(applicationContext, next.first, s.ttsVoiceId) {
                        speaking = false
                        drainQueue()
                    }
                } else {
                    speaking = false
                    drainQueue()
                }
            } else {
                speaking = false
                drainQueue()
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TTS, getString(R.string.channel_tts),
                NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotif(label: String?, text: String): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent()
        val open = PendingIntent.getActivity(
            this, 0,
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TtsService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_TTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label ?: getString(R.string.tts_reading))
            .setContentText(text.take(80))
            .setContentIntent(open)
            .addAction(R.drawable.ic_notification, getString(R.string.action_stop), stop)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        TtsEngineHolder.stop()
        if (Build.VERSION.SDK_INT < 31) {
            runCatching {
                getSystemService(android.telephony.TelephonyManager::class.java)
                    ?.listen(callListener, android.telephony.PhoneStateListener.LISTEN_NONE)
            }
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_SPEAK = "com.sirmrm.messageapp.tts.SPEAK"
        const val ACTION_STOP = "com.sirmrm.messageapp.tts.STOP"
        const val EXTRA_TEXT = "text"
        const val EXTRA_LABEL = "label"
        private const val CHANNEL_TTS = "tts_playback"
        private const val NOTIF_ID = 991
        /** Max queued consecutive reads — the rest stay notification-only. */
        const val MAX_QUEUE = 3

        /**
         * Try to start the foreground service from a background context.
         * On Android 12+ ForegroundServiceStartNotAllowedException may fire —
         * caller then shows a normal notification with a «خواندن» action.
         */
        fun tryStart(context: Context, text: String, label: String?): Boolean {
            val intent = Intent(context, TtsService::class.java)
                .setAction(ACTION_SPEAK)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_LABEL, label)
            return try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: ForegroundServiceStartNotAllowedException) {
                false
            } catch (e: IllegalStateException) {
                false
            }
        }
    }
}
