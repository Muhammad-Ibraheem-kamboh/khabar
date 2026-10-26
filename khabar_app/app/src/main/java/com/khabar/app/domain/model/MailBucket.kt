package com.khabar.app.domain.model

enum class MailBucket(val displayName: String) {
    IMPORTANT("Important"),
    GOOD_TO_KNOW("Good to know"),
    HIDDEN("Hidden");

    companion object {
        fun fromString(str: String?): MailBucket {
            return entries.firstOrNull { it.name.equals(str, ignoreCase = true) } ?: HIDDEN
        }
    }
}
