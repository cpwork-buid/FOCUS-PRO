package com.focuspro.app.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.focuspro.app.data.local.FocusProDatabase
import com.focuspro.app.data.local.entity.AppCategory
import com.focuspro.app.data.local.entity.BlockHistoryEntity
import com.focuspro.app.data.local.entity.BlockMode
import com.focuspro.app.data.local.entity.BlockedAppEntity
import com.focuspro.app.data.local.entity.BlockedAttemptEntity
import com.focuspro.app.data.local.entity.DailyFocusLogEntity
import com.focuspro.app.data.local.entity.StudySessionEntity
import com.focuspro.app.data.local.entity.StudyTaskEntity
import com.focuspro.app.data.local.entity.WebsiteRuleEntity
import com.focuspro.app.data.local.entity.WebsiteVisitEntity
import com.focuspro.app.data.preferences.UserPreferencesManager
import com.focuspro.app.util.PermissionHelper
import com.focuspro.app.worker.AutoUnblockWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val icon: Drawable?
)

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val icon: Drawable?,
    val totalTimeMillis: Long,
    val launchCount: Int,
    val percentage: Float
)

data class UsageReportData(
    val hasPermission: Boolean,
    val totalScreenTimeMillis: Long,
    val apps: List<AppUsageStat>,
    val categoryTotals: Map<AppCategory, Long>
)

class FocusProRepository(
    private val context: Context,
    private val db: FocusProDatabase,
    val prefs: UserPreferencesManager
) {
    private val blockedAppDao = db.blockedAppDao()
    private val websiteDao = db.websiteDao()
    private val taskDao = db.taskDao()
    private val sessionDao = db.studySessionDao()

    fun observeBlockedApps(): Flow<List<BlockedAppEntity>> = blockedAppDao.observeActiveBlockedApps()
    fun observeBlockHistory(): Flow<List<BlockHistoryEntity>> = blockedAppDao.observeBlockHistory()
    fun observeBlockedAttempts(): Flow<List<BlockedAttemptEntity>> = blockedAppDao.observeAttempts()
    fun observeWebsiteRules(): Flow<List<WebsiteRuleEntity>> = websiteDao.observeRules()
    fun observeWebsiteVisits(): Flow<List<WebsiteVisitEntity>> = websiteDao.observeVisits()
    fun observeAllTasks(): Flow<List<StudyTaskEntity>> = taskDao.observeAllTasks()
    fun observeStudySessions(): Flow<List<StudySessionEntity>> = sessionDao.observeSessions()
    fun observeDailyLogs(): Flow<List<DailyFocusLogEntity>> = sessionDao.observeDailyLogs()

    fun observeTodayAttemptsCount(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return blockedAppDao.observeAttemptCountSince(cal.timeInMillis)
    }

    suspend fun getInstalledUserApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launchIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launchIntent, 0)
        }

        val seen = HashSet<String>()
        val result = mutableListOf<InstalledAppInfo>()
        for (ri in resolveInfos) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || !seen.add(pkg)) continue
            val appInfo = ri.activityInfo.applicationInfo
            val name = ri.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
            val category = categorizeApp(pkg, appInfo)
            result.add(InstalledAppInfo(pkg, name, category, icon))
        }
        result.sortedBy { it.appName.lowercase() }
    }

    fun categorizeApp(packageName: String, appInfo: ApplicationInfo?): AppCategory {
        val lower = packageName.lowercase()
        return when {
            lower.contains("instagram") || lower.contains("facebook") ||
                lower.contains("twitter") || lower.contains("snapchat") ||
                lower.contains("reddit") || lower.contains("threads") ||
                lower.contains("pinterest") || lower.contains("linkedin") -> AppCategory.SOCIAL

            lower.contains("youtube") || lower.contains("netflix") ||
                lower.contains("hotstar") || lower.contains("primevideo") ||
                lower.contains("jiocinema") || lower.contains("spotify") ||
                lower.contains("mxplayer") || lower.contains("sonyliv") -> AppCategory.ENTERTAINMENT

            lower.contains("whatsapp") || lower.contains("telegram") ||
                lower.contains("discord") || lower.contains("messenger") ||
                lower.contains("signal") || lower.contains("skype") -> AppCategory.COMMUNICATION

            lower.contains("amazon") || lower.contains("flipkart") ||
                lower.contains("meesho") || lower.contains("myntra") ||
                lower.contains("ajio") || lower.contains("blinkit") -> AppCategory.SHOPPING

            lower.contains("duolingo") || lower.contains("unacademy") ||
                lower.contains("byjus") || lower.contains("physicswallah") ||
                lower.contains("khanacademy") || lower.contains("coursera") ||
                lower.contains("anki") || lower.contains("quizlet") -> AppCategory.EDUCATION

            appInfo != null && appInfo.category == ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            lower.contains("pubg") || lower.contains("freefire") ||
                lower.contains("ludo") || lower.contains("clash") ||
                lower.contains("game") -> AppCategory.GAMES

            else -> AppCategory.OTHER
        }
    }

    suspend fun getUsageReport(daysBack: Int): UsageReportData = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasUsageStatsPermission(context)) {
            return@withContext UsageReportData(
                hasPermission = false,
                totalScreenTimeMillis = 0L,
                apps = emptyList(),
                categoryTotals = emptyMap()
            )
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext UsageReportData(true, 0L, emptyList(), emptyMap())

        val endTime = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        if (daysBack <= 1) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        }
        val startTime = cal.timeInMillis

        val interval = when {
            daysBack <= 1 -> UsageStatsManager.INTERVAL_DAILY
            daysBack <= 7 -> UsageStatsManager.INTERVAL_WEEKLY
            else -> UsageStatsManager.INTERVAL_MONTHLY
        }

        val stats = usm.queryUsageStats(interval, startTime, endTime).orEmpty()
        val timeByPkg = mutableMapOf<String, Long>()
        for (s in stats) {
            if (s.totalTimeInForeground > 0L && s.packageName != context.packageName) {
                timeByPkg[s.packageName] = (timeByPkg[s.packageName] ?: 0L) + s.totalTimeInForeground
            }
        }

        val launchesByPkg = mutableMapOf<String, Int>()
        val events = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                launchesByPkg[event.packageName] = (launchesByPkg[event.packageName] ?: 0) + 1
            }
        }

        val pm = context.packageManager
        val total = timeByPkg.values.sum().coerceAtLeast(1L)
        val appList = mutableListOf<AppUsageStat>()
        val catTotals = mutableMapOf<AppCategory, Long>()

        for ((pkg, timeMs) in timeByPkg) {
            if (timeMs < 1000L) continue
            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
            }.getOrNull() ?: continue

            val appName = pm.getApplicationLabel(appInfo).toString()
            val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
            val cat = categorizeApp(pkg, appInfo)
            val pct = (timeMs.toFloat() / total.toFloat()) * 100f

            appList.add(
                AppUsageStat(
                    packageName = pkg,
                    appName = appName,
                    category = cat,
                    icon = icon,
                    totalTimeMillis = timeMs,
                    launchCount = launchesByPkg[pkg] ?: 0,
                    percentage = pct
                )
            )
            catTotals[cat] = (catTotals[cat] ?: 0L) + timeMs
        }

        UsageReportData(
            hasPermission = true,
            totalScreenTimeMillis = timeByPkg.values.sum(),
            apps = appList.sortedByDescending { it.totalTimeMillis },
            categoryTotals = catTotals
        )
    }

    suspend fun blockApps(
        apps: List<InstalledAppInfo>,
        mode: BlockMode,
        durationMinutes: Int = 0
    ) {
        val now = System.currentTimeMillis()
        val endMillis = if (durationMinutes > 0) now + durationMinutes * 60_000L else 0L
        val entities = apps.map {
            BlockedAppEntity(
                packageName = it.packageName,
                appName = it.appName,
                category = it.category.name,
                blockMode = mode.name,
                isBlocked = true,
                lockStartTimeMillis = now,
                lockEndTimeMillis = endMillis,
                tempAllowUntilMillis = 0L
            )
        }
        blockedAppDao.upsertAll(entities)

        if (mode == BlockMode.TIMER && durationMinutes > 0) {
            prefs.setTimerLockState(true, endMillis, durationMinutes)
        }
        if (endMillis > 0L) {
            scheduleAutoUnblock(endMillis)
        }
    }

    suspend fun unblockApps(packageNames: List<String>, wasCompleted: Boolean = false) {
        val now = System.currentTimeMillis()
        for (pkg in packageNames) {
            val existing = blockedAppDao.getByPackage(pkg)
            if (existing != null) {
                val mins = ((now - existing.lockStartTimeMillis) / 60_000L).toInt().coerceAtLeast(1)
                blockedAppDao.insertHistory(
                    BlockHistoryEntity(
                        packageName = existing.packageName,
                        appName = existing.appName,
                        blockMode = existing.blockMode,
                        startedAtMillis = existing.lockStartTimeMillis,
                        endedAtMillis = now,
                        durationMinutes = mins,
                        wasCompleted = wasCompleted
                    )
                )
            }
        }
        blockedAppDao.deleteByPackages(packageNames)
    }

    suspend fun grantTemporaryAllow(packageName: String, minutes: Int = 5) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        blockedAppDao.setTempAllow(packageName, until)
    }

    suspend fun clearExpiredBlocks() {
        val now = System.currentTimeMillis()
        val active = blockedAppDao.getActiveBlockedAppsSync()
        val expired = active.filter { it.lockEndTimeMillis > 0L && it.lockEndTimeMillis <= now }
        if (expired.isNotEmpty()) {
            unblockApps(expired.map { it.packageName }, wasCompleted = true)
        }
        val currentPrefs = prefs.preferencesFlow.first()
        if (currentPrefs.timerLockActive && currentPrefs.timerLockEndMillis in 1..now) {
            prefs.setTimerLockState(false, 0L, 0)
        }
    }

    private fun scheduleAutoUnblock(triggerAtMillis: Long) {
        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(1000L)
        val request = OneTimeWorkRequestBuilder<AutoUnblockWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong("trigger_at", triggerAtMillis).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "auto_unblock_$triggerAtMillis",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun addWebsiteRule(domain: String, category: String, isAllowListed: Boolean, durationMinutes: Int = 0) {
        val cleanDomain = domain.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
        if (cleanDomain.isBlank()) return
        val until = if (durationMinutes > 0) System.currentTimeMillis() + durationMinutes * 60_000L else 0L
        websiteDao.upsertRule(
            WebsiteRuleEntity(
                domain = cleanDomain,
                category = category,
                isBlocked = !isAllowListed,
                isAllowListed = isAllowListed,
                blockUntilMillis = until
            )
        )
    }

    suspend fun removeWebsiteRule(domain: String) {
        websiteDao.deleteRule(domain)
    }

    suspend fun addOrUpdateTask(task: StudyTaskEntity) {
        if (task.id == 0L) taskDao.insertTask(task) else taskDao.updateTask(task)
    }

    suspend fun toggleTaskCompletion(task: StudyTaskEntity) {
        val nowCompleted = !task.isCompleted
        taskDao.updateTask(
            task.copy(
                isCompleted = nowCompleted,
                completedAtMillis = if (nowCompleted) System.currentTimeMillis() else null
            )
        )
        if (nowCompleted) {
            updateTodayLog(addedTasks = 1)
        }
    }

    suspend fun deleteTask(task: StudyTaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun recordStudySession(presetName: String, studyMinutes: Int, breakMinutes: Int, actualMinutes: Int, completed: Boolean) {
        sessionDao.insertSession(
            StudySessionEntity(
                presetName = presetName,
                studyMinutes = studyMinutes,
                breakMinutes = breakMinutes,
                actualFocusMinutes = actualMinutes,
                wasCompleted = completed
            )
        )
        if (actualMinutes > 0) {
            updateTodayLog(addedFocusMinutes = actualMinutes)
        }
    }

    suspend fun updateTodayLog(
        addedFocusMinutes: Int = 0,
        addedTasks: Int = 0,
        screenTimeMinutes: Int? = null
    ) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = sessionDao.getLogForDate(today) ?: DailyFocusLogEntity(dateKey = today)
        val updated = existing.copy(
            focusMinutes = existing.focusMinutes + addedFocusMinutes,
            tasksCompleted = existing.tasksCompleted + addedTasks,
            screenTimeMinutes = screenTimeMinutes ?: existing.screenTimeMinutes
        )
        sessionDao.upsertDailyLog(updated)
    }

    suspend fun restoreFromBackupJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val tasksArr = root.optJSONArray("tasks") ?: return@withContext 0
        var count = 0
        for (i in 0 until tasksArr.length()) {
            val obj = tasksArr.getJSONObject(i)
            taskDao.insertTask(
                StudyTaskEntity(
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    subject = obj.optString("subject", "OTHER"),
                    priority = obj.optString("priority", "MEDIUM"),
                    dueDateMillis = obj.getLong("dueDateMillis"),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    reminderEnabled = obj.optBoolean("reminderEnabled", true)
                )
            )
            count++
        }
        count
    }
}
