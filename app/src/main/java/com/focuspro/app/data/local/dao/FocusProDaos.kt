package com.focuspro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focuspro.app.data.local.entity.BlockHistoryEntity
import com.focuspro.app.data.local.entity.BlockedAppEntity
import com.focuspro.app.data.local.entity.BlockedAttemptEntity
import com.focuspro.app.data.local.entity.DailyFocusLogEntity
import com.focuspro.app.data.local.entity.StudySessionEntity
import com.focuspro.app.data.local.entity.StudyTaskEntity
import com.focuspro.app.data.local.entity.WebsiteRuleEntity
import com.focuspro.app.data.local.entity.WebsiteVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1 ORDER BY lockStartTimeMillis DESC")
    fun observeActiveBlockedApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    suspend fun getActiveBlockedAppsSync(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): BlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<BlockedAppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM blocked_apps WHERE packageName IN (:packageNames)")
    suspend fun deleteByPackages(packageNames: List<String>)

    @Query("DELETE FROM blocked_apps WHERE blockMode = :mode")
    suspend fun deleteByMode(mode: String)

    @Query("UPDATE blocked_apps SET tempAllowUntilMillis = :untilMillis WHERE packageName = :packageName")
    suspend fun setTempAllow(packageName: String, untilMillis: Long)

    @Query("SELECT * FROM block_history ORDER BY endedAtMillis DESC LIMIT 100")
    fun observeBlockHistory(): Flow<List<BlockHistoryEntity>>

    @Insert
    suspend fun insertHistory(entry: BlockHistoryEntity)

    @Query("SELECT * FROM blocked_attempts ORDER BY timestampMillis DESC LIMIT 200")
    fun observeAttempts(): Flow<List<BlockedAttemptEntity>>

    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE timestampMillis >= :sinceMillis")
    fun observeAttemptCountSince(sinceMillis: Long): Flow<Int>

    @Insert
    suspend fun insertAttempt(attempt: BlockedAttemptEntity)
}

@Dao
interface WebsiteDao {
    @Query("SELECT * FROM website_rules ORDER BY createdAtMillis DESC")
    fun observeRules(): Flow<List<WebsiteRuleEntity>>

    @Query("SELECT * FROM website_rules")
    suspend fun getRulesSync(): List<WebsiteRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: WebsiteRuleEntity)

    @Query("DELETE FROM website_rules WHERE domain = :domain")
    suspend fun deleteRule(domain: String)

    @Query("SELECT * FROM website_visits ORDER BY timestampMillis DESC LIMIT 300")
    fun observeVisits(): Flow<List<WebsiteVisitEntity>>

    @Insert
    suspend fun insertVisit(visit: WebsiteVisitEntity)

    @Query("DELETE FROM website_visits WHERE timestampMillis < :olderThanMillis")
    suspend fun pruneOldVisits(olderThanMillis: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun observeAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks")
    suspend fun getAllTasksSync(): List<StudyTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity): Long

    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    @Delete
    suspend fun deleteTask(task: StudyTaskEntity)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY completedAtMillis DESC LIMIT 100")
    fun observeSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions")
    suspend fun getAllSessionsSync(): List<StudySessionEntity>

    @Insert
    suspend fun insertSession(session: StudySessionEntity)

    @Query("SELECT * FROM daily_focus_logs ORDER BY dateKey DESC LIMIT 30")
    fun observeDailyLogs(): Flow<List<DailyFocusLogEntity>>

    @Query("SELECT * FROM daily_focus_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getLogForDate(dateKey: String): DailyFocusLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyLog(log: DailyFocusLogEntity)
}
