package com.letdigitalfly.ibamail.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unknown_course_prompts")
data class UnknownCoursePromptEntity(
    @PrimaryKey val code: String,
    val mailId: String,
    val answered: Boolean = false
)
