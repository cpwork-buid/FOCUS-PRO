package com.focuspro.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focuspro.app.data.local.entity.StudySessionEntity
import com.focuspro.app.data.local.entity.StudySubject
import com.focuspro.app.data.local.entity.StudyTaskEntity
import com.focuspro.app.data.local.entity.TaskPriority
import com.focuspro.app.data.preferences.UserPreferences
import com.focuspro.app.data.repository.InstalledAppInfo
import com.focuspro.app.ui.components.AnimatedCircularProgress
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.components.ModuleHeader
import com.focuspro.app.ui.theme.AccentStudy
import com.focuspro.app.ui.theme.AccentTasks
import com.focuspro.app.ui.theme.SoftCyan
import kotlinx.coroutines.delay

@Composable
fun TasksAndStudyScreen(
    isHindi: Boolean,
    prefs: UserPreferences,
    tasks: List<StudyTaskEntity>,
    sessions: List<StudySessionEntity>,
    installedApps: List<InstalledAppInfo>,
    initialTab: Int = 0,
    onSaveTask: (StudyTaskEntity) -> Unit,
    onToggleTask: (StudyTaskEntity) -> Unit,
    onDeleteTask: (StudyTaskEntity) -> Unit,
    onStartStudy: (Int, Int, Boolean, Set<String>) -> Unit,
    onStopStudy: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var editingTask by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingTask = null
                        showTaskDialog = true
                    },
                    containerColor = AccentTasks
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Task", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ModuleHeader(
                title = if (isHindi) "टास्क और स्मार्ट स्टडी" else "Tasks & Smart Study Mode",
                subtitle = if (isHindi) "अध्ययन लक्ष्य और पोमोडोरो फोकस" else "Study goals, subjects, and distraction-free sessions",
                icon = Icons.Rounded.School,
                accent = if (selectedTab == 0) AccentTasks else AccentStudy
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isHindi) "टास्क मैनेजर" else "Tasks") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isHindi) "स्टडी मोड" else "Study Mode") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                TaskManagerTab(
                    isHindi = isHindi,
                    tasks = tasks,
                    onToggleTask = onToggleTask,
                    onEditTask = {
                        editingTask = it
                        showTaskDialog = true
                    },
                    onDeleteTask = onDeleteTask
                )
            } else {
                SmartStudyTab(
                    isHindi = isHindi,
                    prefs = prefs,
                    sessions = sessions,
                    installedApps = installedApps,
                    onStartStudy = onStartStudy,
                    onStopStudy = onStopStudy
                )
            }
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            isHindi = isHindi,
            existing = editingTask,
            onDismiss = { showTaskDialog = false },
            onSave = {
                onSaveTask(it)
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun TaskManagerTab(
    isHindi: Boolean,
    tasks: List<StudyTaskEntity>,
    onToggleTask: (StudyTaskEntity) -> Unit,
    onEditTask: (StudyTaskEntity) -> Unit,
    onDeleteTask: (StudyTaskEntity) -> Unit
) {
    var filterIndex by remember { mutableIntStateOf(0) }
    val now = System.currentTimeMillis()
    val filtered = remember(tasks, filterIndex) {
        when (filterIndex) {
            1 -> tasks.filter { !it.isCompleted }
            2 -> tasks.filter { it.isCompleted }
            3 -> tasks.filter { !it.isCompleted && it.dueDateMillis < now }
            else -> tasks
        }
    }

    val completedCount = tasks.count { it.isCompleted }
    val pct = if (tasks.isNotEmpty()) completedCount.toFloat() / tasks.size.toFloat() else 0f

    Column(modifier = Modifier.fillMaxSize()) {
        GlassCard(accentColor = AccentTasks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isHindi) "पूर्ण कार्य: $completedCount / ${tasks.size}" else "Completed: $completedCount / ${tasks.size}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("${(pct * 100).toInt()}%", color = AccentTasks, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val labels = if (isHindi) listOf("सभी", "लंबित", "पूर्ण", "समय-सीमा पार")
            else listOf("All", "Pending", "Completed", "Overdue")
            items(labels.size) { idx ->
                FilterChip(
                    selected = filterIndex == idx,
                    onClick = { filterIndex = idx },
                    label = { Text(labels[idx]) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { task ->
                val subj = runCatching { StudySubject.valueOf(task.subject) }.getOrDefault(StudySubject.OTHER)
                GlassCard(accentColor = AccentTasks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask(task) })
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${if (isHindi) subj.labelHi else subj.labelEn} • ${task.priority}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftCyan
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        TextButton(onClick = { onEditTask(task) }) {
                            Text(if (isHindi) "संपादित" else "Edit")
                        }
                        IconButton(onClick = { onDeleteTask(task) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartStudyTab(
    isHindi: Boolean,
    prefs: UserPreferences,
    sessions: List<StudySessionEntity>,
    installedApps: List<InstalledAppInfo>,
    onStartStudy: (Int, Int, Boolean, Set<String>) -> Unit,
    onStopStudy: () -> Unit
) {
    val presets = listOf(25 to 5, 50 to 10, 90 to 15)
    var studyMins by remember { mutableIntStateOf(25) }
    var breakMins by remember { mutableIntStateOf(5) }
    var blockDuringBreak by remember { mutableStateOf(false) }
    var selectedApps by remember {
        mutableStateOf(
            installedApps.filter {
                it.packageName.contains("youtube") ||
                    it.packageName.contains("instagram") ||
                    it.packageName.contains("telegram")
            }.map { it.packageName }.toSet()
        )
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(prefs.studyModeActive) {
        while (prefs.studyModeActive) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (prefs.studyModeActive) {
            item {
                val remMs = (prefs.studyPhaseEndMillis - now).coerceAtLeast(0L)
                val totalMs = ((if (prefs.studyModeIsBreak) prefs.breakMinutesConfig else prefs.studyMinutesConfig) * 60_000L)
                    .coerceAtLeast(1L).toFloat()
                val sec = remMs / 1000L
                val timeStr = "%02d:%02d".format(sec / 60, sec % 60)

                GlassCard(accentColor = AccentStudy) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedCircularProgress(
                            progress = (remMs.toFloat() / totalMs).coerceIn(0f, 1f),
                            centerTitle = timeStr,
                            centerSubtitle = if (prefs.studyModeIsBreak) {
                                if (isHindi) "ब्रेक समय" else "Break Time"
                            } else {
                                if (isHindi) "स्टडी सेशन" else "Studying"
                            },
                            size = 180.dp,
                            accentStart = AccentStudy,
                            accentEnd = SoftCyan,
                            animate = prefs.animationsEnabled
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onStopStudy,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTasks)
                        ) {
                            Text(if (isHindi) "सेशन समाप्त करें" else "End Study Session")
                        }
                    }
                }
            }
        } else {
            item {
                GlassCard(accentColor = AccentStudy) {
                    Text(
                        text = if (isHindi) "प्रीसेट स्टडी सेशन चुनें" else "Choose Study Preset",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { (s, b) ->
                            FilterChip(
                                selected = studyMins == s && breakMins == b,
                                onClick = {
                                    studyMins = s
                                    breakMins = b
                                },
                                label = { Text("${s}m + ${b}m") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi) "ब्रेक के दौरान भी ऐप्स ब्लॉक रखें" else "Keep apps blocked during break",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = blockDuringBreak, onCheckedChange = { blockDuringBreak = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "स्वतः चयनित डिस्ट्रैक्टिंग ऐप्स: ${selectedApps.size}"
                        else "Auto-selected distracting apps to block: ${selectedApps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onStartStudy(studyMins, breakMins, blockDuringBreak, selectedApps) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentStudy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isHindi) "स्मार्ट स्टडी शुरू करें" else "Start Smart Study", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (sessions.isNotEmpty()) {
            item {
                Text(
                    text = if (isHindi) "पिछले स्टडी सेशन्स" else "Session History",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(sessions.take(10)) { s ->
                GlassCard {
                    Text("${s.presetName} • ${s.actualFocusMinutes} min focus", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    isHindi: Boolean,
    existing: StudyTaskEntity?,
    onDismiss: () -> Unit,
    onSave: (StudyTaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var subject by remember {
        mutableStateOf(
            runCatching { StudySubject.valueOf(existing?.subject ?: StudySubject.MATHEMATICS.name) }
                .getOrDefault(StudySubject.MATHEMATICS)
        )
    }
    var priority by remember {
        mutableStateOf(
            runCatching { TaskPriority.valueOf(existing?.priority ?: TaskPriority.HIGH.name) }
                .getOrDefault(TaskPriority.HIGH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) (if (isHindi) "नया टास्क" else "New Task") else (if (isHindi) "टास्क संपादित करें" else "Edit Task")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isHindi) "टास्क का नाम" else "Task Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isHindi) "विवरण" else "Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(if (isHindi) "विषय" else "Subject", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudySubject.entries.forEach { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(if (isHindi) subj.labelHi else subj.labelEn) }
                        )
                    }
                }
                Text(if (isHindi) "प्राथमिकता" else "Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(if (isHindi) p.labelHi else p.labelEn) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (existing ?: StudyTaskEntity(
                        title = title.trim(),
                        dueDateMillis = System.currentTimeMillis() + 86_400_000L
                    )).copy(
                        title = title.trim(),
                        description = description.trim(),
                        subject = subject.name,
                        priority = priority.name
                    )
                    onSave(entity)
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isHindi) "सहेजें" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}
