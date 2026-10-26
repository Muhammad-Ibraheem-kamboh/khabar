package com.letdigitalfly.ibamail.ui.mails

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letdigitalfly.ibamail.data.local.entities.MailEntity
import com.letdigitalfly.ibamail.domain.model.MailBucket
import com.letdigitalfly.ibamail.domain.model.MailCategory
import com.letdigitalfly.ibamail.ui.components.CategoryTile
import com.letdigitalfly.ibamail.ui.components.EmptyState
import com.letdigitalfly.ibamail.ui.components.LargeTitleScaffold
import com.letdigitalfly.ibamail.ui.components.Pill
import com.letdigitalfly.ibamail.ui.components.SegmentedControl
import com.letdigitalfly.ibamail.ui.theme.IosDivider
import com.letdigitalfly.ibamail.ui.theme.IosSecondaryText
import com.letdigitalfly.ibamail.ui.theme.KhabarBlue
import com.letdigitalfly.ibamail.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailsScreen(
    viewModel: MailsViewModel,
    onMailClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LargeTitleScaffold(
        title = "Mails",
        actions = {
            if (state.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = KhabarBlue
                )
            } else {
                IconButton(onClick = { viewModel.refresh() }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search mails, courses, senders...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = IosSecondaryText
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = IosSecondaryText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Segmented control with counts
            val segments = listOf(
                "Important (${state.importantCount})",
                "Good to know (${state.goodToKnowCount})",
                "Hidden (${state.hiddenCount})"
            )
            val selectedIndex = when (state.selectedBucket) {
                MailBucket.IMPORTANT -> 0
                MailBucket.GOOD_TO_KNOW -> 1
                MailBucket.HIDDEN -> 2
            }

            SegmentedControl(
                options = segments,
                selectedIndex = selectedIndex,
                onOptionSelected = { idx ->
                    val bucket = when (idx) {
                        0 -> MailBucket.IMPORTANT
                        1 -> MailBucket.GOOD_TO_KNOW
                        else -> MailBucket.HIDDEN
                    }
                    viewModel.selectBucket(bucket)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Mail groups
            val hasMails = state.todayMails.isNotEmpty() ||
                    state.yesterdayMails.isNotEmpty() ||
                    state.earlierMails.isNotEmpty()

            if (!hasMails) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyTitle = when (state.selectedBucket) {
                        MailBucket.IMPORTANT -> "No important mails"
                        MailBucket.GOOD_TO_KNOW -> "No good-to-know mails"
                        MailBucket.HIDDEN -> "No hidden mails"
                    }
                    val emptySubtitle = when (state.selectedBucket) {
                        MailBucket.IMPORTANT -> "Emails filtered as urgent for your courses will appear here."
                        MailBucket.GOOD_TO_KNOW -> "General updates and society activities will appear here."
                        MailBucket.HIDDEN -> "Mails that don't match your batch or campus are kept here."
                    }
                    EmptyState(
                        icon = Icons.Rounded.Inbox,
                        title = emptyTitle,
                        description = emptySubtitle
                    )
                }
            } else {
                val handleMailClick: (String) -> Unit = { id ->
                    viewModel.markAsRead(id)
                    onMailClick(id)
                }

                // PERF: har mail ab apna alag lazy item hai (pehle poora section ek hi item tha)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item(key = "top_spacer") { Spacer(modifier = Modifier.height(4.dp)) }

                    mailSection(
                        keyPrefix = "today",
                        title = "TODAY",
                        mails = state.todayMails,
                        onMailClick = handleMailClick
                    )
                    mailSection(
                        keyPrefix = "yesterday",
                        title = "YESTERDAY",
                        mails = state.yesterdayMails,
                        onMailClick = handleMailClick
                    )
                    mailSection(
                        keyPrefix = "earlier",
                        title = "EARLIER",
                        mails = state.earlierMails,
                        onMailClick = handleMailClick
                    )

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
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

/**
 * Ek date-section (header + mails) LazyColumn mein add karta hai.
 * Pehle/aakhri row ko rounded corners milte hain taake pehle wala "grouped card" look barkarar rahe.
 */
private fun LazyListScope.mailSection(
    keyPrefix: String,
    title: String,
    mails: List<MailEntity>,
    onMailClick: (String) -> Unit
) {
    if (mails.isEmpty()) return

    item(key = "${keyPrefix}_header") {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = title)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    itemsIndexed(
        items = mails,
        key = { _, mail -> mail.id },
        contentType = { _, _ -> "mail_row" }
    ) { index, mail ->
        val isFirst = index == 0
        val isLast = index == mails.lastIndex
        val shape = RoundedCornerShape(
            topStart = if (isFirst) 16.dp else 0.dp,
            topEnd = if (isFirst) 16.dp else 0.dp,
            bottomStart = if (isLast) 16.dp else 0.dp,
            bottomEnd = if (isLast) 16.dp else 0.dp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            MailItemRow(
                mail = mail,
                onClick = { onMailClick(mail.id) }
            )
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = IosDivider,
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun MailItemRow(
    mail: MailEntity,
    onClick: () -> Unit
) {
    val category = MailCategory.fromName(mail.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        CategoryTile(
            category = category,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mail.courseCode ?: mail.senderName.ifBlank { "University Notice" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (mail.courseCode != null) KhabarBlue else IosSecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

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

            // PERF: summaryText / matchedReason har baar JSON parse karte hain.
            // remember() se parse sirf tab hota hai jab wo JSON field sach mein badle.
            val summary = remember(mail.id, mail.keyPointsJson) {
                mail.summaryText.ifBlank { mail.bodyPreview }
            }
            val reason = remember(mail.id, mail.reasonsJson) { mail.matchedReason }

            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Pill(
                    text = reason,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!mail.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(KhabarBlue)
            )
        }
    }
}
