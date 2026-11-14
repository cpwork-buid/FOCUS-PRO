package com.focuspro.app.ui.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.focuspro.app.FocusProApplication
import com.focuspro.app.data.preferences.AppLanguage
import com.focuspro.app.data.preferences.UserPreferences
import com.focuspro.app.ui.components.FocusGradientBackground
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.theme.AccentBlocker
import com.focuspro.app.ui.theme.ElectricPurple
import com.focuspro.app.ui.theme.FocusProTheme
import com.focuspro.app.ui.theme.SoftCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlockingOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val blockMode = intent.getStringExtra(EXTRA_BLOCK_MODE) ?: "STANDARD"
        val lockEndMillis = intent.getLongExtra(EXTRA_LOCK_END_MILLIS, 0L)

        val app = application as FocusProApplication

        setContent {
            val prefs by app.preferencesManager.preferencesFlow.collectAsState(initial = UserPreferences())
            FocusProTheme(themeMode = prefs.themeMode) {
                BlockingScreenContent(
                    appName = appName,
                    blockMode = blockMode,
                    lockEndMillis = lockEndMillis,
                    isHindi = prefs.language == AppLanguage.HINDI,
                    studyModeActive = prefs.studyModeActive,
                    onGoHome = {
                        val home = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(home)
                        finish()
                    },
                    onEmergencyTempAllow = {
                        lifecycleScope.launch {
                            app.repository.grantTemporaryAllow(pkgName, minutes = 2)
                            finish()
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_pkg"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_BLOCK_MODE = "extra_block_mode"
        const val EXTRA_LOCK_END_MILLIS = "extra_lock_end"
    }
}

@Composable
private fun BlockingScreenContent(
    appName: String,
    blockMode: String,
    lockEndMillis: Long,
    isHindi: Boolean,
    studyModeActive: Boolean,
    onGoHome: () -> Unit,
    onEmergencyTempAllow: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingText = if (lockEndMillis > now) {
        val sec = (lockEndMillis - now) / 1000L
        val hrs = sec / 3600
        val mins = (sec % 3600) / 60
        val secs = sec % 60
        if (hrs > 0) "%02d:%02d:%02d".format(hrs, mins, secs) else "%02d:%02d".format(mins, secs)
    } else if (lockEndMillis == 0L) {
        if (isHindi) "स्थायी लॉक सक्रिय" else "Always Blocked"
    } else {
        "00:00"
    }

    val quotes = if (isHindi) {
        listOf(
            "आज का अनुशासन ही कल की सफलता है।",
            "ध्यान भटकाव को रोकें, अपने लक्ष्य पर अडिग रहें।",
            "हर मिनट जो आप बचाते हैं, वह आपके भविष्य को बनाता है।"
        )
    } else {
        listOf(
            "Discipline today builds freedom tomorrow.",
            "Stay locked in. Your goals matter more than a scroll.",
            "Every minute saved from distraction is a step closer to mastery."
        )
    }
    val quote = remember { quotes.random() }

    FocusGradientBackground(isDark = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FOCUS PRO",
                style = MaterialTheme.typography.headlineMedium,
                color = SoftCyan,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(accentColor = AccentBlocker) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Blocked",
                        tint = AccentBlocker,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHindi) "$appName ब्लॉक किया गया है" else "$appName is Blocked",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "सक्रिय मोड: $blockMode" else "Active Mode: $blockMode",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftCyan
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "शेष लॉक समय" else "Remaining Lock Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.headlineLarge,
                        color = ElectricPurple
                    )
                    if (studyModeActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "स्मार्ट स्टडी सेशन चालू है" else "Smart Study Session Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "\"$quote\"",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
            ) {
                Text(
                    text = if (isHindi) "होम स्क्रीन पर वापस जाएँ" else "Return to Home Screen",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEmergencyTempAllow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isHindi) "आपातकालीन 2 मिनट अनुमति (Emergency)" else "Emergency 2-Min Allow"
                )
            }
        }
    }
}
