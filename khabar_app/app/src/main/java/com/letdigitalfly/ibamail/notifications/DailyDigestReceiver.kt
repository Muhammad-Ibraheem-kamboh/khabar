package com.letdigitalfly.ibamail.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.letdigitalfly.ibamail.KhabarApp
import com.letdigitalfly.ibamail.R
import com.letdigitalfly.ibamail.domain.model.MailBucket
import com.letdigitalfly.ibamail.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyDigestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as KhabarApp

        CoroutineScope(Dispatchers.IO).launch {
            val profile = app.container.profileRepository.getProfile()
            if (profile != null && !profile.digestEnabled) return@launch

            val topMails = app.container.mailRepository.getMailsByBucket(MailBucket.IMPORTANT).first()
            val nowMs = System.currentTimeMillis()
            val upcomingReminders = app.container.reminderRepository.allRemindersFlow.first()
                .filter { !it.done && it.dueAt >= nowMs }

            val importantCount = topMails.count { !it.read }
            val reminderCount = upcomingReminders.size

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle("Today: $importantCount important, $reminderCount reminders")

            for (mail in topMails.take(3)) {
                inboxStyle.addLine("• ${mail.headline}")
            }
            for (rem in upcomingReminders.take(2)) {
                inboxStyle.addLine("⏰ ${rem.title}")
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationChannelsManager.CHANNEL_DIGEST)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Your Morning Khabar Briefing")
                .setContentText("Today: $importantCount important mails, $reminderCount reminders")
                .setStyle(inboxStyle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(7777, notification)
        }
    }
}
