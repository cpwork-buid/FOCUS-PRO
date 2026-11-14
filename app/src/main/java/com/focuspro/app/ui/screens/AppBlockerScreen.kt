package com.focuspro.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focuspro.app.data.local.entity.AppCategory
import com.focuspro.app.data.local.entity.BlockMode
import com.focuspro.app.data.local.entity.BlockedAppEntity
import com.focuspro.app.data.repository.InstalledAppInfo
import com.focuspro.app.ui.components.AppIconImage
import com.focuspro.app.ui.components.GlassCard
import com.focuspro.app.ui.components.ModuleHeader
import com.focuspro.app.ui.theme.AccentBlocker
import com.focuspro.app.ui.theme.ElectricPurple
import com.focuspro.app.ui.theme.SoftCyan
import com.focuspro.app.util.PermissionHelper
import com.focuspro.app.util.PermissionStatus

@Composable
fun AppBlockerScreen(
    isHindi: Boolean,
    installedApps: List<InstalledAppInfo>,
    blockedApps: List<BlockedAppEntity>,
    permissions: PermissionStatus,
    onBlockSelected: (Set<String>) -> Unit,
    onUnblockSelected: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    val blockedSet = remember(blockedApps) { blockedApps.map { it.packageName }.toSet() }

    val filteredApps = remember(installedApps, searchQuery, selectedCategory) {
        installedApps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || app.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ModuleHeader(
            title = if (isHindi) "ऐप ब्लॉकर" else "App Blocker",
            subtitle = if (isHindi) "इंस्टॉल किए गए ऐप्स को वास्तविक रूप से ब्लॉक करें" else "Select installed apps to block in real time",
            icon = Icons.Rounded.Block,
            accent = AccentBlocker
        )

        if (!permissions.accessibilityEnabled) {
            GlassCard(accentColor = AccentBlocker, modifier = Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = AccentBlocker)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "वास्तविक ब्लॉकिंग के लिए Accessibility अनुमति चालू करें।"
                        else "Enable Accessibility Service for real-time app blocking.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { PermissionHelper.openAccessibilitySettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlocker)
                ) {
                    Text(if (isHindi) "अनुमति चालू करें" else "Enable Service")
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(if (isHindi) "ऐप खोजें (YouTube, Instagram...)" else "Search apps (YouTube, Instagram...)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(if (isHindi) "सभी" else "All") }
                )
            }
            items(AppCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(if (isHindi) category.labelHi else category.labelEn) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { selectedPackages = filteredApps.map { it.packageName }.toSet() },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isHindi) "सभी चुनें" else "Select All", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = { selectedPackages = emptySet() },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isHindi) "चयन हटाएँ" else "Deselect", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onBlockSelected(selectedPackages)
                    selectedPackages = emptySet()
                },
                enabled = selectedPackages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlocker),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (isHindi) "ब्लॉक (${selectedPackages.size})" else "Block (${selectedPackages.size})",
                    maxLines = 1
                )
            }
            Button(
                onClick = {
                    onUnblockSelected(selectedPackages.toList())
                    selectedPackages = emptySet()
                },
                enabled = selectedPackages.any { it in blockedSet },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isHindi) "अनब्लॉक करें" else "Unblock", maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isSelected = app.packageName in selectedPackages
                val isCurrentlyBlocked = app.packageName in blockedSet
                GlassCard(
                    modifier = Modifier.clickable {
                        selectedPackages = if (isSelected) {
                            selectedPackages - app.packageName
                        } else {
                            selectedPackages + app.packageName
                        }
                    },
                    accentColor = if (isCurrentlyBlocked) AccentBlocker else ElectricPurple
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconImage(drawable = app.icon, contentDescription = app.appName)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = (if (isHindi) app.category.labelHi else app.category.labelEn) +
                                    if (isCurrentlyBlocked) (if (isHindi) " • ब्लॉक सक्रिय" else " • BLOCKED") else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrentlyBlocked) AccentBlocker else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedPackages = if (checked) {
                                    selectedPackages + app.packageName
                                } else {
                                    selectedPackages - app.packageName
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
