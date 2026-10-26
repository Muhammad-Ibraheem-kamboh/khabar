package com.khabar.app.domain.model

data class RawMail(
    val id: String,
    val sourceId: String,
    val sender: String,
    val senderEmail: String? = null,
    val subject: String,
    val bodyText: String,
    val receivedAt: Long,
    val isPreviewOnly: Boolean = false,
    val hasAttachments: Boolean = false,
    val attachmentNames: List<String> = emptyList(),
    val link: String? = null,
    val originalIntentKey: String? = null
)
