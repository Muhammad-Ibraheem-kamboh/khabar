package com.letdigitalfly.ibamail.domain.model

enum class AlertLevel(val displayName: String, val description: String) {
    IMPORTANT("Important", "Sound, heads-up banner, and vibration"),
    NORMAL("Normal", "Sound and standard notification in shade"),
    QUIET("Quiet", "Silent notification without sound or vibration"),
    DIGEST_ONLY("Digest only", "Included only in the daily summary digest");

    companion object {
        fun fromString(str: String?): AlertLevel {
            return entries.firstOrNull { it.name.equals(str, ignoreCase = true) } ?: NORMAL
        }
    }
}
