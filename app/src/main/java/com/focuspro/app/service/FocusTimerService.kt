package com.focuspro.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focuspro.app.FocusProApplication
import com.focuspro.app.MainActivity
import com.focuspro.app.R
import com.focuspro.app.data.local.entity.BlockMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("FOCUS PRO", "Focus session running"))
        startTicker()
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val app = applicationContext as? FocusProApplication ?: return@launch
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            while (true) {
                val prefs = app.preferencesManager.preferencesFlow.first()
                val now = System.currentTimeMillis()
                val hasStudy = prefs.studyModeActive && prefs.studyPhaseEndMillis > now
                val hasTimer = prefs.timerLockActive && prefs.timerLockEndMillis > now

                if (!hasStudy && !hasTimer) {
                    app.repository.clearExpiredBlocks()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val targetEnd = if (hasStudy) prefs.studyPhaseEndMillis else prefs.timerLockEndMillis
                val remSec = ((targetEnd - now) / 1000L).coerceAtLeast(0L)
                val mins = remSec / 60
                val secs = remSec % 60
                val label = when {
                    hasStudy && prefs.studyModeIsBreak -> "Break Time: %02d:%02d remaining".format(mins, secs)
                    hasStudy -> "Study Mode Active: %02d:%02d remaining".format(mins, secs)
                    else -> "App Lock Timer: %02d:%02d remaining".format(mins, secs)
                }
                nm?.notify(NOTIFICATION_ID, buildNotification("FOCUS PRO Active", label))

                // Auto-transition Study Mode -> Break -> Study
                if (prefs.studyModeActive && prefs.studyPhaseEndMillis in 1..now) {
                    if (!prefs.studyModeIsBreak) {
                        app.repository.recordStudySession(
                            presetName = "${prefs.studyMinutesConfig}m Study",
                            studyMinutes = prefs.studyMinutesConfig,
                            breakMinutes = prefs.breakMinutesConfig,
                            actualMinutes = prefs.studyMinutesConfig,
                            completed = true
                        )
                        val breakEnd = now + prefs.breakMinutesConfig * 60_000L
                        app.preferencesManager.setStudyModeState(
                            active = true,
                            isBreak = true,
                            phaseEndMillis = breakEnd,
                            studyMinutes = prefs.studyMinutesConfig,
                            breakMinutes = prefs.breakMinutesConfig,
                            blockDuringBreak = prefs.blockAppsDuringBreak
                        )
                    } else {
                        val nextStudyEnd = now + prefs.studyMinutesConfig * 60_000L
                        app.preferencesManager.setStudyModeState(
                            active = true,
                            isBreak = false,
                            phaseEndMillis = nextStudyEnd,
                            studyMinutes = prefs.studyMinutesConfig,
                            breakMinutes = prefs.breakMinutesConfig,
                            blockAppsDuringBreak = prefs.blockAppsDuringBreak
                        )
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun buildNotification(title: String, text: String) =
        NotificationCompat.Builder(this, FocusProApplication.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        scope.cancel()
    }

    companion object {
        const val ACTION_STOP = "com.focuspro.app.ACTION_STOP_TIMER"
        private const val NOTIFICATION_ID = 4101

        fun start(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java)
            context.startForegroundService(intent)
        }
    }
}
