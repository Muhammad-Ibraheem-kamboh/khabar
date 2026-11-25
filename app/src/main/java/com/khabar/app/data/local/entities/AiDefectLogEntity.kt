package com.khabar.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_defect_logs")
data class AiDefectLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "TECHNICAL_BUG" or "ALIGNMENT_CORRECTION"
    val title: String,
    val userPrompt: String,
    val aiResponse: String? = null,
    val userCritique: String? = null,
    val rootCauseAnalysis: String,
    val suggestedFix: String,
    val provider: String? = null,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
