package com.letdigitalfly.ibamail.domain.model

enum class SummaryLanguage(
    val code: String,
    val title: String,
    val subtitle: String,
    val sampleSentence: String
) {
    ENGLISH(
        code = "en",
        title = "English",
        subtitle = "Standard and concise",
        sampleSentence = "Tomorrow's class has been cancelled. You will be notified when rescheduled."
    ),
    ROMAN_URDU(
        code = "roman_urdu",
        title = "Roman Urdu",
        subtitle = "Aasan aur bolchal wali",
        sampleSentence = "Kal ki class cancel ho gayi hai. Nayi date aane par notify karenge."
    ),
    URDU(
        code = "urdu",
        title = "اردو",
        subtitle = "Baqaida Urdu",
        sampleSentence = "کل کی کلاس منسوخ کر دی گئی ہے۔ نئی تاریخ کا اعلان بعد میں ہوگا۔"
    );

    val displayName: String get() = title

    companion object {
        fun fromCode(code: String?): SummaryLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
