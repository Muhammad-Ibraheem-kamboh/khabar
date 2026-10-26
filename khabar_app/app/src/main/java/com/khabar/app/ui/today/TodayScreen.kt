package com.khabar.app.ui.today

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.khabar.app.ui.components.SecondaryButton
import com.khabar.app.ui.theme.IosBackground
import com.khabar.app.ui.theme.IosCard
import com.khabar.app.ui.theme.IosCardDark
import com.khabar.app.ui.theme.IosDivider
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import com.khabar.app.util.DateUtils

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onMailClick: (String) -> Unit,
    onNavigateToMails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    LargeTitleScaffold(
        title = "Today",
        subtitle = "${state.todayDateFormatted} • ${state.greeting}",
        actions = {
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
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
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
                Spacer(modifier = Modifier.height(90.dp))
            }
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
                text = "No urgent mails right now",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "When class changes or deadlines arrive, they will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryText
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
