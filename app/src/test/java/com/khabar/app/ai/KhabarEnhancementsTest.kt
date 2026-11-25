package com.khabar.app.ai

import com.khabar.app.domain.model.PortalCourseDetector
import com.khabar.app.ui.chat.parseInlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KhabarEnhancementsTest {

    @Test
    fun testPortalCourseDetectorDetectsValidCourseCodes() {
        val portalText = """
            Student Enrollment Dashboard
            Term: Spring 2026
            Registered Courses:
            1. MTS201 - Linear Algebra (Section A)
            2. CSE-312: Algorithms and Complexity
            3. ECO101 Macroeconomics
            Notice: PAGE 1 of 2. HTTP 200 OK. ROOM 302.
        """.trimIndent()

        val courses = PortalCourseDetector.detectCourses(portalText, defaultSection = "A")

        val codes = courses.map { it.code }
        assertTrue("MTS201 should be detected", codes.contains("MTS201"))
        assertTrue("CSE312 should be detected", codes.contains("CSE312"))
        assertTrue("ECO101 should be detected", codes.contains("ECO101"))

        // Ensure blacklisted false positives are filtered out
        assertFalse("PAGE should be filtered out", codes.contains("PAGE1"))
        assertFalse("ROOM should be filtered out", codes.contains("ROOM302"))
        assertFalse("HTTP should be filtered out", codes.contains("HTTP200"))
    }

    @Test
    fun testRecoverableModelErrorDetection() {
        assertTrue("404 should be recoverable", OpenAiCompatibleClient.isRecoverableModelError("HTTP 404: Not Found"))
        assertTrue("410 should be recoverable", OpenAiCompatibleClient.isRecoverableModelError("HTTP 410: Gone"))
        assertTrue("503 should be recoverable", OpenAiCompatibleClient.isRecoverableModelError("HTTP 503: Service Unavailable"))
        assertTrue("model_not_found should be recoverable", OpenAiCompatibleClient.isRecoverableModelError("error: model_not_found"))
        assertTrue("deprecated should be recoverable", OpenAiCompatibleClient.isRecoverableModelError("The model 'gemini-1.5-flash' is deprecated"))
        assertTrue("resource exhausted should be recoverable", OpenAiCompatibleClient.isRecoverableModelError("RESOURCE_EXHAUSTED (code 429)"))

        // Non-recoverable generic errors
        assertFalse("Null should not be recoverable", OpenAiCompatibleClient.isRecoverableModelError(null))
        assertFalse("Blank should not be recoverable", OpenAiCompatibleClient.isRecoverableModelError(""))
        assertFalse("Success should not be recoverable", OpenAiCompatibleClient.isRecoverableModelError("Success"))
    }

    @Test
    fun testFallbackAndVisionModelPresets() {
        val geminiFallbacks = KeystoreSecretManager.getFallbackModels("Gemini")
        assertTrue("Gemini should have fallback models", geminiFallbacks.isNotEmpty())
        assertTrue("Gemini should include flash-lite", geminiFallbacks.contains("gemini-flash-lite-latest"))

        val groqFallbacks = KeystoreSecretManager.getFallbackModels("Groq")
        assertTrue("Groq should have fallback models", groqFallbacks.isNotEmpty())
        assertTrue("Groq should include gpt-oss-120b", groqFallbacks.contains("openai/gpt-oss-120b"))

        val geminiVision = KeystoreSecretManager.getVisionModel("Gemini")
        assertEquals("gemini-flash-lite-latest", geminiVision)

        val groqVision = KeystoreSecretManager.getVisionModel("Groq")
        assertEquals("meta-llama/llama-4-scout-17b-16e-instruct", groqVision)
    }

    @Test
    fun testMultimodalChatMessageConstruction() {
        val textOnlyMsg = ChatMessage(role = "user", content = "Hello Khabri")
        assertEquals("user", textOnlyMsg.role)
        assertEquals("Hello Khabri", textOnlyMsg.content)
        assertEquals(null, textOnlyMsg.imageBase64)

        val imageMsg = ChatMessage(
            role = "user",
            content = "Analyze fee voucher",
            imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
            imageMimeType = "image/png"
        )
        assertNotNull(imageMsg.imageBase64)
        assertEquals("image/png", imageMsg.imageMimeType)
    }

    @Test
    fun testInlineMarkdownParser() {
        val parsed = parseInlineMarkdown("Check your **MTS201** exam in `Room 204` now!")
        val text = parsed.text
        assertEquals("Check your MTS201 exam in  Room 204  now!", text)

        // Verify span styles were added
        val spans = parsed.spanStyles
        assertTrue("Should have span styles for bold and code", spans.size >= 2)
    }
}
