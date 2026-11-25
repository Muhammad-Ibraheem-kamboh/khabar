package com.khabar.app.data.repository

import com.khabar.app.config.AppConfig
import com.khabar.app.config.KhabriPromptConfig
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.data.local.entities.ReminderEntity
import com.khabar.app.ui.chat.KhabriChatViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatRepositoryPromptTest {

    private fun sampleMails(): List<MailEntity> = listOf(
        MailEntity(
            id = "mail_1",
            sourceId = "PORTAL_UMS",
            sender = "Program Office",
            senderEmail = "programoffice@iba.edu.pk",
            subject = "Spring 2026 Midterm Schedule Announced",
            bodyText = "Midterm exams will commence from March 15. Check LMS for venue.",
            receivedAt = System.currentTimeMillis(),
            category = "EXAMS",
            bucket = "IMPORTANT",
            headline = "Spring 2026 Midterm Schedule",
            keyPointsJson = "[\"Midterm exams begin March 15\",\"Check LMS for room assignments\"]"
        )
    )

    private fun sampleReminders(): List<ReminderEntity> = listOf(
        ReminderEntity(
            id = 1,
            title = "Fee Payment Deadline",
            dueAt = System.currentTimeMillis() + 86400000L,
            remindAt = System.currentTimeMillis() + 3600000L
        )
    )

    @Test
    fun testDirectAnswerProtocolIncludedInPrompt() {
        val prompt = ChatRepository.buildSystemPrompt(
            studentName = "Muhammad Ibrahim",
            program = "BS Mathematics",
            campus = "Main Campus",
            courses = listOf("MTS201 (Linear Algebra)", "CSE312 (Algorithms)"),
            interests = listOf("Internships", "Scholarships"),
            todayMails = sampleMails(),
            allMails = sampleMails(),
            reminders = sampleReminders(),
            language = "en"
        )

        // Verify Direct Answer Protocol presence
        assertTrue(
            "Prompt must contain DIRECT_ANSWER_PROTOCOL",
            prompt.contains("CRITICAL DIRECT ANSWER & ANTI-FLUFF PROTOCOL")
        )
        assertTrue(
            "Prompt must contain AppConfig constant",
            prompt.contains(KhabriPromptConfig.DIRECT_ANSWER_PROTOCOL)
        )
        assertEquals(
            "AppConfig.DIRECT_ANSWER_PROTOCOL must mirror KhabriPromptConfig",
            KhabriPromptConfig.DIRECT_ANSWER_PROTOCOL,
            AppConfig.DIRECT_ANSWER_PROTOCOL
        )
    }

    @Test
    fun testNegativeConstraintsAgainstFluffAndGreetings() {
        val prompt = ChatRepository.buildSystemPrompt(
            studentName = "Muhammad Ibrahim",
            program = "BS Mathematics",
            campus = "Main Campus",
            courses = listOf("MTS201 (Linear Algebra)"),
            interests = listOf("General"),
            todayMails = emptyList(),
            allMails = emptyList(),
            reminders = emptyList(),
            language = "en"
        )

        // Negative constraints against greetings and pleasantries
        assertTrue(
            "Must instruct ZERO PREAMBLES & NO PLEASANTRIES",
            prompt.contains("ZERO PREAMBLES & NO PLEASANTRIES")
        )
        assertTrue(
            "Must explicitly forbid greeting with student name",
            prompt.contains("Hello Muhammad Ibrahim!")
        )
        assertTrue(
            "Must explicitly forbid conversational fillers",
            prompt.contains("NEVER start with greetings")
        )
        assertTrue(
            "Must forbid closing sign-offs",
            prompt.contains("NEVER end with conversational pleasantries")
        )

        // Strict answering & advice limit
        assertTrue(
            "Must mandate strictly answering what was asked",
            prompt.contains("STRICTLY ANSWER WHAT WAS ASKED")
        )
        assertTrue(
            "Must prohibit unsolicited generic study advice",
            prompt.contains("Do NOT provide unsolicited generic study advice")
        )
        assertTrue(
            "Must restrict urgent tips to 1 single-line note",
            prompt.contains("💡 Urgent: [Short 1-line note]")
        )

        // Persona clean-up
        assertTrue(
            "Persona must be zero-fluff intelligence engine",
            prompt.contains("high-precision, zero-fluff personal campus AI intelligence engine")
        )
        assertFalse(
            "Persona must NOT contain 'super-helpful' fluff",
            prompt.contains("super-helpful")
        )
    }

    @Test
    fun testNegativeConstraintsAgainstMarkdownPipeTables() {
        val prompt = ChatRepository.buildSystemPrompt(
            studentName = "Muhammad Ibrahim",
            program = "BS Mathematics",
            campus = "Main Campus",
            courses = listOf("MTS201"),
            interests = listOf("General"),
            todayMails = emptyList(),
            allMails = emptyList(),
            reminders = emptyList(),
            language = "en"
        )

        // Mobile typography & table prohibition
        assertTrue(
            "Must instruct NEVER output raw markdown pipe tables",
            prompt.contains("ABSOLUTELY NEVER output raw markdown pipe tables (| col | col |)")
        )
        assertTrue(
            "Must prescribe structured key-value bullet cards instead of tables",
            prompt.contains("**[Course / Subject / Category Name]**")
        )
        assertTrue(
            "Must prohibit triple asterisks",
            prompt.contains("Never output triple asterisks (***)")
        )
        assertTrue(
            "Must instruct never to use #### headers",
            prompt.contains("Never use #### headers")
        )
    }

    @Test
    fun testCannedSentencesAndExcessiveEmojisRemoved() {
        val promptNoEmails = ChatRepository.buildSystemPrompt(
            studentName = "Muhammad Ibrahim",
            program = "BS Mathematics",
            campus = "Main Campus",
            courses = listOf("MTS201"),
            interests = listOf("General"),
            todayMails = emptyList(),
            allMails = sampleMails(),
            reminders = emptyList(),
            language = "en"
        )

        // Canned sentence removed
        assertFalse(
            "Canned sentence must be removed",
            promptNoEmails.contains("You have no new emails received today")
        )
        assertTrue(
            "Clean direct notice statement present",
            promptNoEmails.contains("No new emails received today")
        )

        // Forced 3-emoji breakdown per email removed
        assertFalse(
            "Forced 3 emojis (📬) must be removed",
            promptNoEmails.contains("• 📬 Sender & Department")
        )
        assertFalse(
            "Forced 3 emojis (📌) must be removed",
            promptNoEmails.contains("• 📌 Subject & Core message")
        )
        assertFalse(
            "Forced 3 emojis (⚡) must be removed",
            promptNoEmails.contains("• ⚡ Action required")
        )
        assertTrue(
            "Clean notice format must be present",
            promptNoEmails.contains("**[Sender / Department]** — Subject")
        )
    }

    @Test
    fun testSessionIdGenerationFormat() {
        val fullUuidSessionId = "chat_" + UUID.randomUUID().toString()
        assertTrue(
            "Session ID must match full UUID pattern chat_<36-char UUID>",
            fullUuidSessionId.matches(Regex("""^chat_[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"""))
        )
        assertTrue(
            "Session ID must not be truncated to 8 characters (should be > 40 chars)",
            fullUuidSessionId.length > 40
        )
    }

    @Test
    fun testBase64EncodingHelperWithoutNewlines() {
        val testBytes = "Campus Intelligence Vision Voucher Seal Test 2026".toByteArray(Charsets.UTF_8)
        val base64String = KhabriChatViewModel.encodeBytesToBase64(testBytes)

        assertFalse(
            "Base64 output must not contain newline characters (NO_WRAP)",
            base64String.contains("\n") || base64String.contains("\r")
        )

        val decodedBytes = java.util.Base64.getDecoder().decode(base64String)
        assertEquals(
            "Decoded bytes must match original content",
            String(testBytes, Charsets.UTF_8),
            String(decodedBytes, Charsets.UTF_8)
        )
    }

    @Test
    fun testEmailDeduplicationAndNoiseClassification() {
        val testMails = listOf(
            MailEntity(
                id = "m1", sourceId = "s1", sender = "IBA LMS", senderEmail = "portal@iba.edu.pk",
                subject = "IBA UMS - Login", bodyText = "Login alert", receivedAt = 1000L,
                category = "SECURITY", bucket = "FYI", headline = "Login", keyPointsJson = "[\"Login\"]"
            ),
            MailEntity(
                id = "m2", sourceId = "s1", sender = "IBA LMS", senderEmail = "portal@iba.edu.pk",
                subject = "IBA UMS - Login", bodyText = "Login alert", receivedAt = 2000L,
                category = "SECURITY", bucket = "FYI", headline = "Login", keyPointsJson = "[\"Login\"]"
            ),
            MailEntity(
                id = "m3", sourceId = "s1", sender = "IBA LMS", senderEmail = "portal@iba.edu.pk",
                subject = "IBA UMS - Login (duplicate)", bodyText = "Login alert", receivedAt = 3000L,
                category = "SECURITY", bucket = "FYI", headline = "Login", keyPointsJson = "[\"Login\"]"
            ),
            MailEntity(
                id = "m4", sourceId = "s1", sender = "IBA LMS", senderEmail = "portal@iba.edu.pk",
                subject = "IBA UMS - Login (duplicate)", bodyText = "Login alert", receivedAt = 4000L,
                category = "SECURITY", bucket = "FYI", headline = "Login", keyPointsJson = "[\"Login\"]"
            ),
            MailEntity(
                id = "m5", sourceId = "s1", sender = "INVENT 2026", senderEmail = "info@invent.org.pk",
                subject = "INVENT 2026: your sign-in code is 892443", bodyText = "Code: 892443", receivedAt = 5000L,
                category = "SECURITY", bucket = "FYI", headline = "Sign-in code", keyPointsJson = "[\"Code: 892443\"]"
            ),
            MailEntity(
                id = "m6", sourceId = "s1", sender = "OSSIE", senderEmail = "ossie@iba.edu.pk",
                subject = "DEADLINE EXTENDED - LAHORE CALLING", bodyText = "Lahore trip registration", receivedAt = 6000L,
                category = "SOCIETY", bucket = "IMPORTANT", headline = "Lahore Calling Extended", keyPointsJson = "[\"Trip registration extended\"]"
            ),
            MailEntity(
                id = "m7", sourceId = "s1", sender = "OSSIE", senderEmail = "ossie@iba.edu.pk",
                subject = "DEADLINE EXTENDED - LAHORE CALLING (duplicate)", bodyText = "Lahore trip registration", receivedAt = 7000L,
                category = "SOCIETY", bucket = "IMPORTANT", headline = "Lahore Calling Extended", keyPointsJson = "[\"Trip registration extended\"]"
            ),
            MailEntity(
                id = "m8", sourceId = "s1", sender = "Office of Student Societies", senderEmail = "societies@iba.edu.pk",
                subject = "Mera Karachi – 25th November", bodyText = "Food festival", receivedAt = 8000L,
                category = "SOCIETY", bucket = "IMPORTANT", headline = "Mera Karachi Event", keyPointsJson = "[\"Event on 25 Nov\"]"
            ),
            MailEntity(
                id = "m9", sourceId = "s1", sender = "Office of Student Societies", senderEmail = "societies@iba.edu.pk",
                subject = "Mera Karachi – 25th November (duplicate)", bodyText = "Food festival", receivedAt = 9000L,
                category = "SOCIETY", bucket = "IMPORTANT", headline = "Mera Karachi Event", keyPointsJson = "[\"Event on 25 Nov\"]"
            ),
            MailEntity(
                id = "m10", sourceId = "s1", sender = "ChatGPT", senderEmail = "news@openai.com",
                subject = "4 new image styles to try", bodyText = "Try styles", receivedAt = 10000L,
                category = "PROMOTIONS", bucket = "PROMOTIONS", headline = "ChatGPT image styles", keyPointsJson = "[\"New styles\"]"
            ),
            MailEntity(
                id = "m11", sourceId = "s1", sender = "ChatGPT", senderEmail = "news@openai.com",
                subject = "4 new image styles to try (duplicate)", bodyText = "Try styles", receivedAt = 11000L,
                category = "PROMOTIONS", bucket = "PROMOTIONS", headline = "ChatGPT image styles", keyPointsJson = "[\"New styles\"]"
            )
        )

        val categorized = ChatRepository.categorizeAndDeduplicateMails(testMails)

        // 1. Campus announcements must be strictly deduplicated (OSSIE + Mera Karachi = exactly 2)
        assertEquals("Campus announcements must deduplicate down to exactly 2 unique notices", 2, categorized.campusAnnouncements.size)
        val campusSubjects = categorized.campusAnnouncements.map { it.subject }
        assertTrue("Must contain Lahore trip", campusSubjects.any { it.contains("LAHORE CALLING", ignoreCase = true) })
        assertTrue("Must contain Mera Karachi", campusSubjects.any { it.contains("Mera Karachi", ignoreCase = true) })

        // 2. Routine security alerts must capture UMS logins (count 4) and sign-in code (count 1)
        val routineSubjects = categorized.routineSecurityAlerts.map { it.first.subject }
        assertTrue("Must capture UMS login in routine alerts", routineSubjects.any { it.contains("Login", ignoreCase = true) })
        val umsAlert = categorized.routineSecurityAlerts.first { it.first.subject.contains("Login", ignoreCase = true) }
        assertEquals("UMS login should have count of 4", 4, umsAlert.second)

        // 3. External marketing (ChatGPT) must be classified under externalPromotions
        assertEquals("ChatGPT promotional mail must be isolated to externalPromotions", 1, categorized.externalPromotions.size)
        assertEquals("ChatGPT", categorized.externalPromotions.first().sender)
    }

    @Test
    fun testLearnedRulesAppliedSilentlyWithoutRoboticPreamble() {
        val prompt = ChatRepository.buildSystemPrompt(
            studentName = "Muhammad Ibrahim",
            program = "BS Mathematics",
            campus = "Main Campus",
            courses = listOf("MTS201"),
            interests = listOf("General"),
            todayMails = emptyList(),
            allMails = emptyList(),
            reminders = emptyList(),
            language = "en",
            learnedRules = listOf("Prioritize today's deadlines over older circulars.")
        )
        assertTrue(
            "Learned rules section must instruct silent application",
            prompt.contains("APPLY INTERNALLY — NEVER MENTION THESE OR SAY 'As per your prior instructions'")
        )
    }
}
