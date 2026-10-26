package com.letdigitalfly.ibamail.ui.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letdigitalfly.ibamail.data.local.entities.ReminderEntity
import com.letdigitalfly.ibamail.data.repository.ReminderRepository
import com.letdigitalfly.ibamail.notifications.AlarmReminderReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class RemindersUiState(
    val todayReminders: List<ReminderEntity> = emptyList(),
    val tomorrowReminders: List<ReminderEntity> = emptyList(),
    val thisWeekReminders: List<ReminderEntity> = emptyList(),
    val laterReminders: List<ReminderEntity> = emptyList(),
    val completedReminders: List<ReminderEntity> = emptyList(),
    val pendingCount: Int = 0,
    val completedCount: Int = 0
)

class RemindersViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _currentTimeMs = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (isActive) {
                _currentTimeMs.value = System.currentTimeMillis()
                delay(60000)
            }
        }
    }

    val uiState: StateFlow<RemindersUiState> = combine(
        reminderRepository.activeRemindersFlow,
        reminderRepository.completedRemindersFlow,
        _currentTimeMs
    ) { active, completed, _ ->
        val (today, tomorrow, thisWeek, later) = partitionReminders(active)
        RemindersUiState(
            todayReminders = today,
            tomorrowReminders = tomorrow,
            thisWeekReminders = thisWeek,
            laterReminders = later,
            completedReminders = completed,
            pendingCount = active.size,
            completedCount = completed.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RemindersUiState()
    )

    fun toggleComplete(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepository.setReminderCompleted(reminder.id, !reminder.isCompleted)
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminderId)
        }
    }

    fun deleteReminder(reminderId: Long) {
        deleteReminder(reminderId.toString())
    }

    fun addReminder(
        context: Context,
        title: String,
        courseCode: String?,
        dueAt: Long
    ) {
        viewModelScope.launch {
            val triggerAt = (dueAt - 7200_000L).coerceAtLeast(System.currentTimeMillis() + 60_000L)
            val reminder = ReminderEntity(
                title = title.trim(),
                courseCode = courseCode?.trim()?.ifBlank { null },
                dueAt = dueAt,
                remindAt = triggerAt,
                done = false
            )
            val id = reminderRepository.insertReminder(reminder)

            // Set system alarm
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, AlarmReminderReceiver::class.java).apply {
                putExtra("reminder_title", reminder.title)
                putExtra("course_code", reminder.courseCode)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    private fun partitionReminders(
        reminders: List<ReminderEntity>
    ): Array<List<ReminderEntity>> {
        val today = mutableListOf<ReminderEntity>()
        val tomorrow = mutableListOf<ReminderEntity>()
        val thisWeek = mutableListOf<ReminderEntity>()
        val later = mutableListOf<ReminderEntity>()

        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfTomorrow = startOfToday + 86400_000L
        val startOfNextDay = startOfTomorrow + 86400_000L
        val endOfWeek = startOfToday + (7 * 86400_000L)

        for (reminder in reminders) {
            when {
                reminder.dueAt < startOfTomorrow -> today.add(reminder)
                reminder.dueAt < startOfNextDay -> tomorrow.add(reminder)
                reminder.dueAt < endOfWeek -> thisWeek.add(reminder)
                else -> later.add(reminder)
            }
        }

        return arrayOf(today, tomorrow, thisWeek, later)
    }
}
