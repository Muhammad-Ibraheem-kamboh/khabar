package com.khabar.app.domain.model

enum class MailCategory(
    val idString: String,
    val displayName: String,
    val colorHex: Long,
    val defaultLevel: AlertLevel,
    val soundDescription: String,
    val defaultToneHz: List<Int>,
    val defaultToneDurationMs: List<Int>
) {
    CLASS_CHANGE(
        idString = "CLASS_CHANGE",
        displayName = "Class changes",
        colorHex = 0xFFFF3B30,
        defaultLevel = AlertLevel.IMPORTANT,
        soundDescription = "Two descending tones",
        defaultToneHz = listOf(880, 660),
        defaultToneDurationMs = listOf(180, 180)
    ),
    EXAM(
        idString = "EXAM",
        displayName = "Exams and quizzes",
        colorHex = 0xFFAF52DE,
        defaultLevel = AlertLevel.IMPORTANT,
        soundDescription = "Three ascending tones",
        defaultToneHz = listOf(523, 659, 784),
        defaultToneDurationMs = listOf(140, 140, 140)
    ),
    DEADLINE(
        idString = "DEADLINE",
        displayName = "Deadlines",
        colorHex = 0xFFFF9500,
        defaultLevel = AlertLevel.IMPORTANT,
        soundDescription = "One soft decaying tone",
        defaultToneHz = listOf(587),
        defaultToneDurationMs = listOf(300)
    ),
    ATTENDANCE(
        idString = "ATTENDANCE",
        displayName = "Attendance warnings",
        colorHex = 0xFFA2845E,
        defaultLevel = AlertLevel.IMPORTANT,
        soundDescription = "One long low tone",
        defaultToneHz = listOf(330),
        defaultToneDurationMs = listOf(600)
    ),
    REGISTRATION(
        idString = "REGISTRATION",
        displayName = "Registration",
        colorHex = 0xFFFFCC00,
        defaultLevel = AlertLevel.IMPORTANT,
        soundDescription = "Three fast short beeps",
        defaultToneHz = listOf(988, 988, 988),
        defaultToneDurationMs = listOf(80, 80, 80)
    ),
    FEE(
        idString = "FEE",
        displayName = "Fees and scholarships",
        colorHex = 0xFF30B0C7,
        defaultLevel = AlertLevel.NORMAL,
        soundDescription = "Two mid tones",
        defaultToneHz = listOf(440, 494),
        defaultToneDurationMs = listOf(150, 150)
    ),
    RESULT(
        idString = "RESULT",
        displayName = "Results",
        colorHex = 0xFF34C759,
        defaultLevel = AlertLevel.NORMAL,
        soundDescription = "Happy 4-note rise",
        defaultToneHz = listOf(523, 659, 784, 1047),
        defaultToneDurationMs = listOf(100, 100, 100, 150)
    ),
    CAREER(
        idString = "CAREER",
        displayName = "Internships and jobs",
        colorHex = 0xFF5856D6,
        defaultLevel = AlertLevel.NORMAL,
        soundDescription = "Soft chime",
        defaultToneHz = listOf(784, 988),
        defaultToneDurationMs = listOf(160, 200)
    ),
    EVENT(
        idString = "EVENT",
        displayName = "Events and societies",
        colorHex = 0xFFFF2D55,
        defaultLevel = AlertLevel.DIGEST_ONLY,
        soundDescription = "Silent / Digest only",
        defaultToneHz = emptyList(),
        defaultToneDurationMs = emptyList()
    ),
    GENERAL(
        idString = "GENERAL",
        displayName = "General notices",
        colorHex = 0xFF8E8E93,
        defaultLevel = AlertLevel.DIGEST_ONLY,
        soundDescription = "Silent / Digest only",
        defaultToneHz = emptyList(),
        defaultToneDurationMs = emptyList()
    );

    companion object {
        fun fromString(id: String?): MailCategory = fromName(id)

        fun fromName(name: String?): MailCategory {
            if (name == null) return GENERAL
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                it.idString.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true)
            } ?: GENERAL
        }
    }
}
