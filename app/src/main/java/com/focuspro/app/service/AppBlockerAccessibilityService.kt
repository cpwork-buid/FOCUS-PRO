package com.focuspro.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.focuspro.app.FocusProApplication
import com.focuspro.app.data.local.entity.BlockHistoryEntity
import com.focuspro.app.data.local.entity.BlockMode
import com.focuspro.app.data.local.entity.BlockedAttemptEntity
import com.focuspro.app.ui.blocker.BlockingOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var defaultLauncherPackage: String? = null
    private var lastBlockedPkg: String? = null
    private var lastBlockedTimeMillis: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        defaultLauncherPackage = resolveInfo?.activityInfo?.packageName
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg == packageName ||
            pkg == "com.android.systemui" ||
            pkg == "android" ||
            pkg == defaultLauncherPackage
        ) {
            return
        }

        val app = applicationContext as? FocusProApplication ?: return
        val dao = app.database.blockedAppDao()

        serviceScope.launch {
            val blocked = dao.getByPackage(pkg) ?: return@launch
            if (!blocked.isBlocked) return@launch

            val now = System.currentTimeMillis()

            // Auto-unblock if scheduled/timer lock expired
            if (blocked.lockEndTimeMillis in 1..now) {
                val mins = ((now - blocked.lockStartTimeMillis) / 60_000L).toInt().coerceAtLeast(1)
                dao.insertHistory(
                    BlockHistoryEntity(
                        packageName = blocked.packageName,
                        appName = blocked.appName,
                        blockMode = blocked.blockMode,
                        startedAtMillis = blocked.lockStartTimeMillis,
                        endedAtMillis = now,
                        durationMinutes = mins,
                        wasCompleted = true
                    )
                )
                dao.deleteByPackage(pkg)
                return@launch
            }

            // Respect Temporary Allow window
            if (blocked.tempAllowUntilMillis > now) return@launch

            // Respect Study Mode break rules
            val prefs = app.preferencesManager.preferencesFlow.first()
            if (blocked.blockMode == BlockMode.STUDY_MODE.name &&
                prefs.studyModeActive &&
                prefs.studyModeIsBreak &&
                !prefs.blockAppsDuringBreak
            ) {
                return@launch
            }

            // Debounce rapid duplicate events within 1.2s
            if (pkg == lastBlockedPkg && (now - lastBlockedTimeMillis) < 1200L) {
                return@launch
            }
            lastBlockedPkg = pkg
            lastBlockedTimeMillis = now

            dao.insertAttempt(
                BlockedAttemptEntity(
                    packageName = pkg,
                    appName = blocked.appName,
                    timestampMillis = now
                )
            )

            val overlayIntent = Intent(this@AppBlockerAccessibilityService, BlockingOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME, blocked.packageName)
                putExtra(BlockingOverlayActivity.EXTRA_APP_NAME, blocked.appName)
                putExtra(BlockingOverlayActivity.EXTRA_BLOCK_MODE, blocked.blockMode)
                putExtra(BlockingOverlayActivity.EXTRA_LOCK_END_MILLIS, blocked.lockEndTimeMillis)
            }
            startActivity(overlayIntent)
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
