package com.focuspro.app.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.focuspro.app.FocusProApplication
import com.focuspro.app.R
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class DailySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FocusProApplication ?: return Result.failure()
        val prefs = app.preferencesManager.preferencesFlow.first()
        if (!prefs.notifyDailySummary) return Result.success()

        val tasks = app.database.taskDao().getAllTasksSync()
        val pending = tasks.count { !it.isCompleted }
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notif = NotificationCompat.Builder(app, FocusProApplication.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FOCUS PRO • Daily Discipline Check")
            .setContentText("You have $pending pending study tasks. Stay focused today!")
            .setAutoCancel(true)
            .build()
        nm?.notify(4202, notif)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "focus_pro_daily_summary",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
