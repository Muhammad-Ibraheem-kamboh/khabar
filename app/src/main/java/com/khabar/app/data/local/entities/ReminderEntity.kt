package com.khabar.app.data.local.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mailId: String? = null,
    val title: String,
    val courseCode: String? = null,
    val dueAt: Long,
    val remindAt: Long,
    val done: Boolean = false,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val isCompleted: Boolean get() = done

    @get:Ignore
    val triggerAt: Long get() = remindAt
}
