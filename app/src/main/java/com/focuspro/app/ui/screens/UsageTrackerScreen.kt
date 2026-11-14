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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focuspro.app.data.repository.UsageReportData
import com.focuspro.app.ui.components.AppIconImage
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.components.ModuleHeader
import com.focuspro.app.ui.theme.AccentTracker
import com.focuspro.app.ui.theme.SoftCyan
import com.focuspro.app.util.PermissionHelper

@Composable
fun UsageTrackerScreen(
    isHindi: Boolean,
    usageReport: UsageReportData,
    onSelectPeriodDays: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val periods = listOf(1, 7, 30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ModuleHeader(
            title = if (isHindi) "ऐप उपयोग ट्रैकर" else "App Usage Tracker",
            subtitle = if (isHindi) "वास्तविक UsageStatsManager डेटा" else "Real device screen time and launch analytics",
            icon = Icons.Rounded.BarChart,
            accent = AccentTracker
        )

        TabRow(selectedTabIndex = selectedTab) {
            listOf(
                if (isHindi) "दैनिक" else "Daily",
                if (isHindi) "साप्ताहिक" else "Weekly",
                if (isHindi) "मासिक" else "Monthly"
            ).forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = {
                        selectedTab = idx
                        onSelectPeriodDays(periods[idx])
                    },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!usageReport.hasPermission) {
            GlassCard(accentColor = AccentTracker) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = AccentTracker)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "वास्तविक उपयोग आँकड़े देखने के लिए Usage Access अनुमति दें। हम अनुमानित या नकली डेटा नहीं दिखाते।"
                        else "Grant Usage Access permission to view real screen time. No simulated or estimated figures are ever shown.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { PermissionHelper.openUsageAccessSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTracker)
                ) {
                    Text(if (isHindi) "Usage Access चालू करें" else "Grant Usage Access")
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val totalMins = usageReport.totalScreenTimeMillis / 60_000L
                    val hrs = totalMins / 60
                    val mins = totalMins % 60
                    GlassCard(accentColor = AccentTracker) {
                        Text(
                            text = if (isHindi) "कुल स्क्रीन टाइम" else "Total Screen Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${hrs}h ${mins}m",
                            style = MaterialTheme.typography.headlineLarge,
                            color = AccentTracker
                        )
                        if (usageReport.apps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (if (isHindi) "सर्वाधिक उपयोग: " else "Most Used: ") + usageReport.apps.first().appName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = (if (isHindi) "न्यूनतम उपयोग: " else "Least Used: ") + usageReport.apps.last().appName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (usageReport.apps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MiniBarChart(
                                values = usageReport.apps.take(7).map { (it.totalTimeMillis / 60_000f).coerceAtLeast(1f) },
                                barColor = AccentTracker
                            )
                        }
                    }
                }

                if (usageReport.categoryTotals.isNotEmpty()) {
                    item {
                        GlassCard(accentColor = SoftCyan) {
                            Text(
                                text = if (isHindi) "श्रेणी अनुसार उपयोग" else "Category Distribution",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalMs = usageReport.totalScreenTimeMillis.coerceAtLeast(1L)
                            usageReport.categoryTotals.entries.sortedByDescending { it.value }.forEach { (cat, ms) ->
                                val pct = (ms.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isHindi) cat.labelHi else cat.labelEn, style = MaterialTheme.typography.bodyMedium)
                                    Text("${ms / 60_000L}m (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
                                }
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                items(usageReport.apps, key = { it.packageName }) { stat ->
                    val mins = stat.totalTimeMillis / 60_000L
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(drawable = stat.icon, contentDescription = stat.appName)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stat.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${mins}m • ${stat.launchCount} ${if (isHindi) "बार खुला" else "opens"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "%.1f%%".format(stat.percentage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentTracker
                            )
                        }
                    }
                }
            }
        }
    }
}
