package com.focuspro.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_pro_prefs")

enum class AppThemeMode { DARK, LIGHT, AUTO }
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी")
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val dailyStudyTargetMinutes: Int = 180,
    val soundEffectsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val notifyStudyReminders: Boolean = true,
    val notifyTaskDeadlines: Boolean = true,
    val notifyBreaks: Boolean = true,
    val notifyTimerCompletion: Boolean = true,
    val notifyDailySummary: Boolean = true,
    val vpnBlockerActive: Boolean = false,
    val studyModeActive: Boolean = false,
    val studyModeIsBreak: Boolean = false,
    val studyPhaseEndMillis: Long = 0L,
    val studyMinutesConfig: Int = 25,
    val breakMinutesConfig: Int = 5,
    val blockAppsDuringBreak: Boolean = false,
    val timerLockActive: Boolean = false,
    val timerLockEndMillis: Long = 0L,
    val timerLockDurationMinutes: Int = 0
)

class UserPreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val DAILY_TARGET_MIN = intPreferencesKey("daily_target_min")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val NOTIFY_STUDY = booleanPreferencesKey("notify_study")
        val NOTIFY_TASK = booleanPreferencesKey("notify_task")
        val NOTIFY_BREAK = booleanPreferencesKey("notify_break")
        val NOTIFY_TIMER = booleanPreferencesKey("notify_timer")
        val NOTIFY_SUMMARY = booleanPreferencesKey("notify_summary")
        val VPN_ACTIVE = booleanPreferencesKey("vpn_active")
        val STUDY_ACTIVE = booleanPreferencesKey("study_active")
        val STUDY_IS_BREAK = booleanPreferencesKey("study_is_break")
        val STUDY_PHASE_END = longPreferencesKey("study_phase_end")
        val STUDY_MIN_CONFIG = intPreferencesKey("study_min_config")
        val BREAK_MIN_CONFIG = intPreferencesKey("break_min_config")
        val BLOCK_DURING_BREAK = booleanPreferencesKey("block_during_break")
        val TIMER_LOCK_ACTIVE = booleanPreferencesKey("timer_lock_active")
        val TIMER_LOCK_END = longPreferencesKey("timer_lock_end")
        val TIMER_LOCK_DUR = intPreferencesKey("timer_lock_dur")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = runCatching {
                AppThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name)
            }.getOrDefault(AppThemeMode.DARK),
            language = runCatching {
                AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.name)
            }.getOrDefault(AppLanguage.ENGLISH),
            dailyStudyTargetMinutes = prefs[Keys.DAILY_TARGET_MIN] ?: 180,
            soundEffectsEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            notifyStudyReminders = prefs[Keys.NOTIFY_STUDY] ?: true,
            notifyTaskDeadlines = prefs[Keys.NOTIFY_TASK] ?: true,
            notifyBreaks = prefs[Keys.NOTIFY_BREAK] ?: true,
            notifyTimerCompletion = prefs[Keys.NOTIFY_TIMER] ?: true,
            notifyDailySummary = prefs[Keys.NOTIFY_SUMMARY] ?: true,
            vpnBlockerActive = prefs[Keys.VPN_ACTIVE] ?: false,
            studyModeActive = prefs[Keys.STUDY_ACTIVE] ?: false,
            studyModeIsBreak = prefs[Keys.STUDY_IS_BREAK] ?: false,
            studyPhaseEndMillis = prefs[Keys.STUDY_PHASE_END] ?: 0L,
            studyMinutesConfig = prefs[Keys.STUDY_MIN_CONFIG] ?: 25,
            breakMinutesConfig = prefs[Keys.BREAK_MIN_CONFIG] ?: 5,
            blockAppsDuringBreak = prefs[Keys.BLOCK_DURING_BREAK] ?: false,
            timerLockActive = prefs[Keys.TIMER_LOCK_ACTIVE] ?: false,
            timerLockEndMillis = prefs[Keys.TIMER_LOCK_END] ?: 0L,
            timerLockDurationMinutes = prefs[Keys.TIMER_LOCK_DUR] ?: 0
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setDailyTarget(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_TARGET_MIN] = minutes.coerceIn(15, 720) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun updateNotificationSetting(
        study: Boolean? = null,
        task: Boolean? = null,
        breaks: Boolean? = null,
        timer: Boolean? = null,
        summary: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            study?.let { prefs[Keys.NOTIFY_STUDY] = it }
            task?.let { prefs[Keys.NOTIFY_TASK] = it }
            breaks?.let { prefs[Keys.NOTIFY_BREAK] = it }
            timer?.let { prefs[Keys.NOTIFY_TIMER] = it }
            summary?.let { prefs[Keys.NOTIFY_SUMMARY] = it }
        }
    }

    suspend fun setVpnBlockerActive(active: Boolean) {
        context.dataStore.edit { it[Keys.VPN_ACTIVE] = active }
    }

    suspend fun setStudyModeState(
        active: Boolean,
        isBreak: Boolean,
        phaseEndMillis: Long,
        studyMinutes: Int,
        breakMinutes: Int,
        blockDuringBreak: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STUDY_ACTIVE] = active
            prefs[Keys.STUDY_IS_BREAK] = isBreak
            prefs[Keys.STUDY_PHASE_END] = phaseEndMillis
            prefs[Keys.STUDY_MIN_CONFIG] = studyMinutes
            prefs[Keys.BREAK_MIN_CONFIG] = breakMinutes
            prefs[Keys.BLOCK_DURING_BREAK] = blockDuringBreak
        }
    }

    suspend fun setTimerLockState(active: Boolean, endMillis: Long, durationMinutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMER_LOCK_ACTIVE] = active
            prefs[Keys.TIMER_LOCK_END] = endMillis
            prefs[Keys.TIMER_LOCK_DUR] = durationMinutes
        }
    }
}
