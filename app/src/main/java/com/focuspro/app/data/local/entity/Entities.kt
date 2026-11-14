package com.focuspro.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BlockMode {
    STANDARD,
    PERMANENT_ALWAYS,
    PERMANENT_SCHEDULED,
    TIMER,
    STUDY_MODE
}

enum class AppCategory(val labelEn: String, val labelHi: String) {
    SOCIAL("Social Media", "सोशल मीडिया"),
    ENTERTAINMENT("Entertainment", "मनोरंजन"),
    GAMES("Games", "गेम्स"),
    SHOPPING("Shopping", "शॉपिंग"),
    COMMUNICATION("Communication", "कम्युनिकेशन"),
    EDUCATION("Education", "शिक्षा"),
    OTHER("Other Apps", "अन्य ऐप्स")
}

enum class StudySubject(val labelEn: String, val labelHi: String) {
    MATHEMATICS("Mathematics", "गणित"),
    SCIENCE("Science", "विज्ञान"),
    HINDI("Hindi", "हिंदी"),
    ENGLISH("English", "अंग्रेजी"),
    SANSKRIT("Sanskrit", "संस्कृत"),
    SOCIAL_STUDIES("Social Studies", "सामाजिक विज्ञान"),
    GK("General Knowledge", "सामान्य ज्ञान"),
    OTHER("Other", "अन्य")
}

enum class TaskPriority(val labelEn: String, val labelHi: String) {
    HIGH("High", "उच्च"),
    MEDIUM("Medium", "मध्यम"),
    LOW("Low", "सामान्य")
}

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String = AppCategory.OTHER.name,
    val blockMode: String = BlockMode.STANDARD.name,
    val isBlocked: Boolean = true,
    val lockStartTimeMillis: Long = System.currentTimeMillis(),
    val lockEndTimeMillis: Long = 0L, // 0L = indefinite / always
    val tempAllowUntilMillis: Long = 0L
)

@Entity(tableName = "block_history")
data class BlockHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val blockMode: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMinutes: Int,
    val wasCompleted: Boolean
)

@Entity(tableName = "blocked_attempts")
data class BlockedAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "website_rules")
data class WebsiteRuleEntity(
    @PrimaryKey val domain: String,
    val category: String = "Social Media",
    val isBlocked: Boolean = true,
    val isAllowListed: Boolean = false,
    val blockUntilMillis: Long = 0L, // 0L = always while VPN active
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "website_visits")
data class WebsiteVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val wasBlocked: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = StudySubject.OTHER.name,
    val priority: String = TaskPriority.MEDIUM.name,
    val dueDateMillis: Long,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val reminderEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetName: String,
    val studyMinutes: Int,
    val breakMinutes: Int,
    val actualFocusMinutes: Int,
    val wasCompleted: Boolean,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_focus_logs")
data class DailyFocusLogEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val focusMinutes: Int = 0,
    val screenTimeMinutes: Int = 0,
    val blockedAttempts: Int = 0,
    val tasksCompleted: Int = 0,
    val productivityScore: Int = 0
)
