package com.focuspro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focuspro.app.data.local.entity.DailyFocusLogEntity
import com.focuspro.app.data.local.entity.StudySessionEntity
import com.focuspro.app.data.local.entity.StudyTaskEntity
import com.focuspro.app.ui.components.AnimatedCircularProgress
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.components.ModuleHeader
import com.focuspro.app.ui.theme.AccentTracker
import com.focuspro.app.ui.theme.ElectricPurple
import com.focuspro.app.ui.theme.SoftCyan
import com.focuspro.app.util.ProductivityBreakdown

@Composable
fun AnalyticsScreen(
    isHindi: Boolean,
    breakdown: ProductivityBreakdown,
    todayAttempts: Int,
    tasks: List<StudyTaskEntity>,
    sessions: List<StudySessionEntity>,
    dailyLogs: List<DailyFocusLogEntity>
) {
    val totalFocusMinutes = sessions.sumOf { it.actualFocusMinutes }
    val completedTasks = tasks.count { it.isCompleted }
    val completionRate = if (tasks.isNotEmpty()) (completedTasks * 100) / tasks.size else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ModuleHeader(
            title = if (isHindi) "प्रोडक्टिविटी एनालिटिक्स" else "Productivity Analytics",
            subtitle = if (isHindi) "पारदर्शी स्कोर गणना और फोकस रिपोर्ट" else "Transparent score formula and session insights",
            icon = Icons.Rounded.Analytics,
            accent = AccentTracker
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(accentColor = ElectricPurple) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "दैनिक प्रोडक्टिविटी स्कोर" else "Daily Productivity Score",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHindi) "फोकस: ${breakdown.focusPoints}/45\nटास्क: ${breakdown.taskPoints}/35\nअनुशासन: ${breakdown.disciplinePoints}/20"
                                else "Focus Time: ${breakdown.focusPoints}/45\nTask Rate: ${breakdown.taskPoints}/35\nDiscipline: ${breakdown.disciplinePoints}/20",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedCircularProgress(
                            progress = breakdown.totalScore / 100f,
                            centerTitle = "${breakdown.totalScore}",
                            centerSubtitle = "/ 100",
                            size = 112.dp
                        )
                    }
                }
            }

            item {
                GlassCard(accentColor = SoftCyan) {
                    Text(
                        text = if (isHindi) "मुख्य आँकड़े" else "Key Metrics",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(if (isHindi) "कुल फोकस समय" else "Total Focus Hours", "${totalFocusMinutes / 60}h ${totalFocusMinutes % 60}m")
                    StatRow(if (isHindi) "आज रोके गए प्रयास" else "Blocked App Attempts Today", "$todayAttempts")
                    StatRow(if (isHindi) "टास्क पूर्णता दर" else "Task Completion Rate", "$completionRate%")
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { completionRate / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                GlassCard(accentColor = AccentTracker) {
                    Text(
                        text = if (isHindi) "साप्ताहिक / मासिक फोकस ट्रेंड" else "Weekly & Monthly Focus Trend",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val vals = dailyLogs.take(14).reversed().map { it.focusMinutes.toFloat() }
                        .ifEmpty { listOf(0f) }
                    MiniBarChart(values = vals, barColor = ElectricPurple)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
