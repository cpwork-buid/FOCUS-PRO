package com.focuspro.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuspro.app.FocusProApplication
import com.focuspro.app.data.local.entity.BlockMode
import com.focuspro.app.data.local.entity.StudyTaskEntity
import com.focuspro.app.data.preferences.AppLanguage
import com.focuspro.app.data.preferences.AppThemeMode
import com.focuspro.app.data.preferences.UserPreferences
import com.focuspro.app.data.repository.InstalledAppInfo
import com.focuspro.app.data.repository.UsageReportData
import com.focuspro.app.service.FocusTimerService
import com.focuspro.app.service.LocalVpnBlockerService
import com.focuspro.app.util.PermissionHelper
import com.focuspro.app.util.PermissionStatus
import com.focuspro.app.util.ProductivityBreakdown
import com.focuspro.app.util.ProductivityCalculator
import com.focuspro.app.util.ReportExporter
import com.focuspro.app.util.SoundAndHaptics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusProViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusProApplication
    val repo = app.repository

    val preferences: StateFlow<UserPreferences> = repo.prefs.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val blockedApps = repo.observeBlockedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockHistory = repo.observeBlockHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttempts = repo.observeTodayAttemptsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val websiteRules = repo.observeWebsiteRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val websiteVisits = repo.observeWebsiteVisits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks = repo.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions = repo.observeStudySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyLogs = repo.observeDailyLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _usageReport = MutableStateFlow(UsageReportData(false, 0L, emptyList(), emptyMap()))
    val usageReport: StateFlow<UsageReportData> = _usageReport.asStateFlow()

    private val _permissions = MutableStateFlow(PermissionHelper.getStatus(app))
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            repo.clearExpiredBlocks()
            _permissions.value = PermissionHelper.getStatus(app)
            _installedApps.value = repo.getInstalledUserApps()
            loadUsageReport(1)
        }
    }

    fun refreshPermissions() {
        _permissions.value = PermissionHelper.getStatus(app)
    }

    fun loadUsageReport(daysBack: Int) {
        viewModelScope.launch {
            _permissions.value = PermissionHelper.getStatus(app)
            _usageReport.value = repo.getUsageReport(daysBack)
        }
    }

    fun blockSelectedApps(packages: Set<String>, mode: BlockMode, durationMinutes: Int = 0) {
        viewModelScope.launch {
            val selected = _installedApps.value.filter { it.packageName in packages }
            if (selected.isEmpty()) return@launch
            repo.blockApps(selected, mode, durationMinutes)
            if (mode == BlockMode.TIMER && durationMinutes > 0) {
                FocusTimerService.start(app)
            }
        }
    }

    fun unblockPackages(packages: List<String>) {
        viewModelScope.launch {
            repo.unblockApps(packages, wasCompleted = false)
        }
    }

    fun tempAllow(packageName: String, minutes: Int = 5) {
        viewModelScope.launch {
            repo.grantTemporaryAllow(packageName, minutes)
        }
    }

    fun addWebsiteRule(domain: String, category: String, isAllowListed: Boolean, durationMinutes: Int = 0) {
        viewModelScope.launch {
            repo.addWebsiteRule(domain, category, isAllowListed, durationMinutes)
        }
    }

    fun removeWebsiteRule(domain: String) {
        viewModelScope.launch {
            repo.removeWebsiteRule(domain)
        }
    }

    fun stopVpnBlocker() {
        val intent = Intent(app, LocalVpnBlockerService::class.java).apply {
            action = LocalVpnBlockerService.ACTION_STOP
        }
        app.startService(intent)
    }

    fun saveTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repo.addOrUpdateTask(task)
        }
    }

    fun toggleTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            val willComplete = !task.isCompleted
            repo.toggleTaskCompletion(task)
            if (willComplete) {
                SoundAndHaptics.playCompletionChime(app, preferences.value.soundEffectsEnabled)
                if (preferences.value.animationsEnabled) {
                    _showCelebration.value = true
                }
            }
        }
    }

    fun dismissCelebration() {
        _showCelebration.value = false
    }

    fun deleteTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repo.deleteTask(task)
        }
    }

    fun startStudySession(
        studyMinutes: Int,
        breakMinutes: Int,
        blockDuringBreak: Boolean,
        selectedPackages: Set<String>
    ) {
        viewModelScope.launch {
            val endMillis = System.currentTimeMillis() + studyMinutes * 60_000L
            repo.prefs.setStudyModeState(
                active = true,
                isBreak = false,
                phaseEndMillis = endMillis,
                studyMinutes = studyMinutes,
                breakMinutes = breakMinutes,
                blockDuringBreak = blockDuringBreak
            )
            if (selectedPackages.isNotEmpty()) {
                val apps = _installedApps.value.filter { it.packageName in selectedPackages }
                repo.blockApps(apps, BlockMode.STUDY_MODE, durationMinutes = 0)
            }
            FocusTimerService.start(app)
        }
    }

    fun stopStudySession() {
        viewModelScope.launch {
            val prefs = preferences.value
            val remainingMin = ((prefs.studyPhaseEndMillis - System.currentTimeMillis()) / 60_000L).toInt().coerceAtLeast(0)
            val completedMin = (prefs.studyMinutesConfig - remainingMin).coerceAtLeast(0)
            if (!prefs.studyModeIsBreak && completedMin > 0) {
                repo.recordStudySession(
                    presetName = "${prefs.studyMinutesConfig}m Study",
                    studyMinutes = prefs.studyMinutesConfig,
                    breakMinutes = prefs.breakMinutesConfig,
                    actualMinutes = completedMin,
                    completed = false
                )
            }
            repo.prefs.setStudyModeState(false, false, 0L, prefs.studyMinutesConfig, prefs.breakMinutesConfig, prefs.blockAppsDuringBreak)
            val studyBlocked = blockedApps.value.filter { it.blockMode == BlockMode.STUDY_MODE.name }.map { it.packageName }
            if (studyBlocked.isNotEmpty()) {
                repo.unblockApps(studyBlocked, wasCompleted = false)
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { repo.prefs.setThemeMode(mode) }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { repo.prefs.setLanguage(lang) }
    fun setDailyTarget(min: Int) = viewModelScope.launch { repo.prefs.setDailyTarget(min) }
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { repo.prefs.setSoundEnabled(enabled) }
    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { repo.prefs.setAnimationsEnabled(enabled) }
    fun updateNotifications(
        study: Boolean? = null,
        task: Boolean? = null,
        breaks: Boolean? = null,
        timer: Boolean? = null,
        summary: Boolean? = null
    ) = viewModelScope.launch {
        repo.prefs.updateNotificationSetting(study, task, breaks, timer, summary)
    }

    fun getProductivityBreakdown(): ProductivityBreakdown {
        val todayLogs = dailyLogs.value.firstOrNull()
        val focusMins = todayLogs?.focusMinutes ?: 0
        val allTasks = tasks.value
        val completed = allTasks.count { it.isCompleted }
        return ProductivityCalculator.calculate(
            focusMinutes = focusMins,
            dailyTargetMinutes = preferences.value.dailyStudyTargetMinutes,
            completedTasks = completed,
            totalTasks = allTasks.size,
            blockedAttempts = todayAttempts.value
        )
    }

    fun exportCsvIntent(): Intent = ReportExporter.exportCsvReport(app, dailyLogs.value)

    fun exportBackupIntent(): Intent {
        val json = ReportExporter.createBackupJson(tasks.value, dailyLogs.value)
        return ReportExporter.shareBackupFile(app, json)
    }

    fun restoreBackup(json: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = runCatching { repo.restoreFromBackupJson(json) }.getOrDefault(0)
            onResult(count)
        }
    }
}
