package com.focuspro.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focuspro.app.data.local.entity.BlockedAppEntity
import com.focuspro.app.data.local.entity.DailyFocusLogEntity
import com.focuspro.app.data.local.entity.StudySessionEntity
import com.focuspro.app.data.local.entity.StudyTaskEntity
import com.focuspro.app.data.preferences.UserPreferences
import com.focuspro.app.data.repository.UsageReportData
import com.focuspro.app.ui.components.AnimatedCircularProgress
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.theme.AccentBlocker
import com.focuspro.app.ui.theme.AccentHome
import com.focuspro.app.ui.theme.AccentStudy
import com.focuspro.app.ui.theme.AccentTasks
import com.focuspro.app.ui.theme.AccentTracker
import com.focuspro.app.ui.theme.AccentWeb
import com.focuspro.app.ui.theme.ElectricPurple
import com.focuspro.app.ui.theme.SoftCyan
import com.focuspro.app.util.ProductivityBreakdown
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    prefs: UserPreferences,
    isHindi: Boolean,
    blockedApps: List<BlockedAppEntity>,
    tasks: List<StudyTaskEntity>,
    sessions: List<StudySessionEntity>,
    dailyLogs: List<DailyFocusLogEntity>,
    usageReport: UsageReportData,
    breakdown: ProductivityBreakdown,
    onStartFocus: () -> Unit,
    onOpenAppBlocker: () -> Unit,
    onOpenWebsiteBlocker: () -> Unit,
    onAddTask: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> if (isHindi) "सुप्रभात, चैंपियन" else "Good Morning, Achiever"
        hour < 17 -> if (isHindi) "शुभ दोपहर, ध्यान केंद्रित रखें" else "Good Afternoon, Stay Focused"
        else -> if (isHindi) "शुभ संध्या, लक्ष्य की ओर बढ़ें" else "Good Evening, Keep Pushing"
    }

    val todayLog = dailyLogs.firstOrNull()
    val yesterdayLog = dailyLogs.getOrNull(1)
    val todayFocusMins = todayLog?.focusMinutes ?: 0
    val targetMins = prefs.dailyStudyTargetMinutes.coerceAtLeast(1)
    val focusProgress = (todayFocusMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f)
    val completedTasks = tasks.count { it.isCompleted }
    val screenTimeMins = if (usageReport.hasPermission) (usageReport.totalScreenTimeMillis / 60_000L).toInt() else null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = "FOCUS PRO", tint = SoftCyan)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FOCUS PRO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SoftCyan
                        )
                        Text(
                            text = "$greeting • $todayDate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row {
                    IconButton(onClick = onOpenAnalytics) {
                        Icon(Icons.Rounded.Analytics, contentDescription = "Analytics", tint = AccentTracker)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            }
        }

        // Main Focus Card
        item {
            GlassCard(accentColor = AccentHome) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "आज का फोकस स्कोर" else "Today's Focus Overview",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) "$todayFocusMins / $targetMins मिनट" else "$todayFocusMins / $targetMins min",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SoftCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "सेशन्स: ${sessions.size} • ब्लॉक ऐप्स: ${blockedApps.size}"
                            else "Sessions: ${sessions.size} • Blocked: ${blockedApps.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (screenTimeMins != null) {
                                if (isHindi) "स्क्रीन टाइम: ${screenTimeMins}m" else "Screen Time: ${screenTimeMins}m"
                            } else {
                                if (isHindi) "स्क्रीन टाइम: अनुमति आवश्यक" else "Screen Time: Grant Usage Access"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    AnimatedCircularProgress(
                        progress = focusProgress,
                        centerTitle = "${breakdown.totalScore}",
                        centerSubtitle = if (isHindi) "स्कोर" else "Score",
                        size = 112.dp,
                        strokeWidth = 10.dp,
                        animate = prefs.animationsEnabled
                    )
                }
            }
        }

        // Quick Action Buttons (responsive FlowRow for >= 320dp screens)
        item {
            Text(
                text = if (isHindi) "त्वरित कार्य" else "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                QuickActionTile(
                    title = if (isHindi) "फोकस शुरू करें" else "Start Focus",
                    icon = Icons.Rounded.PlayCircle,
                    accent = AccentStudy,
                    modifier = Modifier.weight(1f),
                    onClick = onStartFocus
                )
                QuickActionTile(
                    title = if (isHindi) "ऐप ब्लॉकर" else "App Blocker",
                    icon = Icons.Rounded.Block,
                    accent = AccentBlocker,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAppBlocker
                )
                QuickActionTile(
                    title = if (isHindi) "वेबसाइट ब्लॉकर" else "Website Blocker",
                    icon = Icons.Rounded.Language,
                    accent = AccentWeb,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWebsiteBlocker
                )
                QuickActionTile(
                    title = if (isHindi) "टास्क जोड़ें" else "Add Task",
                    icon = Icons.Rounded.AddTask,
                    accent = AccentTasks,
                    modifier = Modifier.weight(1f),
                    onClick = onAddTask
                )
            }
        }

        // Daily Performance & Yesterday Comparison
        item {
            GlassCard(accentColor = AccentTracker) {
                Text(
                    text = if (isHindi) "दैनिक प्रदर्शन और तुलना" else "Daily Performance & Comparison",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                val yFocus = yesterdayLog?.focusMinutes ?: 0
                val diff = todayFocusMins - yFocus
                val diffLabel = if (diff >= 0) "+$diff min" else "$diff min"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMini(
                        label = if (isHindi) "आज का फोकस" else "Today Focus",
                        value = "${todayFocusMins}m"
                    )
                    MetricMini(
                        label = if (isHindi) "कल की तुलना" else "vs Yesterday",
                        value = diffLabel
                    )
                    MetricMini(
                        label = if (isHindi) "पूर्ण कार्य" else "Tasks Done",
                        value = "$completedTasks/${tasks.size}"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isHindi) "पिछले 7 दिनों का फोकस ग्राफ" else "7-Day Focus Trend",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val chartPoints = dailyLogs.take(7).reversed().map { it.focusMinutes.toFloat() }
                    .ifEmpty { listOf(todayFocusMins.toFloat()) }
                MiniBarChart(values = chartPoints, barColor = SoftCyan)
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        accentColor = accent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MiniBarChart(values: List<Float>, barColor: Color) {
    val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        val count = values.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.55f).coerceAtMost(36.dp.toPx())
        values.forEachIndexed { index, v ->
            val ratio = (v / maxVal).coerceIn(0.06f, 1f)
            val barHeight = size.height * ratio
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}
