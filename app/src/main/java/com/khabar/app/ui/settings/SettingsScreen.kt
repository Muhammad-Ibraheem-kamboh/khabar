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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
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
    onNavigateToAiDefectVault: () -> Unit = {},
    onNavigateToPortalWeb: () -> Unit = {},
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
    var showBriefingTimePicker by remember { mutableStateOf(false) }
    var showLeadTimePicker by remember { mutableStateOf(false) }
    var showCustomLeadDialog by remember { mutableStateOf(false) }
    var customLeadText by remember { mutableStateOf("") }

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

    if (showBriefingTimePicker) {
        val briefingTimes = listOf(
            "07:30" to "7:30 AM (Morning - Recommended)",
            "08:30" to "8:30 AM (Before 9:00 AM class)",
            "12:30" to "12:30 PM (Midday check-in)",
            "18:00" to "6:00 PM (Evening recap)",
            "21:30" to "9:30 PM (Night wrap-up)",
            "23:00" to "11:00 PM (Late night prep)"
        )
        AlertDialog(
            onDismissRequest = { showBriefingTimePicker = false },
            title = { Text(text = "Briefing Delivery Time") },
            text = {
                Column {
                    Text(
                        text = "Choose anytime day or night for your daily lock-screen briefing with classes & deadlines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // CUSTOM TIME PICKER BUTTON (24/7 ANY TIME)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KhabarBlue.copy(alpha = 0.08f))
                            .clickable {
                                showBriefingTimePicker = false
                                val parts = state.morningBriefingTime.split(":")
                                val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
                                val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val formatted = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                                        viewModel.setMorningBriefingTime(context, formatted)
                                    },
                                    initialHour,
                                    initialMinute,
                                    false
                                ).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = KhabarBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pick Any Custom Time (Clock Dial)...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = KhabarBlue
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "QUICK PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    briefingTimes.forEach { (timeStr, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setMorningBriefingTime(context, timeStr)
                                    showBriefingTimePicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (state.morningBriefingTime == timeStr) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            if (state.morningBriefingTime == timeStr) {
                                Pill(text = "Active", containerColor = KhabarBlue.copy(alpha = 0.12f), contentColor = KhabarBlue)
                            }
                        }
                    }

                    val isCustom = briefingTimes.none { it.first == state.morningBriefingTime }
                    if (isCustom) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Time: ${formatBriefingTimeDisplay(state.morningBriefingTime)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Pill(text = "Active", containerColor = KhabarBlue.copy(alpha = 0.12f), contentColor = KhabarBlue)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBriefingTimePicker = false }) {
                    Text("Close", color = IosSecondaryText)
                }
            }
        )
    }

    if (showLeadTimePicker) {
        val leadTimes = listOf(
            15 to "15 minutes before",
            30 to "30 minutes before",
            60 to "1 hour before",
            120 to "2 hours before (Recommended)",
            240 to "4 hours before",
            360 to "6 hours before",
            720 to "12 hours before",
            1440 to "1 day (24 hrs) before"
        )
        AlertDialog(
            onDismissRequest = { showLeadTimePicker = false },
            title = { Text(text = "Deadline Alarm Warning") },
            text = {
                Column {
                    Text(
                        text = "Choose how far in advance Khabar sounds an alarm before an assignment or fee deadline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // CUSTOM LEAD BUTTON
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KhabarBlue.copy(alpha = 0.08f))
                            .clickable {
                                showLeadTimePicker = false
                                customLeadText = state.deadlineAlarmLeadMinutes.toString()
                                showCustomLeadDialog = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = KhabarBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set Custom Minutes / Hours...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = KhabarBlue
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "POPULAR PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    leadTimes.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDeadlineAlarmLeadMinutes(mins)
                                    showLeadTimePicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (state.deadlineAlarmLeadMinutes == mins) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            if (state.deadlineAlarmLeadMinutes == mins) {
                                Pill(text = "Active", containerColor = KhabarBlue.copy(alpha = 0.12f), contentColor = KhabarBlue)
                            }
                        }
                    }

                    val isCustomLead = leadTimes.none { it.first == state.deadlineAlarmLeadMinutes }
                    if (isCustomLead) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom: ${formatLeadTimeDisplay(state.deadlineAlarmLeadMinutes)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Pill(text = "Active", containerColor = KhabarBlue.copy(alpha = 0.12f), contentColor = KhabarBlue)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeadTimePicker = false }) {
                    Text("Close", color = IosSecondaryText)
                }
            }
        )
    }

    if (showCustomLeadDialog) {
        AlertDialog(
            onDismissRequest = { showCustomLeadDialog = false },
            title = { Text(text = "Custom Alarm Warning") },
            text = {
                Column {
                    Text(
                        text = "Enter how many minutes in advance Khabar should sound the alarm (e.g. 45 for 45 mins, 180 for 3 hours):",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customLeadText,
                        onValueChange = { customLeadText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Lead time in minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val mins = customLeadText.toIntOrNull()
                    if (mins != null && mins > 0) {
                        viewModel.setDeadlineAlarmLeadMinutes(mins)
                    }
                    showCustomLeadDialog = false
                }) {
                    Text("Save", color = KhabarBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomLeadDialog = false }) {
                    Text("Cancel", color = IosSecondaryText)
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
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        SettingsRow(
                            label = "Background Battery Optimization",
                            value = if (com.khabar.app.util.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) "Unrestricted (Best)" else "Optimized (Tap to fix)",
                            onClick = {
                                com.khabar.app.util.BatteryOptimizationHelper.openOemBackgroundSettings(context)
                            }
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
                            label = "AI Bug & Quality Vault",
                            value = "Self-Learning Logs",
                            onClick = onNavigateToAiDefectVault
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
                            label = "Next Class Live Widget",
                            checked = state.nextClassWidgetEnabled,
                            onCheckedChange = { viewModel.setNextClassWidget(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "5-Absence Limit Tracker",
                            checked = state.absenceTrackerEnabled,
                            onCheckedChange = { viewModel.setAbsenceTracker(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Daily Briefing (Classes & Deadlines)",
                            checked = state.morningBriefingEnabled,
                            onCheckedChange = { viewModel.setMorningBriefing(context, it) }
                        )
                        if (state.morningBriefingEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                            SettingsRow(
                                label = "Briefing Delivery Time",
                                value = formatBriefingTimeDisplay(state.morningBriefingTime),
                                onClick = { showBriefingTimePicker = true }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Deadline Alarm Warnings",
                            checked = state.deadlineAlarmEnabled,
                            onCheckedChange = { viewModel.setDeadlineAlarm(it) }
                        )
                        if (state.deadlineAlarmEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                            SettingsRow(
                                label = "Alarm Advance Warning",
                                value = formatLeadTimeDisplay(state.deadlineAlarmLeadMinutes),
                                onClick = { showLeadTimePicker = true }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Khabri Offline Intelligence",
                            checked = state.offlineAiEnabled,
                            onCheckedChange = { viewModel.setOfflineAi(it) }
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
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        ToggleRow(
                            label = "Focus on IBA Mails Only",
                            checked = state.focusIbaOnly,
                            onCheckedChange = { viewModel.setFocusIbaOnly(it) }
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
                            label = "University Portals & Web Sync",
                            value = "UMS & Sakai LMS",
                            onClick = onNavigateToPortalWeb
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

private fun formatBriefingTimeDisplay(timeStr: String): String {
    val parts = timeStr.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
    val amPm = if (h >= 12) "PM" else "AM"
    val displayH = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return String.format("%d:%02d %s", displayH, m, amPm)
}

private fun formatLeadTimeDisplay(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes mins before"
        minutes == 60 -> "1 hour before"
        minutes % 60 == 0 -> "${minutes / 60} hours before"
        else -> "$minutes mins before"
    }
}
