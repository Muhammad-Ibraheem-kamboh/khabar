package com.letdigitalfly.ibamail.domain.model

data class ExtractedEntities(
    val eventStart: Long? = null,
    val eventEnd: Long? = null,
    val allDay: Boolean = false,
    val dateText: String? = null,
    val room: String? = null,
    val courseCode: String? = null,
    val section: String? = null,
    val campus: String? = null,
    val link: String? = null,
    val mentionedPrograms: List<String> = emptyList(),
    val mentionedBatches: List<String> = emptyList(),
    val mentionedStandings: List<String> = emptyList(),
    val isBroadcast: Boolean = false
)
