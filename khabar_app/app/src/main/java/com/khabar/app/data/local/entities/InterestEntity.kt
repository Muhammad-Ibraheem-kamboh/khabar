package com.khabar.app.data.local.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val key: String,
    val enabled: Boolean = true
) {
    @get:Ignore
    val id: String get() = key

    @get:Ignore
    val isEnabled: Boolean get() = enabled

    @get:Ignore
    val name: String get() = key.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
