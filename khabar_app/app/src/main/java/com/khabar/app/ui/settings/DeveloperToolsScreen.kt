package com.khabar.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.data.local.entities.CourseEntity
import com.khabar.app.data.repository.MailRepository
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.domain.model.MailCategory
import com.khabar.app.notifications.KhabarNotificationManager
import com.khabar.app.notifications.ToneSynth
import com.khabar.app.sources.DemoMailSource
import com.khabar.app.sources.InstantNotificationListenerService
import com.khabar.app.ui.components.CategoryTile
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.components.SecondaryButton
import com.khabar.app.ui.theme.IosDivider
import com.khabar.app.ui.theme.IosGreen
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperToolsScreen(
    demoMailSource: DemoMailSource,
    mailRepository: MailRepository,
    profileRepository: ProfileRepository,
    notificationManager: KhabarNotificationManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val capturedExtras by InstantNotificationListenerService.capturedNotifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = KhabarBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            statusMessage?.let { msg ->
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = IosGreen.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = IosGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = IosGreen)
                        }
                    }
                }
            }

            // Quick Data Actions
            item { SectionHeader("DEMO DATA SEEDING") }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(
                            text = "Load Sample Profile (Ali Raza, BSCS Fall 23)",
                            onClick = {
                                coroutineScope.launch {
                                    profileRepository.saveAcademicProfile(
                                        firstName = "Ali",
                                        lastName = "Raza",
                                        programCode = "BSCS",
                                        joinTerm = "Fall 23",
                                        campus = "Main Campus",
                                        courses = listOf(
                                            CourseEntity(code = "CS201", section = "A", name = "Data Structures"),
                                            CourseEntity(code = "CS204", section = "A", name = "Computer Organization"),
                                            CourseEntity(code = "MTH201", section = "B", name = "Differential Equations"),
                                            CourseEntity(code = "SS102", section = "A", name = "Pakistan History")
                                        )
                                    )
                                    statusMessage = "Loaded demo profile (Ali Raza, BSCS Fall 23)."
                                }
                            }
                        )

                        SecondaryButton(
                            text = "Load All 30 Realistic Sample Mails",
                            onClick = {
                                coroutineScope.launch {
                                    val count = mailRepository.loadSampleMails()
                                    statusMessage = "Loaded $count sample emails into database."
                                }
                            }
                        )

                        SecondaryButton(
                            text = "Trigger Daily Morning Digest Notification",
                            onClick = {
                                coroutineScope.launch {
                                    notificationManager.postDailyDigest(3, 12)
                                    statusMessage = "Posted Daily Digest notification."
                                }
                            }
                        )
                    }
                }
            }

            // Simulate incoming mail by category
            item { SectionHeader("SIMULATE INCOMING MAIL (RULES + NOTIFY)") }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        MailCategory.entries.forEachIndexed { index, cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            val mail = demoMailSource.simulateIncoming(cat)
                                            mailRepository.processIncomingMail(mail)
                                            statusMessage = "Simulated incoming mail: ${cat.displayName}"
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryTile(category = cat, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Text(
                                    text = "Simulate",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = KhabarBlue
                                    )
                                )
                            }
                            if (index < MailCategory.entries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 58.dp),
                                    color = IosDivider,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }

            // Notification Extras Inspector
            item { SectionHeader("CAPTURED NOTIFICATIONS (${capturedExtras.size})") }
            if (capturedExtras.isEmpty()) {
                item {
                    Text(
                        text = "No notifications intercepted yet. Turn on Instant Mode in Settings, grant Notification Listener permission, and send an email to Outlook or Gmail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(capturedExtras) { extra ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Pkg: ${extra["pkg"] ?: "unknown"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = KhabarBlue
                            )
                            Text(
                                text = "Title: ${extra["title"] ?: ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Text: ${extra["text"] ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = IosSecondaryText
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
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
