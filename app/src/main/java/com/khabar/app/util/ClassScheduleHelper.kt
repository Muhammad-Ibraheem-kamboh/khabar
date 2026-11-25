package com.khabar.app.util

import com.khabar.app.data.local.entities.CourseEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

data class ScheduledClassItem(
    val courseId: Long,
    val courseCode: String,
    val courseName: String,
    val room: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val timeFormatted: String,
    val absences: Int,
    val maxAbsences: Int
)

data class NextClassStatus(
    val hasClassesToday: Boolean = false,
    val isOngoing: Boolean = false,
    val activeClass: ScheduledClassItem? = null,
    val statusText: String = "",
    val countdownText: String = "",
    val allDoneToday: Boolean = false
)

object ClassScheduleHelper {

    fun getTodayClasses(courses: List<CourseEntity>, nowEpochMs: Long = System.currentTimeMillis()): List<ScheduledClassItem> {
        val today = LocalDate.now(ZoneId.systemDefault()).dayOfWeek
        val items = mutableListOf<ScheduledClassItem>()

        for (course in courses) {
            val slots = parseSlots(course.slotsJson)
            for (slot in slots) {
                if (matchesDay(slot.day, today)) {
                    items.add(
                        ScheduledClassItem(
                            courseId = course.id,
                            courseCode = course.code,
                            courseName = course.name.ifBlank { course.code },
                            room = slot.room.ifBlank { "Campus Hall" },
                            startHour = slot.startHour,
                            startMinute = slot.startMinute,
                            endHour = slot.endHour,
                            endMinute = slot.endMinute,
                            timeFormatted = slot.timeString,
                            absences = course.absences,
                            maxAbsences = course.maxAbsences
                        )
                    )
                }
            }
        }

        // Sort by start time
        return items.sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }

    fun findNextClassStatus(
        courses: List<CourseEntity>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): NextClassStatus {
        val todayClasses = getTodayClasses(courses, nowEpochMs)
        if (todayClasses.isEmpty()) {
            return NextClassStatus(
                hasClassesToday = false,
                statusText = "No classes scheduled for today 🎉"
            )
        }

        val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        // 1. Check for ongoing class
        for (item in todayClasses) {
            val startMinutes = item.startHour * 60 + item.startMinute
            val endMinutes = item.endHour * 60 + item.endMinute
            if (currentMinutesOfDay in startMinutes..endMinutes) {
                val remainingMins = (endMinutes - currentMinutesOfDay).coerceAtLeast(1)
                return NextClassStatus(
                    hasClassesToday = true,
                    isOngoing = true,
                    activeClass = item,
                    statusText = "Ongoing Class",
                    countdownText = "$remainingMins min left"
                )
            }
        }

        // 2. Check for next upcoming class today
        for (item in todayClasses) {
            val startMinutes = item.startHour * 60 + item.startMinute
            if (startMinutes > currentMinutesOfDay) {
                val diffMins = startMinutes - currentMinutesOfDay
                val countdown = if (diffMins < 60) {
                    "in $diffMins mins"
                } else {
                    val hrs = diffMins / 60
                    val mins = diffMins % 60
                    if (mins == 0) "in ${hrs}h" else "in ${hrs}h ${mins}m"
                }
                return NextClassStatus(
                    hasClassesToday = true,
                    isOngoing = false,
                    activeClass = item,
                    statusText = "Next Class",
                    countdownText = countdown
                )
            }
        }

        // 3. All classes done for today
        return NextClassStatus(
            hasClassesToday = true,
            allDoneToday = true,
            statusText = "All classes done for today 🎉"
        )
    }

    private data class ParsedSlot(
        val day: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val timeString: String,
        val room: String
    )

    private fun parseSlots(slotsJson: String?): List<ParsedSlot> {
        if (slotsJson.isNullOrBlank() || slotsJson == "[]") return emptyList()
        val list = mutableListOf<ParsedSlot>()
        try {
            val root = Json.parseToJsonElement(slotsJson).jsonArray
            for (element in root) {
                val obj = element.jsonObject
                val day = obj["day"]?.jsonPrimitive?.content ?: ""
                val time = obj["time"]?.jsonPrimitive?.content ?: ""
                val start = obj["start"]?.jsonPrimitive?.content ?: ""
                val end = obj["end"]?.jsonPrimitive?.content ?: ""
                val room = obj["room"]?.jsonPrimitive?.content ?: ""

                val (sH, sM, eH, eM) = parseTimeRange(time.ifBlank { "$start-$end" })
                val timeString = if (time.isNotBlank()) time else "$start-$end"
                list.add(
                    ParsedSlot(
                        day = day,
                        startHour = sH,
                        startMinute = sM,
                        endHour = eH,
                        endMinute = eM,
                        timeString = timeString.ifBlank { "TBA" },
                        room = room
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseTimeRange(timeStr: String): IntArray {
        // e.g. "11:30 - 12:45" or "11:30-12:45" or "09:00 - 10:15"
        val regex = Regex("""(\d{1,2}):(\d{2})\s*([AaPp][Mm])?\s*[-–—to]+\s*(\d{1,2}):(\d{2})\s*([AaPp][Mm])?""")
        val match = regex.find(timeStr)
        if (match != null) {
            var sH = match.groupValues[1].toIntOrNull() ?: 9
            val sM = match.groupValues[2].toIntOrNull() ?: 0
            val sAmPm = match.groupValues[3].uppercase()

            var eH = match.groupValues[4].toIntOrNull() ?: 10
            val eM = match.groupValues[5].toIntOrNull() ?: 15
            val eAmPm = match.groupValues[6].uppercase()

            if (sAmPm == "PM" && sH < 12) sH += 12
            if (sAmPm == "AM" && sH == 12) sH = 0

            if (eAmPm == "PM" && eH < 12) eH += 12
            if (eAmPm == "AM" && eH == 12) eH = 0

            // If end hour < start hour and both lack AM/PM, adjust assuming daytime
            if (eH < sH && eH <= 6) eH += 12

            return intArrayOf(sH, sM, eH, eM)
        }

        // Fallback default
        return intArrayOf(9, 0, 10, 15)
    }

    private fun matchesDay(dayStr: String, today: DayOfWeek): Boolean {
        val upper = dayStr.uppercase()
        return when (today) {
            DayOfWeek.MONDAY -> upper.contains("MON") || upper.startsWith("M") || upper.contains("M/W") || upper.contains("MW")
            DayOfWeek.TUESDAY -> upper.contains("TUE") || upper.contains("TU") || upper.contains("TUTH") || upper.contains("TU/TH")
            DayOfWeek.WEDNESDAY -> upper.contains("WED") || upper.contains("W") || upper.contains("M/W") || upper.contains("MW")
            DayOfWeek.THURSDAY -> upper.contains("THU") || upper.contains("TH") || upper.contains("TUTH") || upper.contains("TU/TH")
            DayOfWeek.FRIDAY -> upper.contains("FRI") || upper.contains("F")
            DayOfWeek.SATURDAY -> upper.contains("SAT")
            DayOfWeek.SUNDAY -> upper.contains("SUN")
        }
    }
}
