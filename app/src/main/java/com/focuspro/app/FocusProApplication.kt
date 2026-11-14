package com.focuspro.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.focuspro.app.data.local.FocusProDatabase
import com.focuspro.app.data.preferences.UserPreferencesManager
import com.focuspro.app.data.repository.FocusProRepository
import com.focuspro.app.worker.DailySummaryWorker

class FocusProApplication : Application() {

    lateinit var database: FocusProDatabase
        private set

    lateinit var preferencesManager: UserPreferencesManager
        private set

    lateinit var repository: FocusProRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = FocusProDatabase.getInstance(this)
        preferencesManager = UserPreferencesManager(this)
        repository = FocusProRepository(this, database, preferencesManager)
        createNotificationChannels()
        DailySummaryWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_TIMER,
                    "Focus & Lock Timers",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Ongoing study and app lock countdowns" },
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Reminders & Completions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Task deadlines, timer completions, and daily summary" },
                NotificationChannel(
                    CHANNEL_VPN,
                    "Website Blocker VPN",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Local DNS filtering status" }
            )
            manager.createNotificationChannels(channels)
        }
    }

    companion object {
        const val CHANNEL_TIMER = "focus_pro_timer"
        const val CHANNEL_ALERTS = "focus_pro_alerts"
        const val CHANNEL_VPN = "focus_pro_vpn"
    }
}
