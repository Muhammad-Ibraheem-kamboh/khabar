package com.khabar.app.rules

/**
 * Pure Kotlin email normalizer.
 * Strips reply chains, forwarded headers, confidentiality disclaimers, and email signatures.
 */
object Normalizer {

    private val replyPatterns = listOf(
        Regex("(?i)-----Original Message-----.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)From:.*Sent:.*To:.*Subject:.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)On .*, .* wrote:.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)________________________________.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)Disclaimer:.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)This email and any files transmitted with it are confidential.*", RegexOption.DOT_MATCHES_ALL)
    )

    private val signaturePatterns = listOf(
        Regex("(?i)\n\nRegards,.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)\n\nBest regards,.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)\n\nSincerely,.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)\n\nThanks & Regards,.*", RegexOption.DOT_MATCHES_ALL),
        Regex("(?i)\n--\n.*", RegexOption.DOT_MATCHES_ALL)
    )

    fun normalize(text: String): String {
        var clean = text.trim()
        for (pattern in replyPatterns) {
            clean = pattern.replace(clean, "")
        }
        for (pattern in signaturePatterns) {
            clean = pattern.replace(clean, "")
        }
        return clean.trim()
    }

    fun cleanForAi(text: String): String {
        var clean = normalize(text)
        // Redact email addresses
        clean = clean.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"), "[EMAIL]")
        // Redact phone numbers and 10+ digit sequences
        clean = clean.replace(Regex("(\\+?\\d{1,3}[- ]?)?\\(?\\d{3,4}\\)?[- ]?\\d{3,4}[- ]?\\d{3,4}"), "[PHONE]")
        clean = clean.replace(Regex("\\b\\d{10,}\\b"), "[NUMBER]")
        return clean.take(3000).trim()
    }
}
