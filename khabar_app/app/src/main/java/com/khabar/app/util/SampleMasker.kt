package com.khabar.app.util

import com.khabar.app.data.local.entities.MailEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

object SampleMasker {

    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")
    private val phoneRegex = Regex("(\\+?\\d{1,3}[- ]?)?\\(?\\d{3,4}\\)?[- ]?\\d{3,4}[- ]?\\d{3,4}")
    private val erpIdRegex = Regex("\\b\\d{4,6}\\b")

    fun mask(text: String): String {
        var masked = text
        masked = emailRegex.replace(masked, "student@university.edu")
        masked = phoneRegex.replace(masked, "+92 300 0000000")
        masked = erpIdRegex.replace(masked, "*****")
        return masked
    }

    fun maskMailsToJson(mails: List<MailEntity>): String {
        val jsonArray = buildJsonArray {
            for (mail in mails) {
                add(
                    buildJsonObject {
                        put("subject", JsonPrimitive(mask(mail.subject)))
                        put("category", JsonPrimitive(mail.category))
                        put("bucket", JsonPrimitive(mail.bucket))
                        put("bodyPreview", JsonPrimitive(mask(mail.bodyPreview)))
                    }
                )
            }
        }
        return Json.encodeToString(JsonElement.serializer(), jsonArray)
    }
}
