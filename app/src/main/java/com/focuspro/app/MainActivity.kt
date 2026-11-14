package com.focuspro.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focuspro.app.data.local.entity.BlockMode
import com.focuspro.app.data.preferences.AppLanguage
import com.focuspro.app.data.preferences.AppThemeMode
import com.focuspro.app.ui.components.ConfettiCelebrationOverlay
import com.focuspro.app.ui.components.FocusGradientBackground
import com.focuspro.app.ui.screens.AnalyticsScreen
import com.focuspro.app.ui.screens.AppBlockerScreen
import com.focuspro.app.ui.screens.BlockerHubScreen
import com.focuspro.app.ui.screens.HomeScreen
import com.focuspro.app.ui.screens.SettingsScreen
import com.focuspro.app.ui.screens.TasksAndStudyScreen
import com.focuspro.app.ui.screens.UsageTrackerScreen
import com.focuspro.app.ui.screens.WebsiteBlockerScreen
import com.focuspro.app.ui.theme.FocusProTheme
import com.focuspro.app.ui.viewmodel.FocusProViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: FocusProViewModel = viewModel()
            FocusProAppRoot(vm)
        }
    }
}

private enum class NavRoute(val route: String, val labelEn: String, val labelHi: String) {
    HOME("home", "Home", "होम"),
    BLOCKER("blocker", "Blocker", "ब्लॉकर"),
    LOCKS("locks", "Locks", "लॉक"),
    USAGE("usage", "Usage", "उपयोग"),
    TASKS("tasks", "Study", "स्टडी"),
    WEBSITE("website", "Web", "वेब"),
    ANALYTICS("analytics", "Analytics", "रिपोर्ट"),
    SETTINGS("settings", "Settings", "सेटिंग्स")
}

@Composable
private fun FocusProAppRoot(vm: FocusProViewModel) {
    val context = LocalContext.current
    val prefs by vm.preferences.collectAsState()
    val isHindi = prefs.language == AppLanguage.HINDI
    val installedApps by vm.installedApps.collectAsState()
    val blockedApps by vm.blockedApps.collectAsState()
    val blockHistory by vm.blockHistory.collectAsState()
    val todayAttempts by vm.todayAttempts.collectAsState()
    val websiteRules by vm.websiteRules.collectAsState()
    val websiteVisits by vm.websiteVisits.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val sessions by vm.studySessions.collectAsState()
    val dailyLogs by vm.dailyLogs.collectAsState()
    val usageReport by vm.usageReport.collectAsState()
    val permissions by vm.permissions.collectAsState()
    val showCelebration by vm.showCelebration.collectAsState()

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshPermissions() }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.notificationGranted) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: NavRoute.HOME.route

    val isDark = when (prefs.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.AUTO -> isSystemInDarkTheme()
    }

    FocusProTheme(themeMode = prefs.themeMode) {
        FocusGradientBackground(isDark = isDark) {
            Box {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        val bottomTabs = listOf(
                            NavRoute.HOME to Icons.Rounded.Home,
                            NavRoute.BLOCKER to Icons.Rounded.Block,
                            NavRoute.LOCKS to Icons.Rounded.LockClock,
                            NavRoute.USAGE to Icons.Rounded.BarChart,
                            NavRoute.TASKS to Icons.Rounded.School
                        )
                        NavigationBar {
                            bottomTabs.forEach { (dest, icon) ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(NavRoute.HOME.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = dest.labelEn) },
                                    label = { Text(if (isHindi) dest.labelHi else dest.labelEn, maxLines = 1) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoute.HOME.route,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(NavRoute.HOME.route) {
                            HomeScreen(
                                prefs = prefs,
                                isHindi = isHindi,
                                blockedApps = blockedApps,
                                tasks = tasks,
                                sessions = sessions,
                                dailyLogs = dailyLogs,
                                usageReport = usageReport,
                                breakdown = vm.getProductivityBreakdown(),
                                onStartFocus = { navController.navigate(NavRoute.TASKS.route) },
                                onOpenAppBlocker = { navController.navigate(NavRoute.BLOCKER.route) },
                                onOpenWebsiteBlocker = { navController.navigate(NavRoute.WEBSITE.route) },
                                onAddTask = { navController.navigate(NavRoute.TASKS.route) },
                                onOpenAnalytics = { navController.navigate(NavRoute.ANALYTICS.route) },
                                onOpenSettings = { navController.navigate(NavRoute.SETTINGS.route) }
                            )
                        }
                        composable(NavRoute.BLOCKER.route) {
                            AppBlockerScreen(
                                isHindi = isHindi,
                                installedApps = installedApps,
                                blockedApps = blockedApps,
                                permissions = permissions,
                                onBlockSelected = { pkgs -> vm.blockSelectedApps(pkgs, BlockMode.STANDARD, 0) },
                                onUnblockSelected = { pkgs -> vm.unblockPackages(pkgs) }
                            )
                        }
                        composable(NavRoute.LOCKS.route) {
                            BlockerHubScreen(
                                isHindi = isHindi,
                                prefs = prefs,
                                installedApps = installedApps,
                                blockedApps = blockedApps,
                                blockHistory = blockHistory,
                                onStartPermanentBlock = { pkgs, mode, mins -> vm.blockSelectedApps(pkgs, mode, mins) },
                                onStartTimerBlock = { pkgs, mins -> vm.blockSelectedApps(pkgs, BlockMode.TIMER, mins) },
                                onTempAllow = { pkg -> vm.tempAllow(pkg, 5) },
                                onUnblock = { pkgs -> vm.unblockPackages(pkgs) }
                            )
                        }
                        composable(NavRoute.USAGE.route) {
                            UsageTrackerScreen(
                                isHindi = isHindi,
                                usageReport = usageReport,
                                onSelectPeriodDays = { days -> vm.loadUsageReport(days) }
                            )
                        }
                        composable(NavRoute.TASKS.route) {
                            TasksAndStudyScreen(
                                isHindi = isHindi,
                                prefs = prefs,
                                tasks = tasks,
                                sessions = sessions,
                                installedApps = installedApps,
                                onSaveTask = { vm.saveTask(it) },
                                onToggleTask = { vm.toggleTask(it) },
                                onDeleteTask = { vm.deleteTask(it) },
                                onStartStudy = { s, b, brkBlock, pkgs -> vm.startStudySession(s, b, brkBlock, pkgs) },
                                onStopStudy = { vm.stopStudySession() }
                            )
                        }
                        composable(NavRoute.WEBSITE.route) {
                            WebsiteBlockerScreen(
                                isHindi = isHindi,
                                prefs = prefs,
                                rules = websiteRules,
                                visits = websiteVisits,
                                onAddRule = { d, c, allow, m -> vm.addWebsiteRule(d, c, allow, m) },
                                onRemoveRule = { d -> vm.removeWebsiteRule(d) },
                                onStopVpn = { vm.stopVpnBlocker() }
                            )
                        }
                        composable(NavRoute.ANALYTICS.route) {
                            AnalyticsScreen(
                                isHindi = isHindi,
                                breakdown = vm.getProductivityBreakdown(),
                                todayAttempts = todayAttempts,
                                tasks = tasks,
                                sessions = sessions,
                                dailyLogs = dailyLogs
                            )
                        }
                        composable(NavRoute.SETTINGS.route) {
                            SettingsScreen(
                                isHindi = isHindi,
                                prefs = prefs,
                                permissions = permissions,
                                onSetTheme = { vm.setThemeMode(it) },
                                onSetLanguage = { vm.setLanguage(it) },
                                onSetDailyTarget = { vm.setDailyTarget(it) },
                                onSetSound = { vm.setSoundEnabled(it) },
                                onSetAnimations = { vm.setAnimationsEnabled(it) },
                                onUpdateNotifications = { s, t, b, tm, sm -> vm.updateNotifications(s, t, b, tm, sm) },
                                onExportCsv = { context.startActivity(vm.exportCsvIntent()) },
                                onExportBackup = { context.startActivity(vm.exportBackupIntent()) }
                            )
                        }
                    }
                }
                ConfettiCelebrationOverlay(
                    visible = showCelebration,
                    onFinished = { vm.dismissCelebration() }
                )
            }
        }
    }
}
