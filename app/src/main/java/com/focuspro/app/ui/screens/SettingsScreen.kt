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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.focuspro.app.BuildConfig
import com.focuspro.app.data.preferences.AppLanguage
import com.focuspro.app.data.preferences.AppThemeMode
import com.focuspro.app.data.preferences.UserPreferences
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.components.ModuleHeader
import com.focuspro.app.ui.theme.ElectricPurple
import com.focuspro.app.ui.theme.SoftCyan
import com.focuspro.app.util.PermissionHelper
import com.focuspro.app.util.PermissionStatus

@Composable
fun SettingsScreen(
    isHindi: Boolean,
    prefs: UserPreferences,
    permissions: PermissionStatus,
    onSetTheme: (AppThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetDailyTarget: (Int) -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetAnimations: (Boolean) -> Unit,
    onUpdateNotifications: (Boolean?, Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onExportCsv: () -> Unit,
    onExportBackup: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ModuleHeader(
            title = if (isHindi) "प्रीमियम सेटिंग्स" else "Premium Settings",
            subtitle = if (isHindi) "थीम, भाषा, अनुमतियाँ और डेटा बैकअप" else "Theme, language, permissions, and data controls",
            icon = Icons.Rounded.Settings,
            accent = ElectricPurple
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(accentColor = ElectricPurple) {
                    Text(if (isHindi) "थीम और भाषा" else "Appearance & Language", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { onSetTheme(mode) },
                                label = { Text(mode.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = { onSetLanguage(lang) },
                                label = { Text(lang.displayName) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "दैनिक अध्ययन लक्ष्य: ${prefs.dailyStudyTargetMinutes} मिनट"
                        else "Daily Study Target: ${prefs.dailyStudyTargetMinutes} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = prefs.dailyStudyTargetMinutes.toFloat(),
                        onValueChange = { onSetDailyTarget(it.toInt()) },
                        valueRange = 30f..480f
                    )
                }
            }

            item {
                GlassCard(accentColor = SoftCyan) {
                    Text(if (isHindi) "अनुमति स्थिति (Permissions)" else "Permission Status", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionRow(
                        title = "Accessibility Service",
                        granted = permissions.accessibilityEnabled,
                        onClick = { PermissionHelper.openAccessibilitySettings(context) }
                    )
                    PermissionRow(
                        title = "Usage Access",
                        granted = permissions.usageAccessGranted,
                        onClick = { PermissionHelper.openUsageAccessSettings(context) }
                    )
                    PermissionRow(
                        title = "Overlay Permission",
                        granted = permissions.overlayGranted,
                        onClick = { PermissionHelper.openOverlaySettings(context) }
                    )
                }
            }

            item {
                GlassCard {
                    Text(if (isHindi) "ध्वनि, एनीमेशन और सूचनाएँ" else "Audio, Animations & Notifications", style = MaterialTheme.typography.titleMedium)
                    ToggleRow(if (isHindi) "ध्वनि प्रभाव" else "Sound Effects", prefs.soundEffectsEnabled, onSetSound)
                    ToggleRow(if (isHindi) "एनीमेशन" else "Animations", prefs.animationsEnabled, onSetAnimations)
                    ToggleRow(if (isHindi) "स्टडी रिमाइंडर" else "Study Reminders", prefs.notifyStudyReminders) {
                        onUpdateNotifications(it, null, null, null, null)
                    }
                    ToggleRow(if (isHindi) "टाइमर पूर्ण सूचना" else "Timer Completion Alerts", prefs.notifyTimerCompletion) {
                        onUpdateNotifications(null, null, null, it, null)
                    }
                    ToggleRow(if (isHindi) "दैनिक सारांश" else "Daily Usage Summary", prefs.notifyDailySummary) {
                        onUpdateNotifications(null, null, null, null, it)
                    }
                }
            }

            item {
                GlassCard {
                    Text(if (isHindi) "बैकअप और रिपोर्ट एक्सपोर्ट" else "Backup & Export Reports", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onExportCsv, modifier = Modifier.weight(1f)) {
                            Text(if (isHindi) "CSV रिपोर्ट" else "Export CSV")
                        }
                        OutlinedButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) {
                            Text(if (isHindi) "JSON बैकअप" else "Backup JSON")
                        }
                    }
                }
            }

            item {
                GlassCard {
                    Text(if (isHindi) "गोपनीयता एवं ऐप जानकारी" else "Privacy & App Info", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "FOCUS PRO v${BuildConfig.VERSION_NAME} • सभी डेटा आपके डिवाइस में स्थानीय रूप से सुरक्षित रहता है। कोई पासवर्ड, स्क्रीन सामग्री या HTTPS ट्रैफिक एकत्र नहीं किया जाता।"
                        else "FOCUS PRO v${BuildConfig.VERSION_NAME} • All data is stored locally on your device. No passwords, screen text, or decrypted HTTPS traffic are ever collected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onClick) {
            Text(if (granted) "Active" else "Grant")
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
