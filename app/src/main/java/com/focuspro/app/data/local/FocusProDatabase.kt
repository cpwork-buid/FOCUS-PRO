package com.focuspro.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focuspro.app.data.local.dao.BlockedAppDao
import com.focuspro.app.data.local.dao.StudySessionDao
import com.focuspro.app.data.local.dao.TaskDao
import com.focuspro.app.data.local.dao.WebsiteDao
import com.focuspro.app.data.local.entity.BlockHistoryEntity
import com.focuspro.app.data.local.entity.BlockedAppEntity
import com.focuspro.app.data.local.entity.BlockedAttemptEntity
import com.focuspro.app.data.local.entity.DailyFocusLogEntity
import com.focuspro.app.data.local.entity.StudySessionEntity
import com.focuspro.app.data.local.entity.StudyTaskEntity
import com.focuspro.app.data.local.entity.WebsiteRuleEntity
import com.focuspro.app.data.local.entity.WebsiteVisitEntity

@Database(
    entities = [
        BlockedAppEntity::class,
        BlockHistoryEntity::class,
        BlockedAttemptEntity::class,
        WebsiteRuleEntity::class,
        WebsiteVisitEntity::class,
        StudyTaskEntity::class,
        StudySessionEntity::class,
        DailyFocusLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusProDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun websiteDao(): WebsiteDao
    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: FocusProDatabase? = null

        fun getInstance(context: Context): FocusProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusProDatabase::class.java,
                    "focus_pro_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
