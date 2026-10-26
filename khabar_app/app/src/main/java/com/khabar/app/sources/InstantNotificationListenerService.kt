package com.khabar.app.sources

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.khabar.app.KhabarApp
import com.khabar.app.domain.model.MailBucket
import com.khabar.app.domain.model.RawMail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections

class InstantNotificationListenerService : NotificationListenerService() {

    companion object {
        val ALLOWED_PACKAGES = mutableSetOf(
            "com.microsoft.office.outlook",
            "com.google.android.gm"
        )

        // In-memory map of contentIntents for "Open in app"
        val pendingIntentMap = Collections.synchronizedMap(mutableMapOf<String, PendingIntent>())

        // In-memory log of last 20 captured notification extras for Developer Tools
        val recentCapturedNotifications = Collections.synchronizedList(mutableListOf<CapturedNotificationLog>())
        val capturedNotifications = kotlinx.coroutines.flow.MutableStateFlow<List<Map<String, String>>>(emptyList())

        fun requestServiceRebind(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestRebind(ComponentName(context, InstantNotificationListenerService::class.java))
            }
        }
    }

    data class CapturedNotificationLog(
        val timestamp: Long,
        val packageName: String,
        val title: String?,
        val text: String?,
        val subText: String?,
        val bigText: String?,
        val rawExtrasSummary: String
    )

    private val seenHashes = Collections.synchronizedSet(mutableSetOf<Int>())

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, InstantNotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return

        // 1. Check package allow-list
        if (!ALLOWED_PACKAGES.contains(pkg)) return

        // 2. Ignore own package
        if (pkg == packageName) return

        // 3. Ignore ongoing notifications or group summaries
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = notification.extras ?: return

        // 4. Ignore Android 15/16 sensitive redaction
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (text.contains("sensitive notification content hidden", ignoreCase = true) ||
            title.contains("sensitive notification content hidden", ignoreCase = true)
        ) {
            return
        }

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") ?: ""

        val preview = when {
            bigText.isNotBlank() -> bigText
            lines.isNotBlank() -> lines
            else -> text
        }

        // Capture extras for Developer Tools
        val logEntry = CapturedNotificationLog(
            timestamp = System.currentTimeMillis(),
            packageName = pkg,
            title = title,
            text = text,
            subText = subText,
            bigText = bigText,
            rawExtrasSummary = "keys: ${extras.keySet().joinToString()}, category: ${notification.category}"
        )
        synchronized(recentCapturedNotifications) {
            recentCapturedNotifications.add(0, logEntry)
            if (recentCapturedNotifications.size > 20) {
                recentCapturedNotifications.removeAt(recentCapturedNotifications.size - 1)
            }
        }

        // Deduplication hash: package + sender + subject + minute
        val minuteBucket = System.currentTimeMillis() / 60000
        val dedupeHash = "$pkg|$title|$text|$minuteBucket".hashCode()
        if (seenHashes.contains(dedupeHash)) return
        seenHashes.add(dedupeHash)

        // Store original contentIntent for "Open in app"
        val intentKey = "intent_${System.currentTimeMillis()}_$dedupeHash"
        notification.contentIntent?.let {
            pendingIntentMap[intentKey] = it
        }

        val app = applicationContext as KhabarApp
        val rawMail = RawMail(
            id = "instant_${sbn.id}_${sbn.postTime}",
            sourceId = "instant_source",
            sender = title.ifBlank { "University Mail" },
            senderEmail = subText.ifBlank { null },
            subject = text.ifBlank { title },
            bodyText = preview.ifBlank { text },
            receivedAt = sbn.postTime,
            isPreviewOnly = true,
            hasAttachments = false,
            link = null,
            originalIntentKey = intentKey
        )

        // Send to InstantMailSource & repository
        InstantMailSource.emitMail(rawMail)

        CoroutineScope(Dispatchers.IO).launch {
            val processed = app.container.mailRepository.processAndSaveIncoming(rawMail)

            // Optional setting: Dismiss original notification if mail is Hidden
            val dismissHidden = app.container.userPreferences.dismissHiddenNotificationsFlow.first()
            if (dismissHidden && processed.bucket == MailBucket.HIDDEN.name) {
                cancelNotification(sbn.key)
            }
        }
    }
}
