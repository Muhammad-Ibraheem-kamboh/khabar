package com.khabar.app.ui.chat

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserUnitTest {

    @Test
    fun testHeaderParsing() {
        // H1 through H6
        val h1 = parseHeader("# Campus Announcements")
        assertNotNull(h1)
        assertEquals(1, h1!!.level)
        assertEquals("Campus Announcements", h1.text)
        assertFalse(h1.isRtl)

        val h2 = parseHeader("## Examination Schedule ##")
        assertNotNull(h2)
        assertEquals(2, h2!!.level)
        assertEquals("Examination Schedule", h2.text)

        val h3 = parseHeader("### Fee Payment Deadline")
        assertNotNull(h3)
        assertEquals(3, h3!!.level)
        assertEquals("Fee Payment Deadline", h3.text)

        val h4 = parseHeader("#### 📬 Today's Email & Campus Notice Summary")
        assertNotNull(h4)
        assertEquals(4, h4!!.level)
        assertEquals("📬 Today's Email & Campus Notice Summary", h4.text)

        val h5 = parseHeader("##### Room Allocation")
        assertNotNull(h5)
        assertEquals(5, h5!!.level)
        assertEquals("Room Allocation", h5.text)

        val h6 = parseHeader("###### Sub-detail note")
        assertNotNull(h6)
        assertEquals(6, h6!!.level)
        assertEquals("Sub-detail note", h6.text)

        // Non-header lines
        val nonHeader = parseHeader("Regular paragraph text with # in the middle")
        assertEquals(null, nonHeader)

        // Urdu Header
        val urduH2 = parseHeader("## اہم اعلانات")
        assertNotNull(urduH2)
        assertEquals(2, urduH2!!.level)
        assertEquals("اہم اعلانات", urduH2.text)
        assertTrue("Urdu header should be detected as RTL", urduH2.isRtl)
    }

    @Test
    fun testMarkdownTableParsing() {
        val tableLines = listOf(
            "| # | 📬 Sender | ⚡ Subject & Key Message | Action Required |",
            "|---|---|---|---|",
            "| 1 | UMS Office | Fee voucher deadline is 25th Nov | Pay before 5 PM |",
            "| 2 | Dr. Tariq | Quiz on Monday in Room 204 | Prepare Chapter 3 |"
        )

        val table = parseTable(tableLines)
        assertNotNull("Table should be parsed successfully", table)
        assertEquals(4, table!!.headers.size)
        assertEquals("#", table.headers[0])
        assertEquals("📬 Sender", table.headers[1])
        assertEquals("⚡ Subject & Key Message", table.headers[2])
        assertEquals("Action Required", table.headers[3])

        assertEquals(2, table.rows.size)
        assertEquals(listOf("1", "UMS Office", "Fee voucher deadline is 25th Nov", "Pay before 5 PM"), table.rows[0])
        assertEquals(listOf("2", "Dr. Tariq", "Quiz on Monday in Room 204", "Prepare Chapter 3"), table.rows[1])

        // Verify raw pipes are stripped
        for (h in table.headers) {
            assertFalse("Header should not contain raw pipes", h.contains("|"))
        }
        for (row in table.rows) {
            for (cell in row) {
                assertFalse("Cell should not contain raw pipes", cell.contains("|"))
            }
        }
    }

    @Test
    fun testUrduTableDetection() {
        val urduTableLines = listOf(
            "| نمبر | بھیجنے والا | تفصیل |",
            "|:---|:---:|---:|",
            "| ۱ | رجسٹرار آفس | چھٹی کا اعلان |"
        )

        val table = parseTable(urduTableLines)
        assertNotNull(table)
        assertTrue("Urdu table should have isRtl = true", table!!.isRtl)
        assertEquals(3, table.headers.size)
        assertEquals(1, table.rows.size)
    }

    @Test
    fun testTablePaddingForMissingCells() {
        val raggedTable = listOf(
            "| Col A | Col B | Col C |",
            "|---|---|---|",
            "| Cell 1 | Cell 2 |", // only 2 cells instead of 3
            "| Cell 3 | Cell 4 | Cell 5 | Cell 6 |" // 4 cells
        )

        val table = parseTable(raggedTable)
        assertNotNull(table)
        assertEquals(3, table!!.headers.size)
        assertEquals(2, table.rows.size)
        assertEquals(3, table.rows[0].size)
        assertEquals("", table.rows[0][2]) // Padded with empty string
        assertEquals(3, table.rows[1].size) // Truncated to header count
    }

    @Test
    fun testBulletNormalizingAndNesting() {
        val bulletDash = parseBulletItem("- Simple dash item")
        assertNotNull(bulletDash)
        assertEquals(0, bulletDash!!.indentLevel)
        assertEquals("Simple dash item", bulletDash.text)

        val bulletAsterisk = parseBulletItem("* Simple asterisk item")
        assertNotNull(bulletAsterisk)
        assertEquals(0, bulletAsterisk!!.indentLevel)
        assertEquals("Simple asterisk item", bulletAsterisk.text)

        val bulletUnicode = parseBulletItem("• Unicode bullet point")
        assertNotNull(bulletUnicode)
        assertEquals(0, bulletUnicode!!.indentLevel)
        assertEquals("Unicode bullet point", bulletUnicode.text)

        // Indented nested bullet with bold noise
        val indentedBold = parseBulletItem("   * **Sender:** Sakai Notification")
        assertNotNull(indentedBold)
        assertEquals(1, indentedBold!!.indentLevel)
        assertEquals("**Sender:** Sakai Notification", indentedBold.text)

        // Triple asterisk bullet
        val tripleAsterisk = parseBulletItem("*** Clean bullet without noise")
        assertNotNull(tripleAsterisk)
        assertEquals(0, tripleAsterisk!!.indentLevel)
        assertEquals("Clean bullet without noise", tripleAsterisk.text)

        // Messy redundant bullets: "* * **Action:**"
        val messyBullet = parseBulletItem("* * **Action:** Submit assignment")
        assertNotNull(messyBullet)
        assertEquals("**Action:** Submit assignment", messyBullet!!.text)

        // Standalone divider lines should not be parsed as bullets
        assertEquals(null, parseBulletItem("***"))
        assertEquals(null, parseBulletItem("---"))
        assertEquals(null, parseBulletItem("___"))
    }

    @Test
    fun testNumberedListParsing() {
        val item1 = parseNumberedItem("1. First course MTS201")
        assertNotNull(item1)
        assertEquals("1.", item1!!.number)
        assertEquals("First course MTS201", item1.text)

        val item2 = parseNumberedItem("   2) Second course CSE141")
        assertNotNull(item2)
        assertEquals("2.", item2!!.number)
        assertEquals("Second course CSE141", item2.text)

        val item10 = parseNumberedItem("10. Tenth item on list")
        assertNotNull(item10)
        assertEquals("10.", item10!!.number)
        assertEquals("Tenth item on list", item10.text)
    }

    @Test
    fun testMonospaceChipAndInlineFormatting() {
        // Course code detection (e.g. MTS201)
        val textWithCourse = parseInlineMarkdown("Please attend MTS201 in Room 204 or contact portal@iba.edu.pk")
        val rawText = textWithCourse.text
        assertTrue("Course code should be in text", rawText.contains("MTS201"))
        assertTrue("Email should be in text", rawText.contains("portal@iba.edu.pk"))

        val spans = textWithCourse.spanStyles
        // Spans should style MTS201 and email as monospace
        val monospaceSpans = spans.filter { it.item.fontFamily == FontFamily.Monospace }
        assertTrue("Should have monospace spans for course code and email", monospaceSpans.size >= 2)

        // Course code with space
        val textWithSpacedCourse = parseInlineMarkdown("Register for CSE 141 before Friday")
        assertTrue(textWithSpacedCourse.text.contains("CSE 141"))
        val courseSpan = textWithSpacedCourse.spanStyles.filter { it.item.fontFamily == FontFamily.Monospace }
        assertTrue("CSE 141 should be monospace styled", courseSpan.isNotEmpty())

        // Blacklisted words should NOT be styled as course codes
        val textWithBlacklist = parseInlineMarkdown("Check PAGE 1 and HTTP 200 and ROOM 302")
        val blacklistMonospaceSpans = textWithBlacklist.spanStyles.filter { it.item.fontFamily == FontFamily.Monospace }
        assertEquals("Blacklisted words should not create monospace chips", 0, blacklistMonospaceSpans.size)

        // Bold text
        val boldText = parseInlineMarkdown("This is **critical** information")
        assertEquals("This is critical information", boldText.text)
        val boldSpans = boldText.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)

        // Links
        val linkText = parseInlineMarkdown("Visit [IBA Portal](https://ums.iba.edu.pk) for results")
        assertEquals("Visit IBA Portal for results", linkText.text)
    }

    @Test
    fun testRtlDetection() {
        // Urdu text
        assertTrue("Urdu sentence must be RTL", isRtlText("آج کی تمام ای میلز کا خلاصہ"))
        assertTrue("Urdu with leading emoji must be RTL", isRtlText("📬 بھیجنے والا: یو ایم ایس آفس"))
        assertTrue("Urdu question must be RTL", isRtlText("کیا کل کوئی کوئز ہے؟"))

        // English text
        assertFalse("English sentence must be LTR", isRtlText("Today's campus notices and emails"))
        assertFalse("English with leading emoji must be LTR", isRtlText("⚡ Action Required: Pay fee voucher"))
        assertFalse("Numbers and symbols should not trigger RTL", isRtlText("12345: HTTP 200 OK"))
    }

    @Test
    fun testFullMarkdownDocumentParsing() {
        val markdownDoc = """
            #### 📬 Today's Email & Campus Notice Summary
            Here is what was received today from IBA Administration:
            
            | # | Course | Status |
            |---|---|---|
            | 1 | MTS201 | Quiz graded |
            | 2 | CSE141 | Assignment 2 posted |
            
            * **Important:** Submit to umsnoreply@iba.edu.pk before 5 PM.
            ***
            1. Review your quiz score on Sakai.
            2. Complete the coding assignment.
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdownDoc)
        assertTrue("Should have multiple blocks", blocks.size >= 6)

        // Block 0: Header level 4
        assertTrue(blocks[0] is MarkdownBlock.Header)
        val h = blocks[0] as MarkdownBlock.Header
        assertEquals(4, h.level)
        assertEquals("📬 Today's Email & Campus Notice Summary", h.text)

        // Block 1: Paragraph
        assertTrue(blocks[1] is MarkdownBlock.Paragraph)

        // Block 2: Table
        assertTrue(blocks[2] is MarkdownBlock.Table)
        val table = blocks[2] as MarkdownBlock.Table
        assertEquals(3, table.headers.size)
        assertEquals(2, table.rows.size)

        // Block 3: Bullet item
        assertTrue(blocks[3] is MarkdownBlock.BulletItem)
        val bullet = blocks[3] as MarkdownBlock.BulletItem
        assertTrue(bullet.text.startsWith("**Important:**"))

        // Block 4: Divider
        assertTrue(blocks[4] is MarkdownBlock.Divider)

        // Block 5 & 6: Numbered items
        assertTrue(blocks[5] is MarkdownBlock.NumberedItem)
        assertTrue(blocks[6] is MarkdownBlock.NumberedItem)
    }

    @Test
    fun testPsychologicalGreeting() {
        // Morning
        val morningGreeting = getPsychologicalGreeting("Ibraheem", "en", hour = 9)
        assertTrue("Morning title should include Morning hustle", morningGreeting.first.contains("Morning hustle"))
        assertTrue("Morning title should include student name", morningGreeting.first.contains("Ibraheem"))

        // Afternoon
        val middayGreeting = getPsychologicalGreeting("Abdaal", "en", hour = 14)
        assertTrue("Midday title should include Midday check-in", middayGreeting.first.contains("Midday check-in"))
        assertTrue("Midday title should include student name", middayGreeting.first.contains("Abdaal"))

        // Night
        val nightGreeting = getPsychologicalGreeting("Ibraheem", "en", hour = 22)
        assertTrue("Night title should include Late night focus", nightGreeting.first.contains("Late night focus"))

        // Urdu
        val urduGreeting = getPsychologicalGreeting("ابراہیم", "ur", hour = 10)
        assertTrue("Urdu morning should contain صبح بخیر", urduGreeting.first.contains("صبح بخیر"))

        // Roman Urdu
        val romanUrduGreeting = getPsychologicalGreeting("Ibraheem", "roman_urdu", hour = 15)
        assertTrue("Roman Urdu midday should contain Midday check-in", romanUrduGreeting.first.contains("Midday check-in"))
    }

    @Test
    fun testBlockquoteAndBoldItalicParsing() {
        val raw = """
            > Deadline extended to Friday 5 PM.
            This has ***critical alert*** and **MTS201** details.
        """.trimIndent()

        val blocks = parseMarkdownBlocks(raw)
        assertEquals(2, blocks.size)
        assertTrue("First block should be Blockquote", blocks[0] is MarkdownBlock.Blockquote)
        val quote = blocks[0] as MarkdownBlock.Blockquote
        assertEquals("Deadline extended to Friday 5 PM.", quote.text)

        val inline = parseInlineMarkdown("This has ***critical alert*** inside.")
        assertEquals("This has critical alert inside.", inline.text)
        assertFalse("Must not leak triple asterisks", inline.text.contains("***"))
    }
}
