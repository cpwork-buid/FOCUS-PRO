package com.focuspro.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.focuspro.app.data.local.entity.DailyFocusLogEntity
import com.focuspro.app.data.local.entity.StudyTaskEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ReportExporter {

    fun exportCsvReport(
        context: Context,
        logs: List<DailyFocusLogEntity>
    ): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "focus_pro_report_${System.currentTimeMillis()}.csv")
        val sb = StringBuilder()
        sb.appendLine("Date,FocusMinutes,ScreenTimeMinutes,BlockedAttempts,TasksCompleted,ProductivityScore")
        logs.forEach { log ->
            sb.appendLine(
                "${log.dateKey},${log.focusMinutes},${log.screenTimeMinutes},${log.blockedAttempts},${log.tasksCompleted},${log.productivityScore}"
            )
        }
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createBackupJson(
        tasks: List<StudyTaskEntity>,
        logs: List<DailyFocusLogEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "FOCUS PRO")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val taskArr = JSONArray()
        tasks.forEach { t ->
            taskArr.put(JSONObject().apply {
                put("title", t.title)
                put("description", t.description)
                put("subject", t.subject)
                put("priority", t.priority)
                put("dueDateMillis", t.dueDateMillis)
                put("isCompleted", t.isCompleted)
                put("reminderEnabled", t.reminderEnabled)
            })
        }
        root.put("tasks", taskArr)

        val logArr = JSONArray()
        logs.forEach { l ->
            logArr.put(JSONObject().apply {
                put("dateKey", l.dateKey)
                put("focusMinutes", l.focusMinutes)
                put("screenTimeMinutes", l.screenTimeMinutes)
                put("blockedAttempts", l.blockedAttempts)
                put("tasksCompleted", l.tasksCompleted)
                put("productivityScore", l.productivityScore)
            })
        }
        root.put("logs", logArr)
        return root.toString(2)
    }

    fun shareBackupFile(context: Context, jsonContent: String): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "focus_pro_backup_${System.currentTimeMillis()}.json")
        file.writeText(jsonContent)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
