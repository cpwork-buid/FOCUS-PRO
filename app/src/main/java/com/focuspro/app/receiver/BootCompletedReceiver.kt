package com.focuspro.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.focuspro.app.worker.StateRecoveryWorker

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val request = OneTimeWorkRequestBuilder<StateRecoveryWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "focus_pro_boot_recovery",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
