package com.focuspro.app.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focuspro.app.FocusProApplication
import com.focuspro.app.R
import com.focuspro.app.service.FocusTimerService
import kotlinx.coroutines.flow.first

class AutoUnblockWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FocusProApplication ?: return Result.failure()
        app.repository.clearExpiredBlocks()
        val prefs = app.preferencesManager.preferencesFlow.first()
        if (prefs.notifyTimerCompletion) {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notif = NotificationCompat.Builder(app, FocusProApplication.CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("FOCUS PRO • Lock Completed")
                .setContentText("Your scheduled app block timer has finished and apps are unlocked.")
                .setAutoCancel(true)
                .build()
            nm?.notify(4201, notif)
        }
        return Result.success()
    }
}

class StateRecoveryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FocusProApplication ?: return Result.failure()
        app.repository.clearExpiredBlocks()
        val prefs = app.preferencesManager.preferencesFlow.first()
        val now = System.currentTimeMillis()
        if ((prefs.timerLockActive && prefs.timerLockEndMillis > now) ||
            (prefs.studyModeActive && prefs.studyPhaseEndMillis > now)
        ) {
            runCatching { FocusTimerService.start(app) }
        }
        return Result.success()
    }
}
