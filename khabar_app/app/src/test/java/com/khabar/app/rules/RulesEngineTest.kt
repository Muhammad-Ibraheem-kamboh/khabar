package com.khabar.app.rules

import com.khabar.app.domain.model.MailBucket
import com.khabar.app.domain.model.MailCategory
import com.khabar.app.domain.model.RawMail
import com.khabar.app.util.SemesterCalculator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RulesEngineTest {

    private lateinit var rulesEngine: RulesEngine
    private lateinit var profileView: UserProfileRulesView

    @Before
    fun setUp() {
        rulesEngine = RulesEngine()

        // Ali Raza, BSCS Fall 23, Main Campus (Sophomore)
        val courses = listOf(
            UserCourseInfo(code = "CS201", section = "A", name = "Data Structures"),
            UserCourseInfo(code = "CS204", section = "B", name = "Computer Organization"),
            UserCourseInfo(code = "MTH201", section = "A", name = "Differential Equations"),
            UserCourseInfo(code = "SS102", section = "A", name = "Pakistan History"),
            UserCourseInfo(code = "MKT201", section = "A", name = "Principles of Marketing"),
            UserCourseInfo(code = "FIN201", section = "B", name = "Financial Management"),
            UserCourseInfo(code = "ECO103", section = "C", name = "Principles of Microeconomics")
        )

        val interests = mapOf(
            "INTERNSHIPS" to true,
            "SOCIETIES" to true,
            "SPORTS" to false,
            "EXCHANGE" to true
        )

        profileView = UserProfileRulesView(
            programId = "bs_cs",
            programName = "BSCS",
            joinTerm = "Fall",
            joinYear = 2023,
            semesterIndex = 3,
            standing = "Sophomore",
            campus = "Main",
            major = "Computer Science",
            shift = "Morning",
            courses = courses,
            interests = interests
        )
    }

    @Test
    fun testSemesterCalculator() {
        val standing = SemesterCalculator.calculateStanding("Fall", 2023, 2024, 10)
        assertNotNull(standing)
    }

    @Test
    fun testCategoryDetectorDirectly() {
        val cat1 = CategoryDetector.detect(
            subject = "Class cancelled: CS201 Data Structures",
            body = "Please note that the lecture for CS201 scheduled today is cancelled."
        )
        assertEquals(MailCategory.CLASS_CHANGE, cat1.primary)

        val cat2 = CategoryDetector.detect(
            subject = "Fee challan Fall installment 2",
            body = "Students are advised to download their fee vouchers and deposit dues before the deadline."
        )
        assertEquals(MailCategory.FEE, cat2.primary)
    }

    @Test
    fun testEntityExtractor() {
        val entities = EntityExtractor.extract(
            subject = "CS201 Quiz 2 in CS-04",
            body = "The quiz will take place tomorrow at 10:00 AM in room CS-04 Main Campus. Submission link: https://portal.iba.edu.pk",
            receivedAtEpochMs = System.currentTimeMillis()
        )

        assertEquals("CS201", entities.courseCode)
        assertEquals("CS-04", entities.room)
        assertEquals("https://portal.iba.edu.pk", entities.link)
    }

    @Test
    fun testAll30SampleMailsFromAssets() {
        val sampleMailsFile = findSampleMailsFile()
        assertTrue("sample_mails.json should exist", sampleMailsFile.exists())

        val jsonString = sampleMailsFile.readText()
        val json = Json { ignoreUnknownKeys = true }
        val array = json.parseToJsonElement(jsonString).jsonArray

        assertEquals("Should have exactly 30 sample emails", 30, array.size)

        var testedCount = 0

        for (item in array) {
            val obj = item.jsonObject
            val id = obj["id"]!!.jsonPrimitive.content
            val subject = obj["subject"]!!.jsonPrimitive.content
            val sender = obj["sender"]!!.jsonPrimitive.content
            val senderEmail = obj["senderEmail"]!!.jsonPrimitive.content
            val body = obj["bodyText"]?.jsonPrimitive?.content ?: ""
            val expectedCategoryStr = obj["expectedCategory"]!!.jsonPrimitive.content
            val expectedBucketStr = obj["expectedBucket"]!!.jsonPrimitive.content

            val rawMail = RawMail(
                id = id,
                sourceId = id,
                sender = sender,
                senderEmail = senderEmail,
                subject = subject,
                bodyText = body,
                receivedAt = System.currentTimeMillis()
            )

            val processed = rulesEngine.process(
                mail = rawMail,
                profile = profileView
            )

            val expectedCat = MailCategory.valueOf(expectedCategoryStr)
            val expectedBkt = MailBucket.valueOf(expectedBucketStr)

            assertEquals(
                "Mail '$subject' category mismatch",
                expectedCat,
                processed.category
            )

            assertEquals(
                "Mail '$subject' bucket mismatch",
                expectedBkt,
                processed.bucket
            )

            testedCount++
        }

        assertEquals(30, testedCount)
    }

    private fun findSampleMailsFile(): File {
        val paths = listOf(
            "src/main/assets/sample_mails.json",
            "app/src/main/assets/sample_mails.json",
            "../app/src/main/assets/sample_mails.json",
            "khabar_app/app/src/main/assets/sample_mails.json"
        )
        for (p in paths) {
            val f = File(p)
            if (f.exists()) return f
        }
        return File("src/main/assets/sample_mails.json")
    }
}
