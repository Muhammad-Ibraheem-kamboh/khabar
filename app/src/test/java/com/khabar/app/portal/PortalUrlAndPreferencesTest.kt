package com.khabar.app.portal

import com.khabar.app.config.AppConfig
import com.khabar.app.data.local.UserPreferences
import com.khabar.app.ui.portal.PortalContentParser
import com.khabar.app.ui.portal.PortalErrorState
import com.khabar.app.ui.portal.PortalSecurityHelper
import com.khabar.app.ui.portal.PortalTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalUrlAndPreferencesTest {

    @Test
    fun testDefaultPortalUrlsMatchRequirements() {
        assertEquals("https://ums.iba.edu.pk/", UserPreferences.DEFAULT_UMS_URL)
        assertEquals("https://lms.iba.edu.pk/", UserPreferences.DEFAULT_LMS_URL)
        assertEquals("https://ums.iba.edu.pk/", AppConfig.PORTAL_URL)
    }

    @Test
    fun testPortalPreferenceKeysAccessible() {
        assertEquals("portal_url", UserPreferences.Keys.PORTAL_URL.name)
        assertEquals("lms_url", UserPreferences.Keys.LMS_URL.name)
        assertEquals("custom_portal_url", UserPreferences.Keys.CUSTOM_PORTAL_URL.name)
    }

    @Test
    fun testObsoletePortalUrlDetection() {
        // Obsolete portal.iba.edu.pk must be flagged as invalid/obsolete
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("https://portal.iba.edu.pk"))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("http://portal.iba.edu.pk/"))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("https://portal.iba.edu.pk/dashboard"))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("http://portal.iba.edu.pk:8080"))

        // Blank, empty, null, and non-web URLs must be flagged
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl(null))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl(""))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("   "))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("javascript:void(0)"))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("ftp://files.iba.edu.pk"))

        // Malformed or schemeless URLs without valid hosts must be flagged
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("https://"))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("http://"))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("https://   "))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("https://..."))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("https://a b c"))
        assertTrue(UserPreferences.isInvalidOrObsoletePortalUrl("ums.iba.edu.pk"))

        // Valid IBA and custom portal URLs must NOT be flagged
        assertFalse(UserPreferences.isInvalidOrObsoletePortalUrl("https://ums.iba.edu.pk/"))
        assertFalse(UserPreferences.isInvalidOrObsoletePortalUrl("https://ums.iba.edu.pk"))
        assertFalse(UserPreferences.isInvalidOrObsoletePortalUrl("https://lms.iba.edu.pk/"))
        assertFalse(UserPreferences.isInvalidOrObsoletePortalUrl("https://lms.iba.edu.pk"))
        assertFalse(UserPreferences.isInvalidOrObsoletePortalUrl("https://iba.edu.pk/"))
        assertFalse(UserPreferences.isInvalidOrObsoletePortalUrl("https://custom-erp.campus.edu"))
    }

    @Test
    fun testNormalizePortalUrl() {
        // Auto-prepends https:// when user enters a domain without scheme
        assertEquals("https://ums.iba.edu.pk", UserPreferences.normalizePortalUrl("ums.iba.edu.pk"))
        assertEquals("https://lms.iba.edu.pk", UserPreferences.normalizePortalUrl("lms.iba.edu.pk", UserPreferences.DEFAULT_LMS_URL))
        assertEquals("https://canvas.iba.edu.pk", UserPreferences.normalizePortalUrl("  canvas.iba.edu.pk  "))

        // Retains existing http:// or https://
        assertEquals("https://ums.iba.edu.pk/", UserPreferences.normalizePortalUrl("https://ums.iba.edu.pk/"))
        assertEquals("http://custom-erp.local:8080", UserPreferences.normalizePortalUrl("http://custom-erp.local:8080"))

        // Resets obsolete portal.iba.edu.pk variations to default
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl("https://portal.iba.edu.pk"))
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl("portal.iba.edu.pk"))
        assertEquals(UserPreferences.DEFAULT_LMS_URL, UserPreferences.normalizePortalUrl("https://portal.iba.edu.pk/home", UserPreferences.DEFAULT_LMS_URL))

        // Null, blank, or malformed inputs fall back to default
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl(null))
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl(""))
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl("   "))
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl("https://"))
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl("http://"))
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl("https://..."))
        assertEquals(UserPreferences.DEFAULT_UMS_URL, UserPreferences.normalizePortalUrl("https://a b c"))
    }

    @Test
    fun testPortalContentParser() {
        // Normal evaluation result from WebView evaluateJavascript
        val jsonOutput = "\"{\\\"title\\\":\\\"IBA LMS - CS101\\\",\\\"text\\\":\\\"Assignment 1 due Nov 30 at 23:59\\\"}\""
        val (title1, text1) = PortalContentParser.parseExtractedContent(jsonOutput)
        assertEquals("IBA LMS - CS101", title1)
        assertEquals("Assignment 1 due Nov 30 at 23:59", text1)

        // Handles multiline text and quotes
        val multilineOutput = "\"{\\\"title\\\":\\\"Announcements\\\",\\\"text\\\":\\\"Notice 1:\\\\nMidterm syllabus uploaded.\\\\n\\\\\\\"Bring Admit Card\\\\\\\" required.\\\"}\""
        val (title2, text2) = PortalContentParser.parseExtractedContent(multilineOutput)
        assertEquals("Announcements", title2)
        assertTrue(text2.contains("Midterm syllabus uploaded."))
        assertTrue(text2.contains("\"Bring Admit Card\""))

        // Handles fallback regex extraction when JSON has unescaped/corrupted content
        val malformedJson = "{ title: \"Fallback Notice\", text: \"Important exam instructions.\" }"
        val (fallbackTitle, fallbackText) = PortalContentParser.parseExtractedContent(malformedJson)
        assertEquals("Fallback Notice", fallbackTitle)
        assertEquals("Important exam instructions.", fallbackText)

        // Handles null, empty, or "null" safely
        val (emptyTitle1, emptyText1) = PortalContentParser.parseExtractedContent(null)
        assertEquals("", emptyTitle1)
        assertEquals("", emptyText1)

        val (emptyTitle2, emptyText2) = PortalContentParser.parseExtractedContent("null")
        assertEquals("", emptyTitle2)
        assertEquals("", emptyText2)

        val (emptyTitle3, emptyText3) = PortalContentParser.parseExtractedContent("")
        assertEquals("", emptyTitle3)
        assertEquals("", emptyText3)
    }

    @Test
    fun testExtractHost() {
        assertEquals("ums.iba.edu.pk", PortalSecurityHelper.extractHost("https://ums.iba.edu.pk/"))
        assertEquals("ums.iba.edu.pk", PortalSecurityHelper.extractHost("ums.iba.edu.pk"))
        assertEquals("lms.iba.edu.pk", PortalSecurityHelper.extractHost("http://lms.iba.edu.pk:8080/portal"))
        assertEquals("custom.iba.edu.pk", PortalSecurityHelper.extractHost("https://user@custom.iba.edu.pk/home"))
        assertEquals(null, PortalSecurityHelper.extractHost(null))
        assertEquals(null, PortalSecurityHelper.extractHost(""))
        assertEquals(null, PortalSecurityHelper.extractHost("   "))
    }

    @Test
    fun testSslHostTrustClassification() {
        // IBA Karachi academic & auth domains must be trusted
        assertTrue(PortalSecurityHelper.isTrustedSslHost("ums.iba.edu.pk"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("lms.iba.edu.pk"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("iba.edu.pk"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("adfs.iba.edu.pk"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("sts.iba.edu.pk"))

        // Microsoft Azure AD and SSO domains must be trusted
        assertTrue(PortalSecurityHelper.isTrustedSslHost("login.microsoftonline.com"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("login.live.com"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("account.live.com"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("login.windows.net"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("login.microsoft.com"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("msauth.net"))

        // Custom configured portal host must be trusted when active
        assertTrue(PortalSecurityHelper.isTrustedSslHost("custom-erp.iba.edu.pk", "https://custom-erp.iba.edu.pk/portal"))
        assertTrue(PortalSecurityHelper.isTrustedSslHost("mycampus.edu", "https://mycampus.edu/login"))

        // Malicious spoofing / phishing attempts without dot boundary MUST be rejected
        assertFalse(PortalSecurityHelper.isTrustedSslHost("fakeiba.edu.pk"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("not-iba.edu.pk"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("iba.edu.attacker.com"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("evilmicrosoft.com"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("fakelive.com"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("maliciousoffice.com"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("attacker-office365.com"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("mywindows.net"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("fakemsauth.net"))

        // Untrusted/third-party arbitrary domains must NOT be automatically trusted
        assertFalse(PortalSecurityHelper.isTrustedSslHost("untrusted-attacker.com"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost("phishing-iba.fake.net"))
        assertFalse(PortalSecurityHelper.isTrustedSslHost(null))
        assertFalse(PortalSecurityHelper.isTrustedSslHost(""))
    }

    @Test
    fun testShouldKeepInWebView() {
        // Web browsing & page scripts stay inside WebView
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("http"))
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("https"))
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("HTTP"))
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("HTTPS"))
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("javascript"))
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("about"))
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("data"))
        assertTrue(PortalSecurityHelper.shouldKeepInWebView("blob"))

        // External app schemes are delegated out
        assertFalse(PortalSecurityHelper.shouldKeepInWebView("intent"))
        assertFalse(PortalSecurityHelper.shouldKeepInWebView("msauth"))
        assertFalse(PortalSecurityHelper.shouldKeepInWebView("mailto"))
        assertFalse(PortalSecurityHelper.shouldKeepInWebView("tel"))
        assertFalse(PortalSecurityHelper.shouldKeepInWebView(null))
        assertFalse(PortalSecurityHelper.shouldKeepInWebView(""))
    }

    @Test
    fun testPortalTabPresets() {
        assertEquals("IBA UMS", PortalTab.UMS.title)
        assertEquals("https://ums.iba.edu.pk/", PortalTab.UMS.defaultUrl)

        assertEquals("IBA LMS", PortalTab.LMS.title)
        assertEquals("https://lms.iba.edu.pk/", PortalTab.LMS.defaultUrl)

        assertEquals("Custom Portal", PortalTab.CUSTOM.title)
        assertEquals("https://iba.edu.pk/", PortalTab.CUSTOM.defaultUrl)
    }

    @Test
    fun testPortalErrorStateProperties() {
        val dnsError = PortalErrorState(
            errorCode = -2,
            description = "net::ERR_NAME_NOT_RESOLVED",
            failingUrl = "https://portal.iba.edu.pk",
            isDnsFailure = true
        )
        assertTrue(dnsError.isDnsFailure)
        assertEquals(-2, dnsError.errorCode)
        assertEquals("net::ERR_NAME_NOT_RESOLVED", dnsError.description)
        assertEquals("https://portal.iba.edu.pk", dnsError.failingUrl)

        val regularError = PortalErrorState(
            errorCode = -6,
            description = "Connection timed out",
            failingUrl = "https://ums.iba.edu.pk/",
            isDnsFailure = false
        )
        assertFalse(regularError.isDnsFailure)
        assertEquals(-6, regularError.errorCode)
    }
}
