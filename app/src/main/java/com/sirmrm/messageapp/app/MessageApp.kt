package com.sirmrm.messageapp.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sirmrm.messageapp.app.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MessageApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        createNotificationChannels()
        workScheduler.scheduleDailyCleanup()
    }

    /** Writes fatal crashes to filesDir/crash.log so the next launch can show the trace. */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                getFileStreamPath("crash.log").writeText(
                    "${java.util.Date()}\nthread=${thread.name}\n${throwable.stackTraceToString()}"
                )
            }
            previous?.uncaughtException(thread, throwable)
                ?: kotlin.system.exitProcess(1)
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        fun channel(id: String, nameRes: Int, importance: Int) {
            val ch = NotificationChannel(id, getString(nameRes), importance)
            if (importance <= NotificationManager.IMPORTANCE_LOW) {
                ch.setSound(null, null)
                ch.enableVibration(false)
            }
            nm.createNotificationChannel(ch)
        }
        // Separate channels so users can tune each class — promos stay silent.
        channel(CHANNEL_PERSONAL, R.string.channel_personal, NotificationManager.IMPORTANCE_HIGH)
        channel(CHANNEL_TX, R.string.channel_tx, NotificationManager.IMPORTANCE_HIGH)
        channel(CHANNEL_OTP, R.string.channel_otp, NotificationManager.IMPORTANCE_HIGH)
        channel(CHANNEL_PROMO, R.string.channel_promo, NotificationManager.IMPORTANCE_LOW)
        channel(CHANNEL_CLEANUP, R.string.channel_cleanup, NotificationManager.IMPORTANCE_LOW)
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_PERSONAL = "msgs_personal"
        const val CHANNEL_TX = "msgs_tx"
        const val CHANNEL_OTP = "msgs_otp"
        const val CHANNEL_PROMO = "msgs_promo"
        const val CHANNEL_CLEANUP = "cleanup"

        /** Set by MainActivity while the UI is resumed (foreground in-app alerts). */
        @Volatile var isInForeground: Boolean = false
    }
}
