package com.khabar.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.config.AppConfig
import com.khabar.app.domain.model.SummaryLanguage
import com.khabar.app.ui.components.LargeTitleScaffold
import com.khabar.app.ui.components.Pill
import com.khabar.app.ui.components.ToggleRow
import com.khabar.app.ui.theme.IosCard
import com.khabar.app.ui.theme.IosDivider
import com.khabar.app.ui.theme.IosRed
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToAlertSounds: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToDevTools: () -> Unit,
    onDataDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var slotHelpDialogTitle by remember { mutableStateOf<String?>(null) }
    var slotHelpDialogMessage by remember { mutableStateOf<String?>(null) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    if (slotHelpDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { slotHelpDialogTitle = null },
            title = { Text(text = slotHelpDialogTitle!!) },
            text = { Text(text = slotHelpDialogMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { slotHelpDialogTitle = null }) {
                    Text(text = "Understood", color = KhabarBlue)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = "Delete all data?") },
            text = {
                Text(
                    text = "This will permanently remove your profile, enrolled courses, interests, reminders, and all stored emails from this phone. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAllData(context, onDataDeleted)
                }) {
                    Text(text = "Delete Everything", color = IosRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "Cancel", color = IosSecondaryText)
                }
            }
        )
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(text = "Summary Language") },
            text = {
                Column {
                    Text(
                        text = "Changes the language used for email summaries and notification text. App buttons and titles stay in English.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SummaryLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSummaryLanguage(lang)
                                    showLanguagePicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (state.summaryLanguage == lang.code) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            if (state.summaryLanguage == lang.code) {
                                Pill(text = "Active", containerColor = KhabarBlue.copy(alpha = 0.12f), contentColor = KhabarBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text("Close", color = KhabarBlue)
                }
            }
        )
    }

    LargeTitleScaffold(
        title = "Settings",
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToEditProfile() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initials = state.profile?.let {
                            val f = it.firstName.firstOrNull()?.uppercase() ?: ""
                            val l = it.lastName.firstOrNull()?.uppercase() ?: ""
                            "$f$l".ifBlank { "U" }
                        } ?: "U"

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(KhabarBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val fullName = state.profile?.let { "${it.firstName} ${it.lastName}".trim() }
                            Text(
                                text = if (!fullName.isNullOrBlank()) fullName else "Student Profile",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${state.profile?.programCode ?: "Student"} • ${state.profile?.joinTerm ?: ""} • ${state.profile?.campus ?: "Main"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = IosSecondaryText
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Profile",
                            tint = KhabarBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ACCOUNTS GROUP
            item { SectionHeader("ACCOUNTS") }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(
                            label = "Microsoft Outlook",
                            value = if (state.isOutlookConfigured) "Connected" else "Not set up",
                            onClick = {
                                if (!state.isOutlookConfigured) {
                                    slotHelpDialogTitle = "Outlook Integration"
                                    slotHelpDialogMessage = "To enable Outlook Graph sync, supply MS_CLIENT_ID and MS_SIGNATURE_HASH in your local.properties file."
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        SettingsRow(
                            label = "Gmail",
                            value = if (state.isGmailConfigured) "Connected" else "Not set up",
                            onClick = {
                                if (!state.isGmailConfigured) {
                                    slotHelpDialogTitle = "Gmail Integration"
                                    slotHelpDialogMessage = "To enable Gmail OAuth sync, set GMAIL_ENABLED=true and configure Google Cloud OAuth client in local.properties."
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Instant Mode (Notifications)",
                            checked = state.instantModeEnabled,
                            onCheckedChange = { viewModel.setInstantMode(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Fast Mode (90s polling)",
                            checked = state.fastModeEnabled,
                            onCheckedChange = { viewModel.setFastMode(it) }
                        )
                    }
                }
            }

            // PREFERENCES GROUP
            item { SectionHeader("PREFERENCES") }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val currentLang = SummaryLanguage.fromCode(state.summaryLanguage).displayName
                        SettingsRow(
                            label = "Summary Language",
                            value = currentLang,
                            onClick = { showLanguagePicker = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        SettingsRow(
                            label = "AI Summaries",
                            value = if (state.isAiConfigured) "Active" else "Off",
                            onClick = onNavigateToAiSettings
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        SettingsRow(
                            label = "Alert Sounds",
                            value = "Custom tones",
                            onClick = onNavigateToAlertSounds
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        SettingsRow(
                            label = "My Courses",
                            value = "${state.courses.size} courses",
                            onClick = onNavigateToEditProfile
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Daily Digest (8:00 AM)",
                            checked = state.dailyDigestEnabled,
                            onCheckedChange = { viewModel.setDailyDigest(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Quiet during class",
                            checked = state.quietDuringClass,
                            onCheckedChange = { viewModel.setQuietDuringClass(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Ongoing Today Board",
                            checked = state.todayBoardEnabled,
                            onCheckedChange = { viewModel.setTodayBoard(it) }
                        )
                    }
                }
            }

            // SYSTEM & PRIVACY GROUP
            item { SectionHeader("SYSTEM & PRIVACY") }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(
                            label = "IBA Student Portal",
                            value = "Open link",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PORTAL_URL))
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        SettingsRow(
                            label = "Battery Optimization",
                            value = "Check",
                            onClick = {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        SettingsRow(
                            label = "Developer Tools",
                            value = "Simulations & tones",
                            onClick = onNavigateToDevTools
                        )
                    }
                }
            }

            // DELETE DATA GROUP
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteConfirm = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = IosRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete my data",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = IosRed
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = IosSecondaryText,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosSecondaryText
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = IosSecondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
