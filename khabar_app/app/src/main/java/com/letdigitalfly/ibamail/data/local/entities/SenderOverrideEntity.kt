package com.letdigitalfly.ibamail.data.local.entities

import androidx.room.Entity

@Entity(tableName = "sender_overrides", primaryKeys = ["sender", "category"])
data class SenderOverrideEntity(
    val sender: String,
    val category: String,
    val bucket: String
)
