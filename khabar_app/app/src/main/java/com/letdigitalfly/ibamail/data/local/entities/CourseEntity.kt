package com.letdigitalfly.ibamail.data.local.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val section: String = "",
    val name: String = "",
    val teacherName: String = "",
    val teacherEmail: String = "",
    val slotsJson: String = "[]" // JSON array of slots: [{"day":"Mon","start":"09:00","end":"10:15"}]
) {
    @get:Ignore
    val courseCode: String get() = code

    @get:Ignore
    val courseName: String get() = name
}
