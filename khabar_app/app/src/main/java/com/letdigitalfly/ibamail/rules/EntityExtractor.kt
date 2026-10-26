package com.letdigitalfly.ibamail.rules

import com.letdigitalfly.ibamail.domain.model.ExtractedEntities
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object EntityExtractor {

    private val courseRegex = Regex("\\b([A-Z]{2,4})\\s?-?(\\d{3})\\b", RegexOption.IGNORE_CASE)
    private val roomRegex = Regex("\\b(MCC-\\d+|MTS-\\d+|MCS-\\d+|AP-\\d+|CS-\\d+|AM-\\d+|Tabba-\\d+|Room\\s*\\w+|Hall\\s*\\w+|Auditorium)\\b", RegexOption.IGNORE_CASE)
    private val linkRegex = Regex("https?://[\\w\\-\\.]+(?::\\d+)?(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?")
    private val batchRegex = Regex("\\b(Fall|Spring|Summer)\\s*(\\d{4})\\s*batch|batch\\s*(\\d{4})|(\\d{4})\\s*batch\\b", RegexOption.IGNORE_CASE)
    private val standingRegex = Regex("\\b(Freshman|Freshmen|Sophomore|Sophomores|Junior|Juniors|Senior|Seniors|(\\d+)(?:st|nd|rd|th)?\\s*semester)\\b", RegexOption.IGNORE_CASE)
    private val broadcastRegex = Regex("\\b(all students|dear students|everyone|all graduating|to all|dear all)\\b", RegexOption.IGNORE_CASE)

    fun extract(
        subject: String,
        body: String,
        receivedAtEpochMs: Long,
        programAliasesMap: Map<String, List<String>> = emptyMap()
    ): ExtractedEntities {
        val fullText = "$subject\n$body"
        val lowerText = fullText.lowercase()

        // 1. Course Code and Section
        var detectedCourse: String? = null
        var detectedSection: String? = null
        val courseMatch = courseRegex.find(fullText)
        if (courseMatch != null) {
            val prefix = courseMatch.groupValues[1].uppercase()
            val number = courseMatch.groupValues[2]
            detectedCourse = "$prefix$number"

            val allSecMatch = Regex("\\b(?:all sections|all sec)\\b", RegexOption.IGNORE_CASE).find(fullText)
            if (allSecMatch != null) {
                detectedSection = "ALL"
            } else {
                val secMatch = Regex("\\b(?:Section|Sec)\\s*([A-H])\\b", RegexOption.IGNORE_CASE).find(fullText)
                if (secMatch != null) {
                    detectedSection = secMatch.groupValues[1].uppercase()
                } else {
                    val parenMatch = Regex("\\b${Regex.escape(courseMatch.value)}\\s*\\(([A-H])\\)", RegexOption.IGNORE_CASE).find(fullText)
                    if (parenMatch != null) {
                        detectedSection = parenMatch.groupValues[1].uppercase()
                    }
                }
            }
        }

        // 2. Room
        val roomMatch = roomRegex.find(fullText)
        val detectedRoom = roomMatch?.value

        // 3. Campus
        val lowerSubject = subject.lowercase()
        val detectedCampus = when {
            lowerSubject.contains("main campus") && !lowerSubject.contains("city campus") -> "Main"
            lowerSubject.contains("city campus") && !lowerSubject.contains("main campus") -> "City"
            lowerText.contains("main campus") && lowerText.contains("city campus") -> "Both"
            lowerText.contains("main campus") -> "Main"
            lowerText.contains("city campus") -> "City"
            else -> null
        }

        // 4. Link
        val detectedLink = linkRegex.find(fullText)?.value

        // 5. Mentioned Programs
        val mentionedPrograms = mutableListOf<String>()
        for ((programId, aliases) in programAliasesMap) {
            for (alias in aliases) {
                if (Regex("\\b${Regex.escape(alias)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(fullText)) {
                    mentionedPrograms.add(programId)
                    break
                }
            }
        }

        // If no explicit program map given, check common programs
        if (mentionedPrograms.isEmpty()) {
            if (Regex("\\bBBA\\b", RegexOption.IGNORE_CASE).containsMatchIn(fullText)) mentionedPrograms.add("bba")
            if (Regex("\\b(BSCS|BS CS|Computer Science)\\b", RegexOption.IGNORE_CASE).containsMatchIn(fullText)) mentionedPrograms.add("bs_cs")
            if (Regex("\\b(BS Accounting|BS A&F|BSAF)\\b", RegexOption.IGNORE_CASE).containsMatchIn(fullText)) mentionedPrograms.add("bs_af")
            if (Regex("\\b(MBA Executive|EMBA)\\b", RegexOption.IGNORE_CASE).containsMatchIn(fullText)) mentionedPrograms.add("mba_exec")
            else if (Regex("\\bMBA\\b").containsMatchIn(fullText)) mentionedPrograms.add("mba")
        }

        // 6. Mentioned Batches
        val mentionedBatches = mutableListOf<String>()
        batchRegex.findAll(fullText).forEach { match ->
            mentionedBatches.add(match.value)
        }

        // 7. Mentioned Standings
        val mentionedStandings = mutableListOf<String>()
        standingRegex.findAll(fullText).forEach { match ->
            val value = match.value.lowercase()
            when {
                value.contains("freshm") || value.contains("1st semester") || value.contains("2nd semester") -> mentionedStandings.add("Freshman")
                value.contains("sophomore") || value.contains("3rd semester") || value.contains("4th semester") -> mentionedStandings.add("Sophomore")
                value.contains("junior") || value.contains("5th semester") || value.contains("6th semester") -> mentionedStandings.add("Junior")
                value.contains("senior") || value.contains("7th semester") || value.contains("8th semester") -> mentionedStandings.add("Senior")
            }
        }

        // 8. Broadcast check
        val isBroadcast = broadcastRegex.containsMatchIn(fullText) ||
                (mentionedPrograms.isEmpty() && mentionedBatches.isEmpty() && detectedCourse == null && mentionedStandings.isEmpty())

        // 9. Date and Time Extraction
        val (eventStart, eventEnd, isAllDay, dateText) = extractDateTime(lowerText, receivedAtEpochMs)

        return ExtractedEntities(
            eventStart = eventStart,
            eventEnd = eventEnd,
            allDay = isAllDay,
            dateText = dateText,
            room = detectedRoom,
            courseCode = detectedCourse,
            section = detectedSection,
            campus = detectedCampus,
            link = detectedLink,
            mentionedPrograms = mentionedPrograms.distinct(),
            mentionedBatches = mentionedBatches.distinct(),
            mentionedStandings = mentionedStandings.distinct(),
            isBroadcast = isBroadcast
        )
    }

    private fun extractDateTime(
        lowerText: String,
        receivedAtEpochMs: Long
    ): Quadruple<Long?, Long?, Boolean, String?> {
        val zoneId = ZoneId.systemDefault()
        val receivedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(receivedAtEpochMs), zoneId)
        val baseDate = receivedDateTime.toLocalDate()

        var resolvedDate: LocalDate? = null
        var isAllDay = true
        var resolvedTime: LocalTime? = null
        var dateLabel: String? = null

        // Relative days
        when {
            lowerText.contains("tomorrow") || lowerText.contains("kal") -> {
                resolvedDate = baseDate.plusDays(1)
                dateLabel = "Tomorrow"
            }
            lowerText.contains("today") || lowerText.contains("aaj") -> {
                resolvedDate = baseDate
                dateLabel = "Today"
            }
            lowerText.contains("in 2 days") || lowerText.contains("2 din baqi") -> {
                resolvedDate = baseDate.plusDays(2)
                dateLabel = "In 2 days"
            }
            lowerText.contains("in 3 days") || lowerText.contains("3 din baqi") -> {
                resolvedDate = baseDate.plusDays(3)
                dateLabel = "In 3 days"
            }
            lowerText.contains("in 6 days") || lowerText.contains("6 din baqi") -> {
                resolvedDate = baseDate.plusDays(6)
                dateLabel = "In 6 days"
            }
            lowerText.contains("in 10 days") || lowerText.contains("10 din baqi") -> {
                resolvedDate = baseDate.plusDays(10)
                dateLabel = "In 10 days"
            }
        }

        // Weekday names
        val weekdays = mapOf(
            "monday" to DayOfWeek.MONDAY,
            "peer" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "mangal" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "budh" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "jumerat" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "juma" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "hafta" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY,
            "itwar" to DayOfWeek.SUNDAY
        )

        for ((name, dow) in weekdays) {
            if (lowerText.contains(name) && resolvedDate == null) {
                var candidate = baseDate
                while (candidate.dayOfWeek != dow) {
                    candidate = candidate.plusDays(1)
                }
                resolvedDate = candidate
                dateLabel = name.replaceFirstChar { it.uppercase() }
                break
            }
        }

        // Explicit dates: e.g. "27 October", "27th Oct", "Oct 27"
        val explicitDateRegex = Regex("(\\d{1,2})(?:st|nd|rd|th)?\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*", RegexOption.IGNORE_CASE)
        val explicitMatch = explicitDateRegex.find(lowerText)
        if (explicitMatch != null && resolvedDate == null) {
            val day = explicitMatch.groupValues[1].toIntOrNull() ?: 1
            val monthStr = explicitMatch.groupValues[2].lowercase()
            val monthNum = when (monthStr) {
                "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6
                "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
                else -> baseDate.monthValue
            }
            resolvedDate = LocalDate.of(baseDate.year, monthNum, day.coerceIn(1, 28))
            dateLabel = "$day $monthStr"
        }

        // Time extraction: "11:00 AM", "11:00", "9:00 am", "11 baje", "11:59 pm"
        val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje)?", RegexOption.IGNORE_CASE)
        val timeMatches = timeRegex.findAll(lowerText)
        for (match in timeMatches) {
            val hour = match.groupValues[1].toIntOrNull() ?: continue
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase()

            if (hour in 1..24 && (amPm.isNotEmpty() || lowerText.contains("baje") || lowerText.contains("at $hour"))) {
                var h = hour
                if (amPm == "pm" && h < 12) h += 12
                if (amPm == "am" && h == 12) h = 0
                if (amPm == "baje" && h in 1..7) h += 12 // Contextual afternoon in Pakistan
                resolvedTime = LocalTime.of(h.coerceIn(0, 23), minute.coerceIn(0, 59))
                isAllDay = false
                break
            }
        }

        if (resolvedDate == null) {
            return Quadruple(null, null, true, null)
        }

        val startDateTime = if (resolvedTime != null) {
            LocalDateTime.of(resolvedDate, resolvedTime)
        } else {
            LocalDateTime.of(resolvedDate, LocalTime.of(9, 0))
        }

        val startMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = startDateTime.plusHours(1).atZone(zoneId).toInstant().toEpochMilli()

        val formattedDate = dateLabel ?: resolvedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))

        return Quadruple(startMillis, endMillis, isAllDay, formattedDate)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
