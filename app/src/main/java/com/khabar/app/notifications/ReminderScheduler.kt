package com.khabar.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {

    fun schedule(context: Context, id: Long, title: String, courseCode: String?, remindAt: Long) {
        val now = System.currentTimeMillis()
        if (remindAt <= now) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReminderReceiver::class.java).apply {
            putExtra("reminder_id", id)
            putExtra("reminder_title", title)
            putExtra("course_code", courseCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, remindAt, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun cancel(context: Context, id: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {}
    }
}
