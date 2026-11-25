package com.khabar.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import com.khabar.app.R
import com.khabar.app.config.AppConfig
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.domain.model.AlertLevel
import com.khabar.app.domain.model.MailBucket
import com.khabar.app.domain.model.MailCategory
import com.khabar.app.ui.MainActivity
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class KhabarNotificationManager(
    private val context: Context,
    private val channelsManager: NotificationChannelsManager
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val json = Json { ignoreUnknownKeys = true }

    // Buffer for quiet during class
    private val quietClassBuffer = mutableListOf<MailEntity>()

    suspend fun postMailNotification(
        mail: MailEntity,
        notifyGoodToKnow: Boolean = false,
        quietDuringClass: Boolean = false,
        isInsideClassSlot: Boolean = false
    ) {
        val bucket = MailBucket.fromString(mail.bucket)
        if (bucket == MailBucket.HIDDEN) return
        if (bucket == MailBucket.GOOD_TO_KNOW && !notifyGoodToKnow) return

        val category = MailCategory.fromString(mail.category)

        // Check Quiet During Class:
        // Hold notifications of all categories except CLASS_CHANGE and ATTENDANCE (and anything due within 24 hours)
        val now = System.currentTimeMillis()
        val dueSoon = mail.eventStart != null && (mail.eventStart - now in 0..86400000)
        val isExempt = category == MailCategory.CLASS_CHANGE || category == MailCategory.ATTENDANCE || dueSoon

        if (quietDuringClass && isInsideClassSlot && !isExempt) {
            quietClassBuffer.add(mail)
            return
        }

        val channelId = channelsManager.getChannelIdForCategory(category)

        // Deep link to mail detail
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("khabar://mail/${mail.id}"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            mail.id.hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse reasons and bullets
        val reasonsList = try {
            json.decodeFromString<List<String>>(mail.reasonsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val firstReason = reasonsList.firstOrNull() ?: "Relevant to your courses"

        val bulletsList = try {
            json.decodeFromString<List<String>>(mail.keyPointsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val bigText = bulletsList.joinToString("\n• ", prefix = if (bulletsList.isNotEmpty()) "• " else "")
            .ifEmpty { mail.bodyText.take(150) }

        // Smart Priority Notifications: Urgent matters (exams, deadlines, fees, class changes) get HIGH priority with heads-up; ordinary notices get DEFAULT
        val isUrgentCategory = category == MailCategory.CLASS_CHANGE ||
                category == MailCategory.EXAM ||
                category == MailCategory.DEADLINE ||
                category == MailCategory.FEE ||
                category == MailCategory.ATTENDANCE ||
                category == MailCategory.REGISTRATION ||
                dueSoon

        val notificationPriority = if (isUrgentCategory) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(category.colorHex.toInt())
            .setContentTitle(mail.headline)
            .setContentText(mail.bodyText.take(90))
            .setSubText("Why: $firstReason")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(notificationPriority)
            .setAutoCancel(true)
            .setGroup("khabar_${category.idString.lowercase()}")
            .setContentIntent(contentPendingIntent)

        // Actions (pick up to 3)
        val actions = buildActions(mail, category)
        for (action in actions.take(3)) {
            builder.addAction(action)
        }

        notificationManager.notify(mail.id.hashCode(), builder.build())
    }

    fun releaseQuietBuffer() {
        if (quietClassBuffer.isEmpty()) return
        val count = quietClassBuffer.size
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("Class Ended: $count held notifications")

        for (item in quietClassBuffer.take(5)) {
            inboxStyle.addLine("• ${item.headline}")
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            5555,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val heldNotification = NotificationCompat.Builder(context, NotificationChannelsManager.CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Class ended")
            .setContentText("$count updates held during class are now available")
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(5555, heldNotification)
        quietClassBuffer.clear()
    }

    private fun buildActions(mail: MailEntity, category: MailCategory): List<NotificationCompat.Action> {
        val actions = mutableListOf<NotificationCompat.Action>()

        val calendarAction = NotificationCompat.Action(
            0,
            "Add to calendar",
            createCalendarPendingIntent(mail)
        )
        val remindAction = NotificationCompat.Action(
            0,
            "Remind me",
            createActionPendingIntent(NotificationActionReceiver.ACTION_REMIND_ME, mail)
        )
        val alarmAction = NotificationCompat.Action(
            0,
            "Set alarm",
            createAlarmPendingIntent(mail)
        )
        val shareAction = NotificationCompat.Action(
            0,
            "Share",
            createSharePendingIntent(mail)
        )
        val openLinkAction = NotificationCompat.Action(
            0,
            "Open link",
            createOpenLinkPendingIntent(mail.link ?: AppConfig.PORTAL_URL, mail.id)
        )
        val markDoneAction = NotificationCompat.Action(
            0,
            "Mark done",
            createActionPendingIntent(NotificationActionReceiver.ACTION_MARK_DONE, mail)
        )
        val openPortalAction = NotificationCompat.Action(
            0,
            "Open portal",
            createOpenLinkPendingIntent(AppConfig.PORTAL_URL, mail.id)
        )
        val emailOfficeAction = NotificationCompat.Action(
            0,
            "Email office",
            createEmailOfficePendingIntent(mail.senderEmail ?: "programoffice@iba.edu.pk", mail.headline, mail.id)
        )
        val explainAction = NotificationCompat.Action(
            0,
            "Explain",
            createDeepLinkPendingIntent(mail.id)
        )

        when (category) {
            MailCategory.CLASS_CHANGE -> {
                if (mail.eventStart != null) actions.add(calendarAction)
                actions.add(shareAction)
                actions.add(explainAction)
            }
            MailCategory.EXAM -> {
                actions.add(calendarAction)
                actions.add(remindAction)
                actions.add(alarmAction)
            }
            MailCategory.DEADLINE -> {
                actions.add(markDoneAction)
                actions.add(remindAction)
                if (!mail.link.isNullOrBlank()) actions.add(openLinkAction) else actions.add(shareAction)
            }
            MailCategory.FEE -> {
                actions.add(openPortalAction)
                actions.add(remindAction)
            }
            MailCategory.REGISTRATION -> {
                actions.add(remindAction)
                actions.add(openPortalAction)
            }
            MailCategory.RESULT -> {
                actions.add(openPortalAction)
                actions.add(explainAction)
            }
            MailCategory.ATTENDANCE -> {
                actions.add(explainAction)
                actions.add(emailOfficeAction)
            }
            MailCategory.CAREER -> {
                if (!mail.link.isNullOrBlank()) actions.add(openLinkAction)
                actions.add(remindAction)
                actions.add(shareAction)
            }
            MailCategory.EVENT -> {
                actions.add(calendarAction)
                actions.add(shareAction)
            }
            MailCategory.GENERAL -> {
                actions.add(explainAction)
            }
        }

        return actions
    }

    private fun createActionPendingIntent(actionName: String, mail: MailEntity): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = actionName
            putExtra(NotificationActionReceiver.EXTRA_MAIL_ID, mail.id)
            putExtra(NotificationActionReceiver.EXTRA_TITLE, mail.headline)
            putExtra(NotificationActionReceiver.EXTRA_COURSE, mail.courseCode)
            putExtra(NotificationActionReceiver.EXTRA_START, mail.eventStart ?: (System.currentTimeMillis() + 86400000))
            putExtra(NotificationActionReceiver.EXTRA_END, mail.eventEnd ?: (System.currentTimeMillis() + 90000000))
            putExtra(NotificationActionReceiver.EXTRA_ALL_DAY, mail.allDay)
            putExtra(NotificationActionReceiver.EXTRA_LOCATION, mail.room ?: "")
            putExtra(NotificationActionReceiver.EXTRA_URL, mail.link ?: "")
            putExtra(NotificationActionReceiver.EXTRA_RECIPIENT, mail.senderEmail ?: "")
        }
        return PendingIntent.getBroadcast(
            context,
            (mail.id + actionName).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCalendarPendingIntent(mail: MailEntity): PendingIntent {
        val start = mail.eventStart ?: (System.currentTimeMillis() + 3600000)
        val end = mail.eventEnd ?: (start + 3600000)
        val calIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, mail.headline)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, mail.allDay)
            putExtra(CalendarContract.Events.EVENT_LOCATION, mail.room ?: "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            (mail.id + "_cal").hashCode(),
            calIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createAlarmPendingIntent(mail: MailEntity): PendingIntent {
        val start = mail.eventStart ?: (System.currentTimeMillis() + 7200000)
        val alarmTimeMs = start - (2 * 3600000)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = alarmTimeMs }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)

        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, mail.headline)
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            (mail.id + "_alarm").hashCode(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createSharePendingIntent(mail: MailEntity): PendingIntent {
        val locationText = if (!mail.room.isNullOrBlank()) " at ${mail.room}" else ""
        val shareText = "${mail.headline}$locationText\n\n${mail.bodyText.take(200)} (via Khabar)"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, "Share with").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            (mail.id + "_share").hashCode(),
            chooser,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createOpenLinkPendingIntent(url: String, mailId: String): PendingIntent {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            (mailId + "_link").hashCode(),
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createEmailOfficePendingIntent(recipient: String, subject: String, mailId: String): PendingIntent {
        val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipient?subject=${Uri.encode(subject)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            (mailId + "_mailto").hashCode(),
            mailtoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postDailyDigest(importantCount: Int, totalCount: Int) {
        val notification = NotificationCompat.Builder(context, NotificationChannelsManager.CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Morning Digest • $importantCount mails need attention")
            .setContentText("You received $totalCount mails today. Check class changes and deadlines.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(9999, notification)
    }

    private fun createDeepLinkPendingIntent(mailId: String): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("khabar://mail/$mailId"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            (mailId + "_deep").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
