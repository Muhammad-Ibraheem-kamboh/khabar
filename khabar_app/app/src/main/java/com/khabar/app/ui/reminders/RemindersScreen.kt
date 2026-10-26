package com.khabar.app.ui.reminders

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.data.local.entities.ReminderEntity
import com.khabar.app.ui.components.EmptyState
import com.khabar.app.ui.components.LargeTitleScaffold
import com.khabar.app.ui.components.Pill
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.components.SecondaryButton
import com.khabar.app.ui.theme.IosDivider
import com.khabar.app.ui.theme.IosOrange
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import com.khabar.app.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var completedExpanded by remember { mutableStateOf(false) }

    LargeTitleScaffold(
        title = "Reminders",
        subtitle = "${state.pendingCount} pending deadlines",
        actions = {
            IconButton(onClick = { showAddSheet = true }) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add reminder",
                    tint = KhabarBlue
                )
            }
        },
        modifier = modifier
    ) { padding ->
        val hasAny = state.todayReminders.isNotEmpty() ||
                state.tomorrowReminders.isNotEmpty() ||
                state.thisWeekReminders.isNotEmpty() ||
                state.laterReminders.isNotEmpty() ||
                state.completedReminders.isNotEmpty()

        if (!hasAny) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Rounded.EventAvailable,
                    title = "No reminders",
                    description = "When emails have deadlines or exam dates, tap 'Remind me' to track them here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                if (state.todayReminders.isNotEmpty()) {
                    item { SectionTitle("DUE TODAY") }
                    item {
                        ReminderCard(
                            reminders = state.todayReminders,
                            onToggle = { viewModel.toggleComplete(it) },
                            onDelete = { viewModel.deleteReminder(it.id) }
                        )
                    }
                }

                if (state.tomorrowReminders.isNotEmpty()) {
                    item { SectionTitle("TOMORROW") }
                    item {
                        ReminderCard(
                            reminders = state.tomorrowReminders,
                            onToggle = { viewModel.toggleComplete(it) },
                            onDelete = { viewModel.deleteReminder(it.id) }
                        )
                    }
                }

                if (state.thisWeekReminders.isNotEmpty()) {
                    item { SectionTitle("THIS WEEK") }
                    item {
                        ReminderCard(
                            reminders = state.thisWeekReminders,
                            onToggle = { viewModel.toggleComplete(it) },
                            onDelete = { viewModel.deleteReminder(it.id) }
                        )
                    }
                }

                if (state.laterReminders.isNotEmpty()) {
                    item { SectionTitle("LATER") }
                    item {
                        ReminderCard(
                            reminders = state.laterReminders,
                            onToggle = { viewModel.toggleComplete(it) },
                            onDelete = { viewModel.deleteReminder(it.id) }
                        )
                    }
                }

                if (state.completedReminders.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { completedExpanded = !completedExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMPLETED (${state.completedCount})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = IosSecondaryText
                            )
                            Icon(
                                imageVector = if (completedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = IosSecondaryText
                            )
                        }
                    }

                    if (completedExpanded) {
                        item {
                            ReminderCard(
                                reminders = state.completedReminders,
                                onToggle = { viewModel.toggleComplete(it) },
                                onDelete = { viewModel.deleteReminder(it.id) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }
    }

    if (showAddSheet) {
        AddReminderSheet(
            onDismiss = { showAddSheet = false },
            onSave = { title, course, dueAt ->
                viewModel.addReminder(context, title, course, dueAt)
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
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
private fun ReminderCard(
    reminders: List<ReminderEntity>,
    onToggle: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            reminders.forEachIndexed { index, reminder ->
                ReminderRow(
                    reminder = reminder,
                    onToggle = { onToggle(reminder) },
                    onDelete = { onDelete(reminder) }
                )
                if (index < reminders.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 54.dp),
                        color = IosDivider,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isUrgent = !reminder.isCompleted && (reminder.dueAt - System.currentTimeMillis()) < 86400_000L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (reminder.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (reminder.isCompleted) "Completed" else "Pending",
                tint = if (reminder.isCompleted) KhabarBlue else IosSecondaryText,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            reminder.courseCode?.let { code ->
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KhabarBlue
                )
            }
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (reminder.isCompleted) IosSecondaryText else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = DateUtils.formatRelative(reminder.dueAt),
                style = MaterialTheme.typography.bodySmall,
                color = if (isUrgent) IosOrange else IosSecondaryText
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete",
                tint = IosSecondaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderSheet(
    onDismiss: () -> Unit,
    onSave: (title: String, course: String?, dueAt: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(0) } // 0 = Tonight, 1 = Tomorrow, 2 = In 3 days, 3 = In 1 week

    val calendar = Calendar.getInstance()
    val dueAt = remember(selectedPreset) {
        val cal = Calendar.getInstance()
        when (selectedPreset) {
            0 -> {
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
            }
            1 -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
            }
            2 -> {
                cal.add(Calendar.DAY_OF_YEAR, 3)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
            }
            else -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
            }
        }
        cal.timeInMillis
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "New Reminder",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. CS201 Assignment 2 submission") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = courseCode,
                onValueChange = { courseCode = it.uppercase() },
                label = { Text("Course code (optional)") },
                placeholder = { Text("e.g. CS201") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DUE DATE",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = IosSecondaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            val presets = listOf("Tonight", "Tomorrow", "In 3 days", "In 1 week")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { index, label ->
                    val isSelected = selectedPreset == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) KhabarBlue else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedPreset = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Save Reminder",
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(title, courseCode, dueAt)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
