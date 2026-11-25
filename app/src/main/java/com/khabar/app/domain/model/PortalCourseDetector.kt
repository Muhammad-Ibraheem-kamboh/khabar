package com.khabar.app.domain.model

import com.khabar.app.data.local.entities.CourseEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PortalCourseDetector {

    private val COURSE_CODE_REGEX = Regex("""\b([A-Z]{2,4})\s?[-_]?\s?(\d{3}[A-Z]?)\b""", RegexOption.IGNORE_CASE)

    // Common non-course false positives found in university portals (rooms, terms, protocols)
    private val BLACKLIST = setOf(
        "HTTP", "HTTPS", "PAGE", "ROOM", "STEP", "YEAR", "POST", "PORT", "CODE",
        "DATE", "TIME", "USER", "TERM", "INFO", "MAIN", "CITY", "FALL", "SPRG",
        "SUMR", "WINT", "TEXT", "BODY", "HTML", "VIEW", "LINK", "SIZE", "HASH",
        "EDIT", "CALL", "TEST", "TRUE", "FASE", "NULL", "WARN", "FAIL", "PASS",
        "ITEM", "LIST", "NAME", "TYPE", "MENU", "DEPT", "WEEK", "SLOT", "SECS",
        "CED", "FCS", "MCS", "TAB", "TABBA", "ADAM", "AUD", "HALL", "LAB",
        "BLDG", "FLR", "CLASS", "CRS", "CRSE", "LEC", "SEC"
    )

    private val INVALID_SECTION_WORDS = setOf(
        "COURSE", "COURSES", "SPRING", "FALL", "SUMMER", "WINTER", "TERM", "CREDIT",
        "CREDITS", "HOURS", "GRADE", "STATUS", "ENROLLED", "REGISTERED", "DROP",
        "ADD", "TOTAL", "PAGE", "ROOM", "DEPT", "AND", "THE", "FOR", "WITH",
        "MIDTERM", "FINAL", "EXAM", "LAB", "THEORY", "LECTURE", "CLASS", "SECTION",
        "SECTIONS", "NONE", "NULL", "TRUE", "FALSE"
    )

    private val DAY_PATTERN = Regex(
        """(?i)\b(Mon(?:day)?|Tue(?:sday)?|Wed(?:nesday)?|Thu(?:rsday)?|Fri(?:day)?|Sat(?:urday)?|Sun(?:day)?|TuTh|Tu/Th|MW|M/W)(?:\s*(?:[/,&]|and)\s*(Mon(?:day)?|Tue(?:sday)?|Wed(?:nesday)?|Thu(?:rsday)?|Fri(?:day)?|Sat(?:urday)?|Sun(?:day)?))?\b"""
    )

    private val TIME_PATTERN = Regex(
        """\b(\d{1,2}:\d{2}(?:\s*[AaPp][Mm])?)\s*(?:-|–|—|to)\s*(\d{1,2}:\d{2}(?:\s*[AaPp][Mm])?)\b"""
    )

    private val ROOM_PATTERN = Regex(
        """(?i)\b(?:Room|Venue|Hall|Lab|CED|Tabba|Adamjee|FCS|MCS|Classroom)\s*[:#\-–—]?\s*([A-Za-z0-9\s\-]+?)(?=(?:\n|\r|\||\bInstructor\b|\bTeacher\b|\bSlots\b|$))"""
    )

    private val TEACHER_PATTERN = Regex(
        """(?i)(?:Instructor|Teacher|Faculty|Prof(?:essor)?|Dr\.)\s*[:\-–—]?\s*([A-Za-z.\s]{3,35})(?=(?:\n|\r|\||\bSlots\b|\bRoom\b|$))"""
    )

    fun detectCourses(text: String, defaultSection: String = "A"): List<CourseEntity> {
        if (text.isBlank()) return emptyList()

        val results = mutableMapOf<String, CourseEntity>()
        val matches = COURSE_CODE_REGEX.findAll(text)
            .filter { !BLACKLIST.contains(it.groupValues[1].uppercase().trim()) }
            .toList()

        for (i in matches.indices) {
            val match = matches[i]
            val prefix = match.groupValues[1].uppercase().trim()
            val number = match.groupValues[2].uppercase().trim()

            val normalizedCode = "$prefix$number"

            // Compute context block for this specific course occurrence
            val blockStart = match.range.first
            val blockEnd = if (i + 1 < matches.size) {
                matches[i + 1].range.first.coerceAtMost(blockStart + 400)
            } else {
                (blockStart + 400).coerceAtMost(text.length)
            }
            val courseBlock = text.substring(blockStart, blockEnd)

            // 1. Detect Real Section
            val detectedSection = extractSection(courseBlock, prefix, number) ?: defaultSection

            // 2. Detect Course Title
            val detectedTitle = extractTitle(courseBlock, prefix, number) ?: inferCourseName(prefix, normalizedCode)

            // 3. Detect Timetable Slots (slotsJson)
            val detectedSlotsJson = extractSlotsJson(courseBlock)

            // 4. Detect Teacher Name
            val detectedTeacher = extractTeacher(courseBlock)

            // If we haven't seen this course yet, or if this detection provides more specific section/slots:
            val existing = results[normalizedCode]
            if (existing == null) {
                results[normalizedCode] = CourseEntity(
                    code = normalizedCode,
                    section = detectedSection,
                    name = detectedTitle,
                    teacherName = detectedTeacher,
                    teacherEmail = "",
                    slotsJson = detectedSlotsJson
                )
            } else {
                // Merge if existing was default and new has real section / slots
                val betterSection = if (existing.section == defaultSection && detectedSection != defaultSection) detectedSection else existing.section
                val betterSlots = if ((existing.slotsJson == "[]" || existing.slotsJson.isBlank()) && detectedSlotsJson != "[]") detectedSlotsJson else existing.slotsJson
                val betterName = if (existing.name.startsWith("University Course") && !detectedTitle.startsWith("University Course")) detectedTitle else existing.name
                val betterTeacher = if (existing.teacherName.isBlank() && detectedTeacher.isNotBlank()) detectedTeacher else existing.teacherName

                results[normalizedCode] = existing.copy(
                    section = betterSection,
                    name = betterName,
                    teacherName = betterTeacher,
                    slotsJson = betterSlots
                )
            }
        }

        return results.values.toList()
    }

    private fun extractSection(block: String, prefix: String, number: String): String? {
        // Pattern A: Suffix immediately after course code (e.g. MTS201-SS1, MTS201 - SS-1, MTS201/B)
        val suffixRegex = Regex("""(?i)\b${prefix}\s?[-_]?\s?${number}\s*[-/]\s*([A-Za-z0-9\-]{1,6})\b""")
        val suffixMatch = suffixRegex.find(block)
        if (suffixMatch != null) {
            val candidate = suffixMatch.groupValues[1].uppercase().trim()
            if (isValidSection(candidate)) return candidate
        }

        // Pattern B1: Explicit Section keyword: Section SS-1, Sec B, Sec: SS-1
        val sectionRegex = Regex("""(?i)(?:section|sec)\s*[:.\-–—\s]*([A-Za-z0-9\-]{1,8})""")
        val sectionMatch = sectionRegex.find(block)
        if (sectionMatch != null) {
            val candidate = sectionMatch.groupValues[1].uppercase().trim()
            if (isValidSection(candidate)) return candidate
        }

        // Pattern C: Parenthetical / Bracketed section: (Section SS-1), (Sec B), (SS-1), [Section B], [L1]
        val bracketRegex = Regex("""[\(\[]\s*(?:(?:sec(?:tion)?|class)\s*[:.\-–—\s]*)?([A-Za-z0-9\-]{1,6})\s*[\)\]]""", RegexOption.IGNORE_CASE)
        val bracketMatch = bracketRegex.find(block)
        if (bracketMatch != null) {
            val candidate = bracketMatch.groupValues[1].uppercase().trim()
            if (isValidSection(candidate)) return candidate
        }

        // Pattern D: Tab-delimited table row (PeopleSoft / UMS table format)
        val firstLine = block.lines().firstOrNull() ?: ""
        if (firstLine.contains("\t")) {
            val cols = firstLine.split("\t").map { it.trim() }
            val codeIndex = cols.indexOfFirst { it.contains(prefix, ignoreCase = true) && it.contains(number) }
            if (codeIndex != -1) {
                for (colIdx in (codeIndex + 1) until cols.size) {
                    val candidate = cols[colIdx].uppercase().trim()
                    if (candidate.length <= 8 && isValidSection(candidate)) {
                        return candidate
                    }
                }
            }
        }

        // Pattern B2: Class number fallback: Class: 4103, Class # 4103
        val classRegex = Regex("""(?i)class(?:\s*#)?\s*[:.\-–—\s]*([A-Za-z0-9\-]{1,8})""")
        val classMatch = classRegex.find(block)
        if (classMatch != null) {
            val candidate = classMatch.groupValues[1].uppercase().trim()
            if (isValidSection(candidate)) return candidate
        }

        return null
    }

    private fun isValidSection(candidate: String): Boolean {
        if (candidate.isBlank() || candidate.length > 8) return false
        if (INVALID_SECTION_WORDS.contains(candidate)) return false
        // Valid section is alphanumeric with optional hyphen (e.g. A, B, 1, 2, SS-1, SS1, CS-2, L1, 4103)
        return candidate.matches(Regex("""^[A-Za-z0-9]{1,4}(?:-[A-Za-z0-9]{1,4})?$"""))
    }

    private fun extractTitle(block: String, prefix: String, number: String): String? {
        // Tab-delimited row check first (e.g. MTS201 \t Linear Algebra \t ...)
        val firstLine = block.lines().firstOrNull() ?: ""
        if (firstLine.contains("\t")) {
            val cols = firstLine.split("\t").map { it.trim() }
            val codeIndex = cols.indexOfFirst { it.contains(prefix, ignoreCase = true) && it.contains(number) }
            if (codeIndex != -1 && codeIndex + 1 < cols.size) {
                val candidate = cols[codeIndex + 1]
                if (candidate.length >= 3 && candidate.any { it.isLetter() } && !isValidSection(candidate.uppercase())) {
                    return candidate
                }
            }
        }

        // Look right after course code for dash, colon, or space: MTS201 - Linear Algebra (Section A) or ECO101 Macroeconomics
        val titleRegex = Regex("""(?i)\b${prefix}\s?[-_]?\s?${number}\s*(?:[-–—:]\s*)?([A-Za-z0-9&/,\s]{3,60})""")
        val match = titleRegex.find(block)
        if (match != null) {
            var raw = match.groupValues[1].trim()
            // Clean out section or bracketed info
            raw = raw.substringBefore("(").substringBefore("[").substringBefore("|").substringBefore("\n").trim()
            // Remove trailing section/class words and separators
            raw = raw.replace(Regex("""(?i)\s*[-–—:]?\s*(?:sec(?:tion)?|class).*$"""), "").trim()
            raw = raw.trimEnd('-', '–', '—', ':', ' ')
            if (raw.length >= 3 && raw.any { it.isLetter() }) {
                return raw
            }
        }
        return null
    }

    private fun extractSlotsJson(block: String): String {
        val dayMatch = DAY_PATTERN.find(block)
        val timeMatch = TIME_PATTERN.find(block)

        if (dayMatch == null && timeMatch == null) return "[]"

        val day = dayMatch?.value?.trim() ?: ""
        val time = timeMatch?.value?.trim() ?: ""

        // Extract Room / Venue
        val roomMatch = ROOM_PATTERN.find(block)
        val room = roomMatch?.groupValues?.getOrNull(1)?.trim() ?: ""

        val slotMap = mutableMapOf<String, JsonPrimitive>()
        if (day.isNotBlank()) slotMap["day"] = JsonPrimitive(normalizeDays(day))
        if (time.isNotBlank()) slotMap["time"] = JsonPrimitive(time)
        if (room.isNotBlank()) slotMap["room"] = JsonPrimitive(room)

        if (slotMap.isNotEmpty()) {
            return JsonArray(listOf(JsonObject(slotMap))).toString()
        }

        return "[]"
    }

    private fun normalizeDays(raw: String): String {
        val clean = raw.trim()
        return when (clean.uppercase()) {
            "TUTH", "TU/TH" -> "Tue/Thu"
            "MW", "M/W" -> "Mon/Wed"
            else -> clean
        }
    }

    private fun extractTeacher(block: String): String {
        val match = TEACHER_PATTERN.find(block) ?: return ""
        return match.groupValues.getOrNull(1)?.trim() ?: ""
    }

    fun inferCourseName(prefix: String, normalizedCode: String): String {
        return when (prefix.uppercase()) {
            "MTS" -> "Mathematics / Statistics Course ($normalizedCode)"
            "CSE" -> "Computer Science Course ($normalizedCode)"
            "ECO" -> "Economics Course ($normalizedCode)"
            "ACC" -> "Accounting Course ($normalizedCode)"
            "FIN" -> "Finance Course ($normalizedCode)"
            "MKT" -> "Marketing Course ($normalizedCode)"
            "MGT" -> "Management Course ($normalizedCode)"
            "SSC" -> "Social Sciences Course ($normalizedCode)"
            "LAW" -> "Law Course ($normalizedCode)"
            "PHY" -> "Physics Course ($normalizedCode)"
            "HUM" -> "Humanities Course ($normalizedCode)"
            else -> "University Course ($normalizedCode)"
        }
    }

    /**
     * Parses a slotsJson array string and produces a human-readable schedule representation.
     * E.g. "Tue/Thu 11:30-12:45" or "Tue/Thu 11:30-12:45 (Aman CED 204)".
     */
    fun formatCourseSchedule(slotsJson: String?): String {
        if (slotsJson.isNullOrBlank() || slotsJson == "[]") return ""
        return try {
            val element = Json.parseToJsonElement(slotsJson)
            val array = element.jsonArray
            if (array.isEmpty()) return ""

            val parts = mutableListOf<String>()
            for (item in array) {
                val obj = item.jsonObject
                val day = obj["day"]?.jsonPrimitive?.content ?: ""
                val time = obj["time"]?.jsonPrimitive?.content ?: ""
                val start = obj["start"]?.jsonPrimitive?.content ?: ""
                val end = obj["end"]?.jsonPrimitive?.content ?: ""
                val room = obj["room"]?.jsonPrimitive?.content ?: ""

                val timeStr = if (time.isNotBlank()) {
                    time
                } else if (start.isNotBlank() && end.isNotBlank()) {
                    "$start-$end"
                } else start

                val dt = listOf(day, timeStr).filter { it.isNotBlank() }.joinToString(" ")
                val full = if (room.isNotBlank()) {
                    if (dt.isNotBlank()) "$dt ($room)" else room
                } else dt

                if (full.isNotBlank()) parts.add(full)
            }
            parts.joinToString("; ")
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Normalizes a course code string to strict canonical alphanumeric representation:
     * e.g. "CSE 141", "cse-141", "CSE_141" -> "CSE141"
     */
    fun normalizeCourseCode(raw: String): String {
        return raw.uppercase().replace(Regex("""[^A-Z0-9]"""), "").trim()
    }

    data class DetectedStudentProfile(
        val fullName: String? = null,
        val erpId: String? = null,
        val programName: String? = null
    )

    /**
     * Extracts student identity (Name, ERP / Student ID, Academic Program) from Sakai LMS or UMS page content.
     */
    fun detectStudentProfile(text: String): DetectedStudentProfile? {
        if (text.isBlank()) return null

        var detectedName: String? = null
        var detectedErp: String? = null
        var detectedProgram: String? = null

        // 1. Sakai LMS Profile pattern: "36930 Muhammad Ibraheem - 36930"
        val sakaiProfileRegex = Regex("""\b(\d{4,6})\s+([A-Za-z\s.]{3,35})\s*-\s*\1\b""")
        val sakaiMatch = sakaiProfileRegex.find(text)
        if (sakaiMatch != null) {
            detectedErp = sakaiMatch.groupValues[1].trim()
            detectedName = cleanStudentName(sakaiMatch.groupValues[2].trim())
        }

        // 2. Generic Student Name / Full Name / Name patterns (e.g. from UMS portal header)
        if (detectedName == null) {
            val nameRegex = Regex("""(?i)(?:Student Name|Full Name|Name)\s*[:.\-–—]\s*([A-Za-z\s.]{3,35})""")
            val nameMatch = nameRegex.find(text)
            if (nameMatch != null) {
                detectedName = cleanStudentName(nameMatch.groupValues[1].trim())
            }
        }

        // 3. ERP / Student ID
        if (detectedErp == null) {
            val erpRegex = Regex("""(?i)(?:ERP|Student ID|Roll No|ERP ID)\s*[:.\-–—]?\s*(\d{4,6})\b""")
            val erpMatch = erpRegex.find(text)
            if (erpMatch != null) {
                detectedErp = erpMatch.groupValues[1].trim()
            }
        }

        // 4. Academic Program (BS Mathematics, BSCS, BBA, MS, etc.)
        val programRegex = Regex("""(?i)\b(BS\s*(?:Computer Science|Mathematics|Maths|Economics|Accounting\s*(?:&|and)\s*Finance|Social Sciences|Artificial Intelligence|Data Science)|BBA|MS\s*(?:CS|DS|Maths)|EMBA|MBA)\b""")
        val progMatch = programRegex.find(text)
        if (progMatch != null) {
            detectedProgram = normalizeProgramName(progMatch.groupValues[1].trim())
        }

        if (detectedName == null && detectedErp == null && detectedProgram == null) {
            return null
        }

        return DetectedStudentProfile(
            fullName = detectedName,
            erpId = detectedErp,
            programName = detectedProgram
        )
    }

    private fun cleanStudentName(raw: String): String? {
        val cleaned = raw.replace(Regex("""\s+"""), " ").trim()
        val lower = cleaned.lowercase()
        if (lower.contains("login") || lower.contains("logout") || lower.contains("portal") ||
            lower.contains("sakai") || lower.contains("overview") || lower.contains("profile") ||
            lower.contains("system") || lower.contains("institute") || lower.contains("welcome")
        ) {
            return null
        }
        if (cleaned.length < 3) return null
        return cleaned.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun normalizeProgramName(raw: String): String {
        val upper = raw.uppercase().replace(Regex("""\s+"""), " ").trim()
        return when {
            upper.contains("MATH") -> "BS Mathematics"
            upper.contains("COMPUTER") -> "BS Computer Science"
            upper.contains("ECON") -> "BS Economics"
            upper.contains("ACCOUNT") || upper.contains("FINANCE") -> "BS Accounting & Finance"
            upper.contains("SOCIAL") -> "BS Social Sciences"
            upper.contains("ARTIFICIAL") || upper == "BS AI" -> "BS Artificial Intelligence"
            upper.contains("DATA SCIENCE") || upper == "BS DS" -> "BS Data Science"
            upper.contains("BBA") -> "BBA"
            upper.contains("MBA") -> "MBA"
            else -> raw.trim()
        }
    }

    /**
     * Formats a course into Khabri's elite prompt bullet format:
     * e.g. "• MTS201 (Linear Algebra) - Section: SS-1 | Slots: Tue/Thu 11:30-12:45"
     */
    fun formatCoursePromptLine(course: CourseEntity): String {
        val namePart = if (course.name.isNotBlank()) " (${course.name})" else ""
        val sectionPart = if (course.section.isNotBlank()) " - Section: ${course.section}" else ""
        val scheduleStr = formatCourseSchedule(course.slotsJson)
        val schedulePart = if (scheduleStr.isNotBlank()) " | Slots: $scheduleStr" else ""
        return "• ${course.code}$namePart$sectionPart$schedulePart"
    }
}
