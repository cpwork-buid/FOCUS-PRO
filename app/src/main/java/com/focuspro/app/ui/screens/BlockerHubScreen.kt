package com.focuspro.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focuspro.app.data.local.entity.BlockHistoryEntity
import com.focuspro.app.data.local.entity.BlockMode
import com.focuspro.app.data.local.entity.BlockedAppEntity
import com.focuspro.app.data.preferences.UserPreferences
import com.focuspro.app.data.repository.InstalledAppInfo
import com.focuspro.app.ui.components.AnimatedCircularProgress
import com.focuspro.app.ui.components.AppIconImage
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.components.ModuleHeader
import com.focuspro.app.ui.theme.AccentPermanent
import com.focuspro.app.ui.theme.AccentTimer
import com.focuspro.app.ui.theme.SoftCyan
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockerHubScreen(
    isHindi: Boolean,
    prefs: UserPreferences,
    installedApps: List<InstalledAppInfo>,
    blockedApps: List<BlockedAppEntity>,
    blockHistory: List<BlockHistoryEntity>,
    onStartPermanentBlock: (Set<String>, BlockMode, Int) -> Unit,
    onStartTimerBlock: (Set<String>, Int) -> Unit,
    onTempAllow: (String) -> Unit,
    onUnblock: (List<String>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAppPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ModuleHeader(
            title = if (isHindi) "स्थायी एवं टाइमर लॉक" else "Permanent & Timer Blocker",
            subtitle = if (isHindi) "समय-आधारित और स्थायी ऐप लॉकिंग" else "Persistent countdown and scheduled app locks",
            icon = Icons.Rounded.LockClock,
            accent = if (selectedTab == 0) AccentPermanent else AccentTimer
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (isHindi) "स्थायी ब्लॉकर" else "Permanent") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isHindi) "टाइमर ब्लॉकर" else "Timer Lock") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            PermanentBlockerTab(
                isHindi = isHindi,
                selectedCount = selectedPackages.size,
                blockedApps = blockedApps.filter {
                    it.blockMode == BlockMode.PERMANENT_ALWAYS.name ||
                        it.blockMode == BlockMode.PERMANENT_SCHEDULED.name
                },
                history = blockHistory.filter {
                    it.blockMode == BlockMode.PERMANENT_ALWAYS.name ||
                        it.blockMode == BlockMode.PERMANENT_SCHEDULED.name
                },
                onPickApps = { showAppPicker = true },
                onActivatePermanent = { mode, minutes ->
                    onStartPermanentBlock(selectedPackages, mode, minutes)
                    selectedPackages = emptySet()
                },
                onTempAllow = onTempAllow,
                onUnblock = { pkg -> onUnblock(listOf(pkg)) }
            )
        } else {
            TimerBlockerTab(
                isHindi = isHindi,
                prefs = prefs,
                selectedCount = selectedPackages.size,
                timerBlocked = blockedApps.filter { it.blockMode == BlockMode.TIMER.name },
                history = blockHistory.filter { it.blockMode == BlockMode.TIMER.name },
                onPickApps = { showAppPicker = true },
                onStartTimer = { mins ->
                    onStartTimerBlock(selectedPackages, mins)
                    selectedPackages = emptySet()
                },
                onEmergencyExit = {
                    val pkgs = blockedApps.filter { it.blockMode == BlockMode.TIMER.name }.map { it.packageName }
                    onUnblock(pkgs)
                }
            )
        }
    }

    if (showAppPicker) {
        AppMultiSelectDialog(
            isHindi = isHindi,
            installedApps = installedApps,
            initialSelection = selectedPackages,
            onDismiss = { showAppPicker = false },
            onConfirm = {
                selectedPackages = it
                showAppPicker = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermanentBlockerTab(
    isHindi: Boolean,
    selectedCount: Int,
    blockedApps: List<BlockedAppEntity>,
    history: List<BlockHistoryEntity>,
    onPickApps: () -> Unit,
    onActivatePermanent: (BlockMode, Int) -> Unit,
    onTempAllow: (String) -> Unit,
    onUnblock: (String) -> Unit
) {
    val presets = listOf(
        0 to (if (isHindi) "हमेशा ब्लॉक" else "Always Block"),
        24 * 60 to (if (isHindi) "24 घंटे" else "24 Hours"),
        3 * 24 * 60 to (if (isHindi) "3 दिन" else "3 Days"),
        7 * 24 * 60 to (if (isHindi) "7 दिन" else "7 Days"),
        15 * 24 * 60 to (if (isHindi) "15 दिन" else "15 Days"),
        30 * 24 * 60 to (if (isHindi) "30 दिन" else "30 Days")
    )
    var selectedMinutes by remember { mutableIntStateOf(7 * 24 * 60) }
    var customDays by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(accentColor = AccentPermanent) {
                Text(
                    text = if (isHindi) "A. हमेशा या निर्धारित अवधि ब्लॉक" else "Always or Scheduled Permanent Block",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onPickApps, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isHindi) "ऐप्स चुनें ($selectedCount चयनित)" else "Select Apps ($selectedCount selected)"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (mins, label) ->
                        FilterChip(
                            selected = selectedMinutes == mins && customDays.isBlank(),
                            onClick = {
                                selectedMinutes = mins
                                customDays = ""
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { customDays = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isHindi) "कस्टम दिन (वैकल्पिक)" else "Custom Days (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val customMins = customDays.toIntOrNull()?.times(24 * 60)
                        val finalMins = customMins ?: selectedMinutes
                        val mode = if (finalMins == 0) BlockMode.PERMANENT_ALWAYS else BlockMode.PERMANENT_SCHEDULED
                        onActivatePermanent(mode, finalMins)
                    },
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPermanent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isHindi) "स्थायी लॉक लागू करें" else "Activate Permanent Lock", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (blockedApps.isNotEmpty()) {
            item {
                Text(
                    text = if (isHindi) "सक्रिय स्थायी लॉक (${blockedApps.size})" else "Active Permanent Locks (${blockedApps.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(blockedApps, key = { it.packageName }) { app ->
                val remDays = if (app.lockEndTimeMillis > 0) {
                    val remMs = (app.lockEndTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                    val hrs = remMs / 3_600_000L
                    if (hrs >= 24) "${hrs / 24}d ${hrs % 24}h" else "${hrs}h"
                } else {
                    if (isHindi) "हमेशा" else "Always"
                }
                GlassCard(accentColor = AccentPermanent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = if (isHindi) "शेष: $remDays" else "Remaining: $remDays",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentPermanent
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { onTempAllow(app.packageName) }) {
                                Text(if (isHindi) "5m अनुमति" else "5m Allow")
                            }
                            TextButton(onClick = { onUnblock(app.packageName) }) {
                                Text(if (isHindi) "हटाएँ" else "Remove")
                            }
                        }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Text(
                    text = if (isHindi) "ब्लॉक इतिहास" else "Block History",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(history.take(10)) { h ->
                GlassCard {
                    Text("${h.appName} • ${h.durationMinutes} min • ${if (h.wasCompleted) "Completed" else "Stopped"}",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerBlockerTab(
    isHindi: Boolean,
    prefs: UserPreferences,
    selectedCount: Int,
    timerBlocked: List<BlockedAppEntity>,
    history: List<BlockHistoryEntity>,
    onPickApps: () -> Unit,
    onStartTimer: (Int) -> Unit,
    onEmergencyExit: () -> Unit
) {
    val presets = listOf(15, 30, 45, 60, 120, 180, 240, 360, 480)
    var selectedMinutes by remember { mutableIntStateOf(60) }
    var customMinutes by remember { mutableStateOf("") }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(prefs.timerLockActive) {
        while (prefs.timerLockActive) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val isRunning = timerBlocked.isNotEmpty() && timerBlocked.any { it.lockEndTimeMillis > now }
    val endMillis = timerBlocked.maxOfOrNull { it.lockEndTimeMillis } ?: prefs.timerLockEndMillis
    val totalDurMs = (prefs.timerLockDurationMinutes.coerceAtLeast(1) * 60_000L).toFloat()
    val remainingMs = (endMillis - now).coerceAtLeast(0L)
    val progress = if (isRunning) (remainingMs.toFloat() / totalDurMs).coerceIn(0f, 1f) else 0f

    val sec = remainingMs / 1000L
    val formattedCountdown = "%02d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isRunning) {
            item {
                GlassCard(accentColor = AccentTimer) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedCircularProgress(
                            progress = progress,
                            centerTitle = formattedCountdown,
                            centerSubtitle = if (isHindi) "लॉक सक्रिय" else "Lock Active",
                            size = 180.dp,
                            strokeWidth = 14.dp,
                            accentStart = AccentTimer,
                            accentEnd = SoftCyan,
                            animate = prefs.animationsEnabled
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi) "${timerBlocked.size} ऐप्स लॉक हैं (रीस्टार्ट के बाद भी सुरक्षित)"
                            else "${timerBlocked.size} apps locked (persists across reboot)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = { showEmergencyDialog = true }) {
                            Text(if (isHindi) "आपातकालीन निकास (Emergency Exit)" else "Emergency Exit")
                        }
                    }
                }
            }
        } else {
            item {
                GlassCard(accentColor = AccentTimer) {
                    Text(
                        text = if (isHindi) "टाइमर अवधि चुनें" else "Select Lock Duration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onPickApps, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (isHindi) "ऐप्स चुनें ($selectedCount चयनित)" else "Select Apps ($selectedCount selected)"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { mins ->
                            val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                            FilterChip(
                                selected = selectedMinutes == mins && customMinutes.isBlank(),
                                onClick = {
                                    selectedMinutes = mins
                                    customMinutes = ""
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isHindi) "कस्टम मिनट (वैकल्पिक)" else "Custom Minutes (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val finalMins = customMinutes.toIntOrNull()?.coerceAtLeast(1) ?: selectedMinutes
                            onStartTimer(finalMins)
                        },
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTimer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isHindi) "लॉक टाइमर शुरू करें" else "Start Lock Timer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Text(
                    text = if (isHindi) "टाइमर इतिहास" else "Lock History",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(history.take(10)) { h ->
                GlassCard {
                    Text("${h.appName} • ${h.durationMinutes} min", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text(if (isHindi) "आपातकालीन अनलॉक?" else "Confirm Emergency Exit") },
            text = {
                Text(
                    if (isHindi) "क्या आप वाकई सक्रिय टाइमर लॉक को समाप्त करना चाहते हैं?"
                    else "Are you sure you want to end the active timer lock early?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onEmergencyExit()
                    }
                ) {
                    Text(if (isHindi) "हाँ, अनलॉक करें" else "Yes, Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun AppMultiSelectDialog(
    isHindi: Boolean,
    installedApps: List<InstalledAppInfo>,
    initialSelection: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var tempSelection by remember { mutableStateOf(initialSelection) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(installedApps, query) {
        installedApps.filter { query.isBlank() || it.appName.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isHindi) "ऐप्स चुनें" else "Select Apps") },
        text = {
            Column(modifier = Modifier.height(340.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (isHindi) "खोजें..." else "Search...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = app.packageName in tempSelection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelection = if (checked) tempSelection - app.packageName else tempSelection + app.packageName
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(drawable = app.icon, contentDescription = app.appName, size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.appName,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { c ->
                                    tempSelection = if (c) tempSelection + app.packageName else tempSelection - app.packageName
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelection) }) {
                Text(if (isHindi) "चुनें (${tempSelection.size})" else "Done (${tempSelection.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}
