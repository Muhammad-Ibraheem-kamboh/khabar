package com.khabar.app.util

import com.khabar.app.config.CampusConfig
import com.khabar.app.data.local.entities.CourseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class ClassScheduleAndOfflineTest {

    @Test
    fun testCampusConfigLookup() {
        val iba = CampusConfig.getCampusById("iba")
        assertEquals("IBA Karachi", iba.name)
        assertEquals(5, iba.maxAbsencesAllowed)

        val fast = CampusConfig.getCampusById("fast")
        assertEquals("FAST NUCES", fast.name)

        val nust = CampusConfig.getCampusById("nust")
        assertEquals("NUST Islamabad", nust.name)

        val fallback = CampusConfig.getCampusById("unknown")
        assertEquals("IBA Karachi", fallback.name)
    }

    @Test
    fun testClassScheduleParsingAndSorting() {
        val todayName = LocalDate.now(ZoneId.systemDefault()).dayOfWeek.name.take(3)
        val slotsJson = """[{"day":"$todayName","time":"11:30 - 12:45","room":"Aman CED 204"}]"""

        val course = CourseEntity(
            id = 101L,
            code = "MTS201",
            section = "A",
            name = "Linear Algebra",
            slotsJson = slotsJson,
            absences = 2,
            maxAbsences = 5
        )

        val classes = ClassScheduleHelper.getTodayClasses(listOf(course))
        assertEquals(1, classes.size)
        val item = classes.first()
        assertEquals("MTS201", item.courseCode)
        assertEquals("Aman CED 204", item.room)
        assertEquals(11, item.startHour)
        assertEquals(30, item.startMinute)
        assertEquals(2, item.absences)
        assertEquals(5, item.maxAbsences)
    }

    @Test
    fun testNextClassStatusComputation() {
        val todayName = LocalDate.now(ZoneId.systemDefault()).dayOfWeek.name.take(3)
        // Schedule class at 23:50 tonight (guaranteed in future today)
        val slotsJson = """[{"day":"$todayName","time":"23:50 - 23:59","room":"Tabba 101"}]"""

        val course = CourseEntity(
            id = 102L,
            code = "CSE312",
            name = "Algorithms",
            slotsJson = slotsJson
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
        }

        val status = ClassScheduleHelper.findNextClassStatus(listOf(course), cal.timeInMillis)
        assertTrue(status.hasClassesToday)
        assertFalse(status.isOngoing)
        assertNotNull(status.activeClass)
        assertEquals("CSE312", status.activeClass?.courseCode)
        assertEquals("Tabba 101", status.activeClass?.room)
        assertTrue(status.countdownText.contains("h") || status.countdownText.contains("m"))
    }

    @Test
    fun testEmptyScheduleReturnsFriendlyState() {
        val status = ClassScheduleHelper.findNextClassStatus(emptyList())
        assertFalse(status.hasClassesToday)
        assertTrue(status.statusText.contains("No classes"))
    }
}
