package com.novamusic.player.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novamusic.player.NovaMusicApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NovaMusicApplication ?: return Result.success()
        val container = app.container

        container.updateManager.clearStaleNotificationIfUpdated()

        val autoCheckEnabled = container.preferencesRepository.appSettings.first().autoCheckUpdates
        if (!autoCheckEnabled) {
            return Result.success()
        }

        val outcome = container.updateManager.checkIfIntervalElapsed()
        return if (!outcome && runAttemptCount < 2) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}

object UpdateWorkScheduler {
    private const val UNIQUE_WORK_NAME = "nova_periodic_app_update_check"
    private const val WORK_TAG = "nova_app_update_worker"

    fun syncSchedule(context: Context, autoCheckEnabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!autoCheckEnabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            UpdateConfig.CHECK_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
