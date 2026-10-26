package com.letdigitalfly.ibamail.config

import com.letdigitalfly.ibamail.BuildConfig

/**
 * Global application configuration.
 * Contains the constant APP_NAME and provides access to build-time or overridden slots.
 */
object AppConfig {
    const val APP_NAME = "Khabar"
    const val APP_TAGLINE = "Only the mails that matter to you."

    // Slots from BuildConfig (defaults to empty or false when unset)
    val buildMsClientId: String = BuildConfig.MS_CLIENT_ID
    val buildMsSignatureHash: String = BuildConfig.MS_SIGNATURE_HASH
    val buildGmailEnabled: Boolean = BuildConfig.GMAIL_ENABLED
    val buildGmailClientId: String = BuildConfig.GMAIL_CLIENT_ID
    val buildAiBaseUrl: String = BuildConfig.AI_BASE_URL
    val buildAiModel: String = BuildConfig.AI_MODEL
    val buildAiApiKey: String = BuildConfig.AI_API_KEY

    val MS_CLIENT_ID: String get() = buildMsClientId
    val MS_SIGNATURE_HASH: String get() = buildMsSignatureHash
    val GMAIL_ENABLED: Boolean get() = buildGmailEnabled
    val GMAIL_CLIENT_ID: String get() = buildGmailClientId
    val AI_BASE_URL: String get() = buildAiBaseUrl
    val AI_MODEL: String get() = buildAiModel
    val AI_API_KEY: String get() = buildAiApiKey

    const val PORTAL_URL = "https://portal.iba.edu.pk"
    const val PREF_KEY_AI_KEY = "ai_api_key"

    const val DEFAULT_DIGEST_TIME = "07:30"
    const val SYNC_INTERVAL_MINUTES = 15L
    const val FAST_MODE_INTERVAL_SECONDS = 90L

    val DEFAULT_INTERESTS = listOf(
        "INTERNSHIPS" to "Internships and jobs",
        "EXCHANGE" to "Exchange and study abroad",
        "SCHOLARSHIPS" to "Scholarships and fee help",
        "HOSTEL" to "Hostel",
        "TRANSPORT" to "Transport",
        "SOCIETIES" to "Societies and events",
        "ALUMNI" to "Alumni",
        "SPORTS" to "Sports"
    )
}
