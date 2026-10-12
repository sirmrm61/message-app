package com.sirmrm.messageapp.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sirmrm.messageapp.domain.repository.MessageRepository
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Daily maintenance:
 *  - purge trash items past the retention window
 *  - move old promos to trash (only when the user enabled auto-delete)
 * All deletions go through the 30-day trash — nothing is silently removed.
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            messageRepository.purgeExpiredTrash()
            val settings = settingsRepository.settings.first()
            if (settings.autoDeleteEnabled) {
                val cleaned = messageRepository.autoCleanOldPromos(settings.autoDeleteDays)
                if (cleaned > 0) {
                    settingsRepository.update {
                        it.copy(promosCleanedThisWeek = it.promosCleanedThisWeek + cleaned)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
