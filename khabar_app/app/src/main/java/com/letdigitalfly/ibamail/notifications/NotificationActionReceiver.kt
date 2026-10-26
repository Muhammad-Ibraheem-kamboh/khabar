package com.letdigitalfly.ibamail.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import com.letdigitalfly.ibamail.KhabarApp
import com.letdigitalfly.ibamail.data.local.entities.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CALENDAR = "com.letdigitalfly.ibamail.ACTION_CALENDAR"
        const val ACTION_REMIND_ME = "com.letdigitalfly.ibamail.ACTION_REMIND_ME"
        const val ACTION_SET_ALARM = "com.letdigitalfly.ibamail.ACTION_SET_ALARM"
        const val ACTION_SHARE = "com.letdigitalfly.ibamail.ACTION_SHARE"
        const val ACTION_OPEN_LINK = "com.letdigitalfly.ibamail.ACTION_OPEN_LINK"
        const val ACTION_MARK_DONE = "com.letdigitalfly.ibamail.ACTION_MARK_DONE"
        const val ACTION_OPEN_PORTAL = "com.letdigitalfly.ibamail.ACTION_OPEN_PORTAL"
        const val ACTION_EMAIL_OFFICE = "com.letdigitalfly.ibamail.ACTION_EMAIL_OFFICE"

        const val EXTRA_MAIL_ID = "extra_mail_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_COURSE = "extra_course"
        const val EXTRA_START = "extra_start"
        const val EXTRA_END = "extra_end"
        const val EXTRA_ALL_DAY = "extra_all_day"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_RECIPIENT = "extra_recipient"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_CALENDAR -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Class / Event"
                val start = intent.getLongExtra(EXTRA_START, System.currentTimeMillis() + 3600000)
                val end = intent.getLongExtra(EXTRA_END, start + 3600000)
                val allDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false)
                val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""

                val calIntent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                    putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(calIntent)
            }

            ACTION_REMIND_ME -> {
                val mailId = intent.getStringExtra(EXTRA_MAIL_ID)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                val course = intent.getStringExtra(EXTRA_COURSE)
                val dueAt = intent.getLongExtra(EXTRA_START, System.currentTimeMillis() + 86400000)

                scheduleQuickReminder(context, mailId, title, course, dueAt)
                Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
            }

            ACTION_SET_ALARM -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Academic Alarm"
                val start = intent.getLongExtra(EXTRA_START, System.currentTimeMillis() + 7200000)
                // 2 hours before event
                val alarmTimeMs = start - (2 * 3600000)
                val localTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(alarmTimeMs), ZoneId.systemDefault())

                val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, title)
                    putExtra(AlarmClock.EXTRA_HOUR, localTime.hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, localTime.minute)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(alarmIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Unable to open system alarm", Toast.LENGTH_SHORT).show()
                }
            }

            ACTION_SHARE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
                val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
                val shareText = "$title ${if (location.isNotBlank()) "at $location" else ""} (via Khabar)"

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(sendIntent, "Share with").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
            }

            ACTION_OPEN_LINK -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (!url.isNullOrBlank()) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {}
                }
            }

            ACTION_MARK_DONE -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                val mailId = intent.getStringExtra(EXTRA_MAIL_ID)
                val app = context.applicationContext as KhabarApp
                CoroutineScope(Dispatchers.IO).launch {
                    if (reminderId > 0) {
                        app.container.reminderRepository.markReminderDone(reminderId, true)
                    } else if (mailId != null) {
                        val reminder = app.container.reminderRepository.getReminderByMailId(mailId)
                        if (reminder != null) {
                            app.container.reminderRepository.markReminderDone(reminder.id, true)
                        }
                    }
                }
                Toast.makeText(context, "Marked as done", Toast.LENGTH_SHORT).show()
            }

            ACTION_OPEN_PORTAL -> {
                val app = context.applicationContext as KhabarApp
                CoroutineScope(Dispatchers.IO).launch {
                    val profile = app.container.profileRepository.getProfile()
                    val portalUrl = profile?.portalUrl?.ifBlank { "https://portal.iba.edu.pk" } ?: "https://portal.iba.edu.pk"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {}
                }
            }

            ACTION_EMAIL_OFFICE -> {
                val recipient = intent.getStringExtra(EXTRA_RECIPIENT) ?: "programoffice@iba.edu.pk"
                val subject = intent.getStringExtra(EXTRA_TITLE) ?: "Inquiry regarding attendance"
                val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$recipient?subject=${Uri.encode(subject)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(mailtoIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "No email client installed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scheduleQuickReminder(
        context: Context,
        mailId: String?,
        title: String,
        courseCode: String?,
        dueAt: Long
    ) {
        val app = context.applicationContext as KhabarApp
        val now = System.currentTimeMillis()

        // Default 24h before; if past then 1h before; if <1h away then in 1 minute
        val remindAt = when {
            dueAt - (24 * 3600000) > now -> dueAt - (24 * 3600000)
            dueAt - (3600000) > now -> dueAt - (3600000)
            else -> now + 60000
        }

        CoroutineScope(Dispatchers.IO).launch {
            val entity = ReminderEntity(
                mailId = mailId,
                title = title,
                courseCode = courseCode,
                dueAt = dueAt,
                remindAt = remindAt
            )
            val id = app.container.reminderRepository.insertReminder(entity)
            scheduleSystemAlarm(context, id, title, remindAt)
        }
    }

    private fun scheduleSystemAlarm(context: Context, reminderId: Long, title: String, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminderId)
            putExtra("reminder_title", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }
}
