package com.novamusic.player.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.novamusic.player.NovaMusicApplication

class BroadcastRegistrationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NovaMusicApplication ?: return Result.success()
        val result = app.container.fcmUpdatePushManager.syncDeviceRegistration(force = false)
        return when (result) {
            is BroadcastRegistrationResult.RetryableFailure -> {
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
            }
            else -> Result.success()
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
    }
}
