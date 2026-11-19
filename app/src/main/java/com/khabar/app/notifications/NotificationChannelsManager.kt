package com.khabar.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.khabar.app.data.local.dao.FeedbackDao
import com.khabar.app.data.local.entities.NotificationChannelVersionEntity
import com.khabar.app.domain.model.AlertLevel
import com.khabar.app.domain.model.MailCategory

class NotificationChannelsManager(
    private val context: Context,
    private val feedbackDao: FeedbackDao
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_DIGEST = "khabar_digest_v1"
        const val CHANNEL_REMINDERS = "khabar_reminders_v1"
        const val CHANNEL_TODAY_BOARD = "khabar_today_board_v1"
        const val CHANNEL_FAST_SERVICE = "khabar_fast_service_v1"
    }

    suspend fun initializeChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // 1. Fixed system channels
        val digestChannel = NotificationChannel(
            CHANNEL_DIGEST,
            "Daily Summary Digest",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Consolidated morning briefing of emails and reminders"
        }

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Deadlines & Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarms and cutoff countdown reminders"
            enableVibration(true)
        }

        val todayBoardChannel = NotificationChannel(
            CHANNEL_TODAY_BOARD,
            "Today Board",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent status overview of upcoming academic events"
            setShowBadge(false)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_FAST_SERVICE,
            "Background Sync",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Fast sync status indicator"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(
            listOf(digestChannel, remindersChannel, todayBoardChannel, serviceChannel)
        )

        // 2. Category channels
        for (category in MailCategory.entries) {
            val existing = feedbackDao.getChannelVersion(category.idString)
            if (existing == null) {
                val initialEntity = NotificationChannelVersionEntity(
                    categoryId = category.idString,
                    version = 1,
                    alertLevel = category.defaultLevel.name
                )
                feedbackDao.saveChannelVersion(initialEntity)
                createOrUpdateCategoryChannel(category, 1, category.defaultLevel, null)
            } else {
                val level = AlertLevel.fromString(existing.alertLevel)
                createOrUpdateCategoryChannel(category, existing.version, level, existing.customSoundUri)
            }
        }
    }

    suspend fun getChannelIdForCategory(category: MailCategory): String {
        val existing = feedbackDao.getChannelVersion(category.idString)
        val version = existing?.version ?: 1
        return "${category.idString.lowercase()}_v$version"
    }

    suspend fun updateCategoryChannel(
        category: MailCategory,
        newLevel: AlertLevel,
        newCustomSoundUri: String? = null
    ): String {
        val current = feedbackDao.getChannelVersion(category.idString)
        val currentVersion = current?.version ?: 1
        val newVersion = currentVersion + 1

        // Delete old channel
        val oldChannelId = "${category.idString.lowercase()}_v$currentVersion"
        try {
            notificationManager.deleteNotificationChannel(oldChannelId)
        } catch (_: Exception) {}

        // Save new version in database
        val newEntity = NotificationChannelVersionEntity(
            categoryId = category.idString,
            version = newVersion,
            alertLevel = newLevel.name,
            customSoundUri = newCustomSoundUri
        )
        feedbackDao.saveChannelVersion(newEntity)

        // Create new channel
        createOrUpdateCategoryChannel(category, newVersion, newLevel, newCustomSoundUri)

        return "${category.idString.lowercase()}_v$newVersion"
    }

    private fun createOrUpdateCategoryChannel(
        category: MailCategory,
        version: Int,
        level: AlertLevel,
        customSoundUriStr: String?
    ) {
        val channelId = "${category.idString.lowercase()}_v$version"

        val importance = when (level) {
            AlertLevel.IMPORTANT -> NotificationManager.IMPORTANCE_HIGH
            AlertLevel.NORMAL -> NotificationManager.IMPORTANCE_DEFAULT
            AlertLevel.QUIET, AlertLevel.DIGEST_ONLY -> NotificationManager.IMPORTANCE_LOW
        }

        val channel = NotificationChannel(channelId, category.displayName, importance).apply {
            description = "Alerts for ${category.displayName}"

            if (level == AlertLevel.IMPORTANT) {
                enableVibration(true)
                if (category == MailCategory.ATTENDANCE) {
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
            } else if (level == AlertLevel.QUIET || level == AlertLevel.DIGEST_ONLY) {
                enableVibration(false)
                setSound(null, null)
            }

            if (level != AlertLevel.QUIET && level != AlertLevel.DIGEST_ONLY) {
                val soundUri = if (!customSoundUriStr.isNullOrBlank()) {
                    Uri.parse(customSoundUriStr)
                } else {
                    ToneSynth.getOrCreateToneUri(context, category)
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                setSound(soundUri, audioAttributes)
            }
        }

        notificationManager.createNotificationChannel(channel)
    }
}
