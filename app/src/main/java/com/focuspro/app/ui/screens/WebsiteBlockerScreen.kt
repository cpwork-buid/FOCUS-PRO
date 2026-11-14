package com.focuspro.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focuspro.app.data.local.entity.WebsiteRuleEntity
import com.focuspro.app.data.local.entity.WebsiteVisitEntity
import com.focuspro.app.data.preferences.UserPreferences
import com.focuspro.app.service.LocalVpnBlockerService
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.components.ModuleHeader
import com.focuspro.app.ui.theme.AccentBlocker
import com.focuspro.app.ui.theme.AccentWeb
import com.focuspro.app.ui.theme.SoftCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebsiteBlockerScreen(
    isHindi: Boolean,
    prefs: UserPreferences,
    rules: List<WebsiteRuleEntity>,
    visits: List<WebsiteVisitEntity>,
    onAddRule: (String, String, Boolean, Int) -> Unit,
    onRemoveRule: (String) -> Unit,
    onStopVpn: () -> Unit
) {
    val context = LocalContext.current
    var domainInput by remember { mutableStateOf("") }
    var isAllowMode by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableStateOf("0") }

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            context.startForegroundService(Intent(context, LocalVpnBlockerService::class.java))
        }
    }

    val topVisited = remember(visits) {
        visits.groupBy { it.domain }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ModuleHeader(
            title = if (isHindi) "वेबसाइट ब्लॉकर एवं ट्रैकर" else "Website Blocker & Tracker",
            subtitle = if (isHindi) "स्थानीय DNS VPN फ़िल्टरिंग (HTTPS डिक्रिप्ट नहीं होता)"
            else "Local DNS VPN filtering • HTTPS traffic is never decrypted",
            icon = Icons.Rounded.Language,
            accent = AccentWeb
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(accentColor = AccentWeb) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "स्थानीय VPN वेबसाइट फ़िल्टर" else "Local VPN Website Filter",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (prefs.vpnBlockerActive) {
                                    if (isHindi) "सक्रिय • DNS क्वेरी ट्रैक हो रही हैं" else "Active • DNS queries filtered locally"
                                } else {
                                    if (isHindi) "निष्क्रिय • चालू करने के लिए टैप करें" else "Inactive • Tap to enable local VPN"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (prefs.vpnBlockerActive) SoftCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.vpnBlockerActive,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    val prepareIntent = VpnService.prepare(context)
                                    if (prepareIntent != null) {
                                        vpnLauncher.launch(prepareIntent)
                                    } else {
                                        context.startForegroundService(Intent(context, LocalVpnBlockerService::class.java))
                                    }
                                } else {
                                    onStopVpn()
                                }
                            }
                        )
                    }
                }
            }

            item {
                GlassCard(accentColor = AccentWeb) {
                    Text(
                        text = if (isHindi) "वेबसाइट नियम जोड़ें" else "Add Website Rule",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        label = { Text(if (isHindi) "डोमेन (जैसे youtube.com)" else "Domain (e.g., youtube.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isAllowMode,
                            onClick = { isAllowMode = false },
                            label = { Text(if (isHindi) "ब्लॉक लिस्ट" else "Block List") }
                        )
                        FilterChip(
                            selected = isAllowMode,
                            onClick = { isAllowMode = true },
                            label = { Text(if (isHindi) "अनुमति लिस्ट" else "Allow List") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("youtube.com", "instagram.com", "facebook.com", "reddit.com").forEach { preset ->
                            OutlinedButton(onClick = { onAddRule(preset, "Social Media", false, 0) }) {
                                Text("+ $preset", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onAddRule(domainInput, "Custom", isAllowMode, timerMinutes.toIntOrNull() ?: 0)
                            domainInput = ""
                        },
                        enabled = domainInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentWeb),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isHindi) "नियम सहेजें" else "Save Website Rule", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (rules.isNotEmpty()) {
                item {
                    Text(
                        text = if (isHindi) "सक्रिय वेबसाइट नियम (${rules.size})" else "Active Website Rules (${rules.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(rules, key = { it.domain }) { rule ->
                    GlassCard(accentColor = if (rule.isAllowListed) SoftCyan else AccentBlocker) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.domain, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (rule.isAllowListed) (if (isHindi) "अनुमति प्राप्त" else "Allowed")
                                    else (if (isHindi) "ब्लॉक किया गया" else "Blocked"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (rule.isAllowListed) SoftCyan else AccentBlocker
                                )
                            }
                            IconButton(onClick = { onRemoveRule(rule.domain) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            item {
                GlassCard {
                    Text(
                        text = if (isHindi) "शीर्ष विज़िट/प्रयास डोमेन (DNS लॉग)" else "Most Queried Domains (Local DNS)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (topVisited.isEmpty()) {
                        Text(
                            text = if (isHindi) "डेटा उपलब्ध नहीं है। ट्रैकिंग के लिए स्थानीय VPN चालू करें।"
                            else "No DNS visit data yet. Enable Local VPN above to track domain requests.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        topVisited.forEach { (domain, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(domain, style = MaterialTheme.typography.bodyMedium)
                                Text("$count reqs", style = MaterialTheme.typography.labelMedium, color = SoftCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}
