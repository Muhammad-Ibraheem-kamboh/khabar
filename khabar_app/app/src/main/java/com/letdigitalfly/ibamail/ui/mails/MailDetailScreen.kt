package com.letdigitalfly.ibamail.ui.mails

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letdigitalfly.ibamail.data.local.entities.MailEntity
import com.letdigitalfly.ibamail.domain.model.MailBucket
import com.letdigitalfly.ibamail.domain.model.MailCategory
import com.letdigitalfly.ibamail.ui.components.CategoryTile
import com.letdigitalfly.ibamail.ui.components.Pill
import com.letdigitalfly.ibamail.ui.components.PrimaryButton
import com.letdigitalfly.ibamail.ui.components.SecondaryButton
import com.letdigitalfly.ibamail.ui.components.SegmentedControl
import com.letdigitalfly.ibamail.ui.theme.IosDivider
import com.letdigitalfly.ibamail.ui.theme.IosSecondaryText
import com.letdigitalfly.ibamail.ui.theme.KhabarBlue
import com.letdigitalfly.ibamail.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailDetailScreen(
    viewModel: MailDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val mail = state.mail

    if (state.showAiConfigHelp) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiHelp() },
            title = { Text(text = "AI Summaries not set up") },
            text = {
                Text(
                    text = "To enable intelligent summaries, add your API key in Settings > AI Summaries, or configure AI_API_KEY in local.properties. Works with Groq, Gemini, and OpenRouter."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAiHelp() }) {
                    Text(text = "OK", color = KhabarBlue)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = KhabarBlue
                        )
                    }
                },
                actions = {
                    if (mail != null) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    mail.headline.ifBlank { mail.subject }
                                )
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${mail.headline.ifBlank { mail.subject }}\n\n${mail.summaryText.ifBlank { mail.bodyPreview }}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share mail"))
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share",
                                tint = KhabarBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (mail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KhabarBlue)
            }
        } else {
            val category = MailCategory.fromName(mail.category)
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Header: Category Pill & Headline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryTile(category = category, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mail.courseCode ?: mail.senderName.ifBlank { "University" },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = KhabarBlue
                        )
                        Text(
                            text = DateUtils.formatRelative(mail.receivedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryText
                        )
                    }
                    if (mail.isAiGenerated) {
                        Pill(
                            text = "AI",
                            containerColor = KhabarBlue.copy(alpha = 0.1f),
                            contentColor = KhabarBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = mail.headline.ifBlank { mail.subject },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Control: Summary | Original mail
                SegmentedControl(
                    options = listOf("Summary", "Original mail"),
                    selectedIndex = state.selectedTab,
                    onOptionSelected = { viewModel.selectTab(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (state.selectedTab == 0) {
                    // SUMMARY TAB
                    SummaryTabContent(
                        mail = mail,
                        category = category,
                        state = state,
                        onAiClick = { viewModel.requestAiSummary() },
                        onAddReminder = { viewModel.addReminder(context) },
                        onMoveBucket = {
                            if (mail.bucket == MailBucket.HIDDEN.name) {
                                viewModel.moveToImportant()
                            } else {
                                viewModel.moveToHidden()
                            }
                        }
                    )
                } else {
                    // ORIGINAL MAIL TAB
                    OriginalMailTabContent(mail = mail)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SummaryTabContent(
    mail: MailEntity,
    category: MailCategory,
    state: MailDetailUiState,
    onAiClick: () -> Unit,
    onAddReminder: () -> Unit,
    onMoveBucket: () -> Unit
) {
    val context = LocalContext.current
    var whyExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Key Points Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "KEY POINTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = IosSecondaryText
                )

                Spacer(modifier = Modifier.height(12.dp))

                val bullets = listOfNotNull(
                    mail.bullet1?.takeIf { it.isNotBlank() },
                    mail.bullet2?.takeIf { it.isNotBlank() },
                    mail.bullet3?.takeIf { it.isNotBlank() }
                )

                if (bullets.isEmpty()) {
                    Text(
                        text = mail.summaryText.ifBlank { mail.bodyPreview },
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    bullets.forEach { bullet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(KhabarBlue)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Key Facts Specs Table (When, Where, Course, Link)
        val hasFacts = mail.extractedDateEpoch != null ||
                !mail.extractedRoom.isNullOrBlank() ||
                !mail.courseCode.isNullOrBlank() ||
                !mail.extractedLink.isNullOrBlank()

        if (hasFacts) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DETAILS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = IosSecondaryText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    mail.extractedDateEpoch?.let { epoch ->
                        FactRow(
                            icon = Icons.Rounded.AccessTime,
                            label = "When",
                            value = DateUtils.formatFullDate(epoch)
                        )
                    }

                    mail.extractedRoom?.let { room ->
                        FactRow(
                            icon = Icons.Rounded.LocationOn,
                            label = "Where",
                            value = room
                        )
                    }

                    mail.courseCode?.let { code ->
                        FactRow(
                            icon = Icons.Rounded.School,
                            label = "Course",
                            value = code
                        )
                    }

                    mail.extractedLink?.let { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    context.startActivity(intent)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = null,
                                tint = KhabarBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Open link",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = KhabarBlue,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = KhabarBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons based on category
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Calendar button
                SecondaryButton(
                    text = "Add to calendar",
                    leadingIcon = Icons.Rounded.CalendarToday,
                    onClick = {
                        val start = mail.extractedDateEpoch ?: (System.currentTimeMillis() + 86400_000L)
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, mail.headline.ifBlank { mail.subject })
                            putExtra(CalendarContract.Events.DESCRIPTION, mail.summaryText)
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3600_000L)
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                )

                // Remind me button
                SecondaryButton(
                    text = if (state.isReminderAdded) "Reminder added" else "Remind me before",
                    leadingIcon = if (state.isReminderAdded) Icons.Rounded.Check else Icons.Rounded.Notifications,
                    onClick = { if (!state.isReminderAdded) onAddReminder() }
                )

                // Set alarm button for exams/quizzes
                if (category == MailCategory.EXAM) {
                    SecondaryButton(
                        text = "Set alarm for morning of exam",
                        leadingIcon = Icons.Rounded.Alarm,
                        onClick = {
                            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                                putExtra(AlarmClock.EXTRA_MESSAGE, mail.headline.ifBlank { mail.subject })
                                putExtra(AlarmClock.EXTRA_HOUR, 7)
                                putExtra(AlarmClock.EXTRA_MINUTES, 30)
                                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                // AI button
                SecondaryButton(
                    text = if (state.isAiLoading) "Summarizing..." else "Explain with AI",
                    leadingIcon = Icons.Rounded.AutoAwesome,
                    onClick = onAiClick
                )
            }
        }

        // Why you see this disclosure
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { whyExpanded = !whyExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = IosSecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Why you see this",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Icon(
                        imageVector = if (whyExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = IosSecondaryText
                    )
                }

                AnimatedVisibility(visible = whyExpanded) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = mail.matchedReason.ifBlank { "Matched by university notice rules" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = IosSecondaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bucket: ${mail.bucket.replace('_', ' ')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryText
                        )
                    }
                }
            }
        }

        // Not for me / This is for me button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (mail.bucket == MailBucket.HIDDEN.name) "This is for me (Move to Important)" else "Not for me (Move to Hidden)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = KhabarBlue
                ),
                modifier = Modifier.clickable { onMoveBucket() }
            )
        }
    }
}

@Composable
private fun FactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IosSecondaryText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = IosSecondaryText,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OriginalMailTabContent(mail: MailEntity) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (mail.isNotificationPreview) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = KhabarBlue.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instant notification preview",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = KhabarBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This mail was captured from an incoming notification. To see the full email including attachments, open Outlook or Gmail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrimaryButton(
                        text = "Open Mail App",
                        onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.microsoft.office.outlook")
                                ?: context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "From: ${mail.senderName} <${mail.senderEmail}>",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subject: ${mail.subject}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Date: ${DateUtils.formatFullDate(mail.receivedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryText
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = IosDivider,
                    thickness = 0.5.dp
                )

                val bodyContent = mail.bodyText.ifBlank { mail.bodyPreview }
                Text(
                    text = bodyContent,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                )
            }
        }
    }
}
