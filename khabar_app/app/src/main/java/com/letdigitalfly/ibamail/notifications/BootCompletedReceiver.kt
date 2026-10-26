package com.letdigitalfly.ibamail.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.letdigitalfly.ibamail.KhabarApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as KhabarApp
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Reschedule all pending reminders
            val now = System.currentTimeMillis()
            val pendingReminders = app.container.reminderRepository.allRemindersFlow.first()
                .filter { !it.done && it.remindAt > now }

            for (reminder in pendingReminders) {
                val alarmIntent = Intent(context, AlarmReminderReceiver::class.java).apply {
                    putExtra("reminder_id", reminder.id)
                    putExtra("reminder_title", reminder.title)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.id.toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.remindAt, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.remindAt, pendingIntent)
                    }
                } catch (_: Exception) {}
            }

            // 2. Schedule Daily Digest
            val profile = app.container.profileRepository.getProfile()
            if (profile != null && profile.digestEnabled) {
                scheduleDailyDigest(context, profile.digestTime)
            }
        }
    }

    companion object {
        fun scheduleDailyDigest(context: Context, digestTimeStr: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val parts = digestTimeStr.split(":").mapNotNull { it.toIntOrNull() }
            val hour = parts.getOrNull(0) ?: 7
            val minute = parts.getOrNull(1) ?: 30

            val now = LocalDateTime.now()
            var digestTarget = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))
            if (digestTarget.isBefore(now)) {
                digestTarget = digestTarget.plusDays(1)
            }

            val triggerEpochMs = digestTarget.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val digestIntent = Intent(context, DailyDigestReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9090,
                digestIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }
}
