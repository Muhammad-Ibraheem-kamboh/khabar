package com.letdigitalfly.ibamail.data.local.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Entity(tableName = "mails")
data class MailEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sender: String,
    val senderEmail: String? = null,
    val subject: String,
    val bodyText: String,
    val receivedAt: Long,
    val isPreviewOnly: Boolean = false,
    val hasAttachments: Boolean = false,
    val attachmentNames: String = "[]", // JSON array of string filenames
    val link: String? = null,
    val category: String, // MailCategory idString
    val secondaryCategory: String? = null,
    val bucket: String, // MailBucket name (IMPORTANT, GOOD_TO_KNOW, HIDDEN)
    val reasonsJson: String = "[]", // JSON array of string reasons
    val headline: String,
    val keyPointsJson: String = "[]", // JSON array of string bullets
    val factsJson: String = "{}", // JSON map of key specs
    val eventStart: Long? = null,
    val eventEnd: Long? = null,
    val allDay: Boolean = false,
    val room: String? = null,
    val courseCode: String? = null,
    val read: Boolean = false,
    val summarySource: String = "OFFLINE", // OFFLINE or AI
    val aiSummaryJson: String? = null,
    val overrideBucket: String? = null,
    val originalIntentKey: String? = null
) {
    @get:Ignore
    val senderName: String get() = sender

    @get:Ignore
    val isRead: Boolean get() = read

    @get:Ignore
    val bodyPreview: String get() = bodyText.take(150)

    @get:Ignore
    val isNotificationPreview: Boolean get() = isPreviewOnly

    @get:Ignore
    val isAiGenerated: Boolean get() = summarySource.equals("AI", ignoreCase = true)

    @get:Ignore
    val extractedDateEpoch: Long? get() = eventStart

    @get:Ignore
    val extractedRoom: String? get() = room

    @get:Ignore
    val extractedLink: String? get() = link

    @get:Ignore
    val summaryText: String
        get() {
            return try {
                val array = Json.parseToJsonElement(keyPointsJson).jsonArray
                array.firstOrNull()?.jsonPrimitive?.content ?: bodyPreview
            } catch (_: Exception) {
                bodyPreview
            }
        }

    @get:Ignore
    val bullet1: String?
        get() = try {
            val array = Json.parseToJsonElement(keyPointsJson).jsonArray
            array.getOrNull(0)?.jsonPrimitive?.content
        } catch (_: Exception) { null }

    @get:Ignore
    val bullet2: String?
        get() = try {
            val array = Json.parseToJsonElement(keyPointsJson).jsonArray
            array.getOrNull(1)?.jsonPrimitive?.content
        } catch (_: Exception) { null }

    @get:Ignore
    val bullet3: String?
        get() = try {
            val array = Json.parseToJsonElement(keyPointsJson).jsonArray
            array.getOrNull(2)?.jsonPrimitive?.content
        } catch (_: Exception) { null }

    @get:Ignore
    val matchedReason: String
        get() = try {
            val array = Json.parseToJsonElement(reasonsJson).jsonArray
            array.joinToString(", ") { it.jsonPrimitive.content }
        } catch (_: Exception) { "" }
}
