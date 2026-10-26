package com.khabar.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_channel_versions")
data class NotificationChannelVersionEntity(
    @PrimaryKey val categoryId: String,
    val version: Int = 1,
    val alertLevel: String = "NORMAL",
    val customSoundUri: String? = null
)
