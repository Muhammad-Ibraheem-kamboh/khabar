package com.khabar.app.portal

import com.khabar.app.data.local.entities.CourseEntity
import com.khabar.app.data.repository.ChatRepository
import com.khabar.app.domain.model.PortalCourseDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalCourseDetectorTest {

    @Test
    fun testVariedIbaSectionFormatsDetection() {
        val portalText = """
            IBA Karachi - Student Enrollment Summary
            Semester: Spring 2026
            
            1. MTS201 - Linear Algebra (Section SS-1)
               Class # 4103 | Tue/Thu 11:30-12:45 | Venue: Aman CED 204 | Instructor: Dr. Yaseen
               
            2. CSE-312: Algorithms and Complexity - Sec B
               Mon/Wed 08:30-09:45 | Room: Tabba 101 | Teacher: Dr. Imran
               
            3. ECO101 Macroeconomics [Section L1]
               Fri 09:00-12:00 | FCS Lab 1
               
            4. ACC111 Principles of Accounting Sec. A
               Tue/Thu 14:30-15:45 | Adamjee 2
               
            5. FIN201 Financial Management (Section: CS-2)
               Mon/Wed 10:00-11:15 | Room: MCS-3
               
            6. LAW105 Business Law
               Notice: PAGE 1 of 2. HTTP 200 OK. ROOM 302. FALL 2025.
        """.trimIndent()

        val courses = PortalCourseDetector.detectCourses(portalText, defaultSection = "A")
        val courseMap = courses.associateBy { it.code }

        // 1. Verify MTS201
        val mts = courseMap["MTS201"]
        assertNotNull("MTS201 must be detected", mts)
        assertEquals("MTS201 section should be SS-1", "SS-1", mts?.section)
        assertEquals("MTS201 title should be Linear Algebra", "Linear Algebra", mts?.name)
        assertTrue("MTS201 slotsJson should contain Tue/Thu", mts?.slotsJson?.contains("Tue/Thu") == true)
        assertTrue("MTS201 slotsJson should contain 11:30-12:45", mts?.slotsJson?.contains("11:30-12:45") == true)
        assertTrue("MTS201 slotsJson should contain Aman CED 204", mts?.slotsJson?.contains("Aman CED 204") == true)

        // 2. Verify CSE312
        val cse = courseMap["CSE312"]
        assertNotNull("CSE312 must be detected", cse)
        assertEquals("CSE312 section should be B", "B", cse?.section)
        assertEquals("CSE312 title should be Algorithms and Complexity", "Algorithms and Complexity", cse?.name)
        assertTrue("CSE312 slotsJson should contain Mon/Wed", cse?.slotsJson?.contains("Mon/Wed") == true)
        assertTrue("CSE312 slotsJson should contain 08:30-09:45", cse?.slotsJson?.contains("08:30-09:45") == true)

        // 3. Verify ECO101
        val eco = courseMap["ECO101"]
        assertNotNull("ECO101 must be detected", eco)
        assertEquals("ECO101 section should be L1", "L1", eco?.section)
        assertEquals("ECO101 title should be Macroeconomics", "Macroeconomics", eco?.name)

        // 4. Verify ACC111
        val acc = courseMap["ACC111"]
        assertNotNull("ACC111 must be detected", acc)
        assertEquals("ACC111 section should be A", "A", acc?.section)
        assertEquals("ACC111 title should be Principles of Accounting", "Principles of Accounting", acc?.name)

        // 5. Verify FIN201
        val fin = courseMap["FIN201"]
        assertNotNull("FIN201 must be detected", fin)
        assertEquals("FIN201 section should be CS-2", "CS-2", fin?.section)

        // 6. Verify LAW105 fallback section
        val law = courseMap["LAW105"]
        assertNotNull("LAW105 must be detected", law)
        assertEquals("LAW105 should fall back to default section A", "A", law?.section)

        // 7. Verify false positives are strictly filtered out
        assertFalse("PAGE should not be detected as course", courseMap.containsKey("PAGE1"))
        assertFalse("HTTP should not be detected as course", courseMap.containsKey("HTTP200"))
        assertFalse("ROOM should not be detected as course", courseMap.containsKey("ROOM302"))
        assertFalse("FALL should not be detected as course", courseMap.containsKey("FALL2025"))
    }

    @Test
    fun testCourseScheduleFormatting() {
        val slotsJson = """[{"day":"Tue/Thu","time":"11:30-12:45","room":"Aman CED 204"}]"""
        val formatted = PortalCourseDetector.formatCourseSchedule(slotsJson)
        assertEquals("Tue/Thu 11:30-12:45 (Aman CED 204)", formatted)

        val emptyFormatted = PortalCourseDetector.formatCourseSchedule("[]")
        assertEquals("", emptyFormatted)

        val nullFormatted = PortalCourseDetector.formatCourseSchedule(null)
        assertEquals("", nullFormatted)
    }

    @Test
    fun testCoursePromptLineFormatting() {
        val course = CourseEntity(
            code = "MTS201",
            section = "SS-1",
            name = "Linear Algebra",
            slotsJson = """[{"day":"Tue/Thu","time":"11:30-12:45","room":"Aman CED 204"}]"""
        )

        val promptLine = PortalCourseDetector.formatCoursePromptLine(course)
        assertEquals("• MTS201 (Linear Algebra) - Section: SS-1 | Slots: Tue/Thu 11:30-12:45 (Aman CED 204)", promptLine)
    }

    @Test
    fun testChatRepositoryPromptIncludesSemesterAndCourseSections() {
        val sampleCourses = listOf(
            "• MTS201 (Linear Algebra) - Section: SS-1 | Slots: Tue/Thu 11:30-12:45",
            "• CSE312 (Algorithms and Complexity) - Section: B | Slots: Mon/Wed 08:30-09:45"
        )

        val prompt = ChatRepository.buildSystemPrompt(
            studentName = "Muhammad Ibrahim",
            program = "BS Mathematics",
            campus = "Main Campus",
            courses = sampleCourses,
            interests = listOf("Artificial Intelligence", "Cybersecurity"),
            todayMails = emptyList(),
            allMails = emptyList(),
            reminders = emptyList(),
            language = "en",
            activeSemester = "Semester 5, Junior, Spring 2026"
        )

        // Verify active semester is present
        assertTrue(
            "Prompt must include active semester",
            prompt.contains("- Active Semester: Semester 5, Junior, Spring 2026")
        )

        // Verify course sections and timetable slots are present
        assertTrue(
            "Prompt must format MTS201 with Section SS-1",
            prompt.contains("• MTS201 (Linear Algebra) - Section: SS-1 | Slots: Tue/Thu 11:30-12:45")
        )
        assertTrue(
            "Prompt must format CSE312 with Section B",
            prompt.contains("• CSE312 (Algorithms and Complexity) - Section: B | Slots: Mon/Wed 08:30-09:45")
        )
    }

    @Test
    fun testPeopleSoftTableFormatScraping() {
        val tableText = """
            Class Schedule - Spring 2026
            Course	Title	Sec	Day/Time	Room	Instructor
            MTS201	Linear Algebra	SS-1	TuTh 11:30AM - 12:45PM	Aman CED 204	Dr. Yaseen
            CSE312	Algorithms	B	MW 08:30AM - 09:45AM	Tabba 101	Dr. Imran
        """.trimIndent()

        val courses = PortalCourseDetector.detectCourses(tableText, defaultSection = "A")
        val courseMap = courses.associateBy { it.code }

        assertEquals(2, courses.size)
        assertEquals("SS-1", courseMap["MTS201"]?.section)
        assertEquals("B", courseMap["CSE312"]?.section)
    }
}
