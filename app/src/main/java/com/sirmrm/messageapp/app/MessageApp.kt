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
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                getString(R.string.channel_messages),
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CLEANUP,
                getString(R.string.channel_cleanup),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_CLEANUP = "cleanup"
    }
}
