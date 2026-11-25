package com.khabar.app.ui.today

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.domain.model.MailCategory
import com.khabar.app.ui.components.CategoryTile
import com.khabar.app.ui.components.CountdownRing
import com.khabar.app.ui.components.LargeTitleScaffold
import com.khabar.app.ui.components.Pill
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.theme.IosGreen
import com.khabar.app.ui.theme.IosRed
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import com.khabar.app.util.BatteryOptimizationHelper
import com.khabar.app.util.DateUtils
import com.khabar.app.util.DocumentScannerHelper
import com.khabar.app.util.ScannedDocumentResult
import com.khabar.app.ui.components.SecondaryButton
import com.khabar.app.ui.theme.IosBackground
import com.khabar.app.ui.theme.IosCard
import com.khabar.app.ui.theme.IosCardDark
import com.khabar.app.ui.theme.IosDivider

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onMailClick: (String) -> Unit,
    onNavigateToMails: () -> Unit,
    onNavigateToKhabri: () -> Unit = {},
    onNavigateToPortalWeb: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedNoticeResult by viewModel.scannedNoticeDialog.collectAsState()
    var showScanChooserSheet by remember { mutableStateOf(false) }

    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    val hasNotificationAccess = androidx.compose.runtime.remember(state.isRefreshing) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    val isBatteryOptimized = androidx.compose.runtime.remember(state.isRefreshing) {
        !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.scanBitmap(context, bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanUri(context, uri)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanUri(context, uri)
        }
    }

    LargeTitleScaffold(
        title = "Today",
        subtitle = "${state.todayDateFormatted} • ${state.greeting}",
        actions = {
            IconButton(onClick = onNavigateToKhabri) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = "Ask Khabri AI",
                    tint = KhabarBlue
                )
            }
            if (state.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = KhabarBlue
                )
            } else {
                IconButton(onClick = { viewModel.refreshAll() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = KhabarBlue
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 0. Notification Listener Access check
            if (!hasNotificationAccess) {
                item {
                    NotificationAccessCard(
                        onEnable = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }

            // 0b. Background Battery Optimization check (Vivo / OEM)
            if (isBatteryOptimized) {
                item {
                    BatteryOptimizationCard(
                        onFix = {
                            BatteryOptimizationHelper.openOemBackgroundSettings(context)
                        }
                    )
                }
            }

            // Unknown Course Confirmation Prompt if detected
            state.unknownCoursePromptCode?.let { courseCode ->
                item {
                    CoursePromptCard(
                        courseCode = courseCode,
                        onYes = { viewModel.answerCoursePrompt(courseCode, true) },
                        onNo = { viewModel.answerCoursePrompt(courseCode, false) }
                    )
                }
            }

            // Ask Khabri AI Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KhabarBlue.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, KhabarBlue.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToKhabri() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(KhabarBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask Khabri AI",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Say \"Hi Khabri updates\" to hear today's summary",
                                style = MaterialTheme.typography.bodySmall,
                                color = IosSecondaryText
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Actions: Notice Scanner & University Portal Sync
            item {
                QuickActionsRow(
                    onScanNotice = { showScanChooserSheet = true },
                    onNavigateToPortalWeb = { onNavigateToPortalWeb() }
                )
            }

            // Scanning indicator if OCR is actively processing
            if (isScanning) {
                item {
                    ScanningNoticeCard()
                }
            }

            // Next Class & Timetable Card (Live Campus Schedule)
            if (state.showNextClassWidget) {
                item {
                    NextClassCard(
                        state = state,
                        onUpdateAbsence = { courseId, newAbs ->
                            viewModel.updateCourseAbsence(courseId, newAbs)
                        }
                    )
                }
            }

            // Up Next Hero Card
            item {
                UpNextCard(
                    state = state,
                    onAddToCalendar = { reminder ->
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, reminder.title)
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, reminder.dueAt)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, reminder.dueAt + 3600_000)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }

            // Important Mails Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IMPORTANT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = IosSecondaryText
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = KhabarBlue
                        ),
                        modifier = Modifier.clickable { onNavigateToMails() }
                    )
                }
            }

            if (state.topImportantMails.isEmpty()) {
                item {
                    EmptyImportantCard()
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            state.topImportantMails.forEachIndexed { index, mail ->
                                ImportantMailRow(
                                    mail = mail,
                                    onClick = { onMailClick(mail.id) }
                                )
                                if (index < state.topImportantMails.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 68.dp),
                                        color = IosDivider,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Today's summary card
            item {
                Text(
                    text = "TODAY'S SUMMARY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = IosSecondaryText,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            item {
                SummaryStatsCard(
                    received = state.mailsReceivedToday,
                    forYou = state.mailsForYouToday,
                    hidden = state.mailsHiddenToday
                )
            }

            // Padding at bottom for floating tab bar
            item {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }

        if (showScanChooserSheet) {
            ScanChooserDialog(
                onDismiss = { showScanChooserSheet = false },
                onTakePhoto = { cameraLauncher.launch(null) },
                onPickImage = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickPdf = {
                    pdfLauncher.launch("application/pdf")
                }
            )
        }

        scannedNoticeResult?.let { result ->
            ScannedNoticeResultDialog(
                result = result,
                onSave = {
                    viewModel.saveScannedNotice(context, result) {
                        viewModel.dismissScannedDialog()
                    }
                },
                onDismiss = { viewModel.dismissScannedDialog() }
            )
        }
    }
}

@Composable
private fun UpNextCard(
    state: TodayUiState,
    onAddToCalendar: (com.khabar.app.data.local.entities.ReminderEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UP NEXT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = KhabarBlue
                )
                if (state.upNextReminder != null) {
                    Pill(
                        text = state.upNextCountdown,
                        containerColor = KhabarBlue.copy(alpha = 0.12f),
                        contentColor = KhabarBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.upNextReminder != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CountdownRing(
                        progress = state.upNextProgress,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = KhabarBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        state.upNextReminder.courseCode?.let { code ->
                            Text(
                                text = code,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = KhabarBlue
                            )
                        }
                        Text(
                            text = state.upNextReminder.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = DateUtils.formatRelative(state.upNextReminder.dueAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Add to calendar",
                        onClick = { onAddToCalendar(state.upNextReminder) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Rounded.CalendarToday
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(KhabarBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = KhabarBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "All caught up",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "No upcoming deadlines or exams scheduled today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportantMailRow(
    mail: MailEntity,
    onClick: () -> Unit
) {
    val category = MailCategory.fromName(mail.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryTile(
            category = category,
            modifier = Modifier.size(42.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mail.courseCode ?: mail.senderName.ifBlank { "University" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = IosSecondaryText
                )
                Text(
                    text = DateUtils.formatRelative(mail.receivedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = IosSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mail.headline.ifBlank { mail.subject },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (!mail.isRead) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mail.summaryText.ifBlank { mail.bodyPreview },
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!mail.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(KhabarBlue)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = IosSecondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyImportantCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No urgent announcements",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "When class changes, quiz alerts, or deadlines arrive, they will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SummaryStatsCard(
    received: Int,
    forYou: Int,
    hidden: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(count = received, label = "Received")
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(0.5.dp)
                    .background(IosDivider)
            )
            StatItem(count = forYou, label = "For you", highlight = true)
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(0.5.dp)
                    .background(IosDivider)
            )
            StatItem(count = hidden, label = "Hidden")
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (highlight) KhabarBlue else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = IosSecondaryText
        )
    }
}

@Composable
private fun CoursePromptCard(
    courseCode: String,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Course check",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = KhabarBlue
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Are you enrolled in $courseCode this semester?",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "No",
                    onClick = onNo,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Yes, add it",
                    onClick = onYes,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotificationAccessCard(onEnable: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3CD).copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Email Sync Inactive",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFC107)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android requires 'Notification Access' permission so Khabar can capture your incoming announcements from Gmail & Outlook.",
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = onEnable,
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enable Notification Access in Settings ↗",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BatteryOptimizationCard(onFix: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KhabarBlue.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, KhabarBlue.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.BatteryAlert,
                    contentDescription = null,
                    tint = KhabarBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keep Background Sync Active",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Allow Khabar to run unrestricted in the background so incoming IBA notices and deadline alerts are never delayed by battery saving.",
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onFix,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, KhabarBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚡ Allow Unrestricted Background Sync",
                    color = KhabarBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onScanNotice: () -> Unit,
    onNavigateToPortalWeb: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, KhabarBlue.copy(alpha = 0.35f)),
            modifier = Modifier
                .weight(1f)
                .clickable { onScanNotice() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(KhabarBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DocumentScanner,
                        contentDescription = null,
                        tint = KhabarBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Scan Notice",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Camera & PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = IosSecondaryText
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.35f)),
            modifier = Modifier
                .weight(1f)
                .clickable { onNavigateToPortalWeb() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9C27B0).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Portals & LMS",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "UMS & Sakai 2FA",
                        style = MaterialTheme.typography.labelSmall,
                        color = IosSecondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanningNoticeCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KhabarBlue.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, KhabarBlue.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = KhabarBlue
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Analyzing Document On-Device...",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Extracting text, fee deadlines, and exam schedules with ML Kit",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryText
                )
            }
        }
    }
}

@Composable
private fun ScanChooserDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onPickPdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Scan Campus Notice / Document",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Capture a physical notice board or upload a circular / fee slip to extract deadlines on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onTakePhoto()
                    }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = KhabarBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("📷 Take Photo of Notice Board", fontWeight = FontWeight.SemiBold)
                    }
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onPickImage()
                    }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = IosGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("🖼️ Choose Image from Gallery", fontWeight = FontWeight.SemiBold)
                    }
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onPickPdf()
                    }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = Color(0xFFE53935))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("📄 Upload PDF Circular / Fee Slip", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ScannedNoticeResultDialog(
    result: ScannedDocumentResult,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.isPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (result.isPdf) Color(0xFFE53935) else IosGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (result.isPdf) "PDF Extracted" else "Notice Scanned",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = result.detectedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (result.entities.eventStart != null) {
                    val deadlineStr = DateUtils.formatFullDate(result.entities.eventStart)
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9500).copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⏰ Detected Deadline: $deadlineStr",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF9500)
                            )
                        }
                    }
                }

                Text(
                    text = "Text Content (On-Device OCR):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = IosSecondaryText
                )

                Text(
                    text = result.rawText.take(300) + if (result.rawText.length > 300) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue)
            ) {
                Text("✓ Save Notice & Remind Me", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Discard")
            }
        }
    )
}

@Composable
private fun NextClassCard(
    state: TodayUiState,
    onUpdateAbsence: (courseId: Long, newAbsences: Int) -> Unit
) {
    val status = state.nextClassStatus
    val activeClass = status.activeClass

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (status.isOngoing) Color(0xFF4CAF50).copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Section label & countdown pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (status.isOngoing) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else KhabarBlue.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (status.isOngoing) Icons.Rounded.Schedule else Icons.Rounded.School,
                            contentDescription = null,
                            tint = if (status.isOngoing) Color(0xFF4CAF50) else KhabarBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status.isOngoing) "ONGOING CLASS" else "NEXT UPCOMING CLASS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (status.isOngoing) Color(0xFF4CAF50) else KhabarBlue
                    )
                }

                if (status.countdownText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (status.isOngoing) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else KhabarBlue.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = status.countdownText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (status.isOngoing) Color(0xFF4CAF50) else KhabarBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeClass != null) {
                // Course Code & Name
                Text(
                    text = "${activeClass.courseCode} — ${activeClass.courseName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Venue & Time details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = IosSecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeClass.room,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = IosSecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeClass.timeFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryText
                    )
                }

                // Absence Tracker Sub-Card (if enabled)
                if (state.showAbsenceTracker) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = IosDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Absence Buffer:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = IosSecondaryText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val isCritical = activeClass.absences >= 4
                                Text(
                                    text = "${activeClass.absences} / ${activeClass.maxAbsences} used",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCritical) IosRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (activeClass.absences >= 4) {
                                Text(
                                    text = if (activeClass.absences >= 5) "🚨 Absence limit reached! (F grade risk)" else "⚠️ 1 absence remaining!",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IosRed
                                )
                            }
                        }

                        // Absence adjustment buttons (- / +)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (activeClass.absences > 0) {
                                        onUpdateAbsence(activeClass.courseId, activeClass.absences - 1)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrement absence",
                                    tint = IosSecondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeClass.absences}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    onUpdateAbsence(activeClass.courseId, activeClass.absences + 1)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increment absence",
                                    tint = KhabarBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // No active class right now (either done for today or no classes today)
                Text(
                    text = status.statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = IosSecondaryText
                )
            }
        }
    }
}
