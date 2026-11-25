package com.khabar.app.ui.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.ui.theme.KhabarBlue
import com.khabar.app.ui.theme.IosSecondaryText

/**
 * AST Data Model for parsed Markdown blocks.
 */
sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String, val isRtl: Boolean) : MarkdownBlock()
    data class Paragraph(val text: String, val isRtl: Boolean) : MarkdownBlock()
    data class BulletItem(val indentLevel: Int, val text: String, val isRtl: Boolean) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String, val isRtl: Boolean) : MarkdownBlock()
    data class Blockquote(val text: String, val isRtl: Boolean) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>, val isRtl: Boolean) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

/**
 * Checks if the text has Right-to-Left directionality (Urdu, Arabic, Hebrew).
 * Inspects code points to properly handle multi-byte Unicode emojis and characters.
 */
fun isRtlText(text: String): Boolean {
    var i = 0
    val length = text.length
    while (i < length) {
        val cp = text.codePointAt(i)
        val dir = Character.getDirectionality(cp)
        if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
            (cp in 0x0590..0x08FF) || (cp in 0xFB1D..0xFEFC)
        ) {
            return true
        } else if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT ||
            (cp in 0x0041..0x005A) || (cp in 0x0061..0x007A)
        ) {
            return false
        }
        i += Character.charCount(cp)
    }
    return false
}

private val COURSE_BLACKLIST = setOf(
    "PAGE", "ROOM", "HTTP", "TERM", "SECT", "WEEK", "SLOT", "SEMESTER", "CLASS", "STEP", "TEST", "HALL", "LAB"
)

/**
 * Parses inline markdown:
 * - Bold text: **text**
 * - Inline code: `code`
 * - Links: [label](url)
 * - Emails: styled in monospace chips
 * - Course codes: (e.g. MTS201, CSE141) styled in monospace pill chips
 * - Italic text: *text*
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("""(\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|`([^`]+)`|\[([^\]]+)\]\(([^)]+)\)|\b([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b|\b([A-Z]{2,4}[\s-]?\d{3}[A-Z]?)\b|(?<!\*)\*([^*]+)\*(?!\*))""")
        val matches = regex.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                append(text.substring(cursor, start))
            }

            val fullMatch = match.value
            when {
                // Bold + Italic: ***content***
                match.groupValues[2].isNotEmpty() -> {
                    val biContent = match.groupValues[2]
                    val biStart = length
                    append(biContent)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), biStart, length)
                }
                // Bold: **content**
                match.groupValues[3].isNotEmpty() -> {
                    val boldContent = match.groupValues[3]
                    val boldStart = length
                    append(boldContent)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), boldStart, length)
                }
                // Explicit inline code: `content`
                match.groupValues[4].isNotEmpty() -> {
                    val codeContent = match.groupValues[4]
                    val codeStart = length
                    append(" $codeContent ")
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x22888888),
                            fontSize = 13.sp
                        ),
                        codeStart,
                        length
                    )
                }
                // Link: [label](url)
                match.groupValues[5].isNotEmpty() -> {
                    val linkText = match.groupValues[5]
                    val linkStart = length
                    append(linkText)
                    addStyle(
                        SpanStyle(
                            color = KhabarBlue,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold
                        ),
                        linkStart,
                        length
                    )
                }
                // Email
                match.groupValues[7].isNotEmpty() -> {
                    val email = match.groupValues[7]
                    val emailStart = length
                    append(email)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = KhabarBlue,
                            background = Color(0x221E88E5),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        emailStart,
                        length
                    )
                }
                // Course code
                match.groupValues[8].isNotEmpty() -> {
                    val rawCourse = match.groupValues[8]
                    val prefix = rawCourse.takeWhile { it.isLetter() }.uppercase()
                    if (prefix !in COURSE_BLACKLIST) {
                        val chipStart = length
                        append(rawCourse)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = KhabarBlue,
                                background = Color(0x221E88E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            chipStart,
                            length
                        )
                    } else {
                        append(rawCourse)
                    }
                }
                // Italic: *content*
                match.groupValues[9].isNotEmpty() -> {
                    val italicContent = match.groupValues[9]
                    val italicStart = length
                    append(italicContent)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), italicStart, length)
                }
                else -> {
                    append(fullMatch)
                }
            }

            cursor = end
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

/**
 * Checks if a line resembles a markdown table row (begins with '|' and contains '|').
 */
fun isTableRow(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("|") && (trimmed.endsWith("|") || trimmed.count { it == '|' } >= 2)
}

/**
 * Checks if a line is a markdown table delimiter row (e.g., |---|---|).
 */
fun isTableDivider(line: String): Boolean {
    val trimmed = line.trim()
    if (!isTableRow(trimmed)) return false
    val cleaned = trimmed.replace("|", "").replace(":", "").replace("-", "").replace(" ", "").replace("\t", "")
    return cleaned.isEmpty() && trimmed.contains("-")
}

/**
 * Splits a table row into trimmed cell contents.
 */
fun splitRowCells(line: String): List<String> {
    val trimmed = line.trim().removeSurrounding("|")
    return trimmed.split("|").map { it.trim() }
}

/**
 * Parses consecutive table lines into a MarkdownBlock.Table model.
 */
fun parseTable(tableLines: List<String>): MarkdownBlock.Table? {
    if (tableLines.isEmpty()) return null
    val firstLine = tableLines[0].trim()
    if (isTableDivider(firstLine)) return null

    val headers = splitRowCells(firstLine)
    val dataRowLines = mutableListOf<String>()

    for (j in 1 until tableLines.size) {
        val line = tableLines[j].trim()
        if (isTableDivider(line)) {
            continue
        }
        dataRowLines.add(line)
    }

    val colCount = headers.size
    val rows = dataRowLines.map { line ->
        val cells = splitRowCells(line)
        if (cells.size < colCount) {
            cells + List(colCount - cells.size) { "" }
        } else {
            cells.take(colCount)
        }
    }

    val isRtl = headers.any { isRtlText(it) } || rows.any { r -> r.any { isRtlText(it) } }
    return MarkdownBlock.Table(headers = headers, rows = rows, isRtl = isRtl)
}

/**
 * Parses markdown header lines (# through ######).
 */
fun parseHeader(line: String): MarkdownBlock.Header? {
    val trimmed = line.trim()
    val match = Regex("""^(#{1,6})\s+(.+?)(?:\s+#+)?$""").find(trimmed) ?: return null
    val level = match.groupValues[1].length
    val text = match.groupValues[2].trim()
    return MarkdownBlock.Header(
        level = level,
        text = text,
        isRtl = isRtlText(text)
    )
}

/**
 * Parses numbered list items (e.g. 1. or 2) ).
 */
fun parseNumberedItem(line: String): MarkdownBlock.NumberedItem? {
    val match = Regex("""^\s*(\d+)[.)]\s+(.+)$""").find(line) ?: return null
    val number = "${match.groupValues[1]}."
    val text = match.groupValues[2].trim()
    return MarkdownBlock.NumberedItem(
        number = number,
        text = text,
        isRtl = isRtlText(text)
    )
}

/**
 * Parses bullet list items (-, *, •, +, ***) with nesting detection and noise cleanup.
 */
fun parseBulletItem(line: String): MarkdownBlock.BulletItem? {
    val trimmed = line.trim()
    if (trimmed == "***" || trimmed == "---" || trimmed == "___" ||
        trimmed == "- - -" || trimmed == "* * *"
    ) {
        return null
    }

    val match = Regex("""^(\s*)(?:[-*•+]|\*\*\*)\s+(.+)$""").find(line) ?: return null
    val leadingSpaces = match.groupValues[1].replace("\t", "  ").length
    val indentLevel = (leadingSpaces / 2).coerceIn(0, 3)
    var text = match.groupValues[2].trim()

    // Clean up redundant bullet characters or leading asterisk clutter (e.g. * ** -> **)
    while (text.startsWith("* ") || text.startsWith("- ") || text.startsWith("• ")) {
        text = text.substring(2).trim()
    }

    return MarkdownBlock.BulletItem(
        indentLevel = indentLevel,
        text = text,
        isRtl = isRtlText(text)
    )
}

/**
 * Parses full raw markdown message into an AST list of MarkdownBlock.
 */
fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = content.lines()
    var i = 0
    val total = lines.size

    var inCodeBlock = false
    var codeLanguage = ""
    val codeBlockLines = mutableListOf<String>()

    while (i < total) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Code blocks (```lang ... ```)
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(
                    MarkdownBlock.CodeBlock(
                        language = codeLanguage,
                        code = codeBlockLines.joinToString("\n")
                    )
                )
                codeBlockLines.clear()
                codeLanguage = ""
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeLanguage = trimmed.removePrefix("```").trim()
            }
            i++
            continue
        }

        if (inCodeBlock) {
            codeBlockLines.add(line)
            i++
            continue
        }

        // 2. Blank lines
        if (trimmed.isBlank()) {
            i++
            continue
        }

        // 3. Horizontal dividers (---, ***, ___)
        if (trimmed == "---" || trimmed == "***" || trimmed == "___" ||
            trimmed == "- - -" || trimmed == "* * *"
        ) {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 4. Markdown Pipe Tables
        if (isTableRow(line)) {
            val tableLines = mutableListOf<String>()
            while (i < total && isTableRow(lines[i])) {
                tableLines.add(lines[i])
                i++
            }
            val parsedTable = parseTable(tableLines)
            if (parsedTable != null) {
                blocks.add(parsedTable)
            } else {
                for (tl in tableLines) {
                    blocks.add(MarkdownBlock.Paragraph(tl.trim(), isRtlText(tl)))
                }
            }
            continue
        }

        // 5. Headers (# to ######)
        val header = parseHeader(line)
        if (header != null) {
            blocks.add(header)
            i++
            continue
        }

        // 6. Numbered lists (1. , 1) )
        val numbered = parseNumberedItem(line)
        if (numbered != null) {
            blocks.add(numbered)
            i++
            continue
        }

        // 7. Bullet lists (-, *, •, +, *** )
        val bullet = parseBulletItem(line)
        if (bullet != null) {
            blocks.add(bullet)
            i++
            continue
        }

        // 8. Blockquotes (> quote)
        if (trimmed.startsWith(">")) {
            val quoteText = trimmed.removePrefix(">").trimStart()
            if (quoteText.isNotBlank()) {
                blocks.add(MarkdownBlock.Blockquote(quoteText, isRtlText(quoteText)))
            }
            i++
            continue
        }

        // 9. Standard paragraph
        blocks.add(MarkdownBlock.Paragraph(trimmed, isRtlText(trimmed)))
        i++
    }

    // Flush any open code block at EOF
    if (inCodeBlock && codeBlockLines.isNotEmpty()) {
        blocks.add(
            MarkdownBlock.CodeBlock(
                language = codeLanguage,
                code = codeBlockLines.joinToString("\n")
            )
        )
    }

    return blocks
}

@Composable
fun RenderHeader(header: MarkdownBlock.Header, modifier: Modifier = Modifier) {
    val layoutDirection = if (header.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        val (style, color, topPadding) = when (header.level) {
            1 -> Triple(
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                MaterialTheme.colorScheme.onSurface,
                8.dp
            )
            2 -> Triple(
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                KhabarBlue,
                6.dp
            )
            3 -> Triple(
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                KhabarBlue,
                4.dp
            )
            4 -> Triple(
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                KhabarBlue,
                4.dp
            )
            5 -> Triple(
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                MaterialTheme.colorScheme.onSurface,
                3.dp
            )
            else -> Triple(
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                MaterialTheme.colorScheme.onSurface,
                2.dp
            )
        }

        Text(
            text = parseInlineMarkdown(header.text),
            style = style,
            color = color,
            modifier = modifier
                .fillMaxWidth()
                .padding(top = topPadding, bottom = 2.dp)
        )
    }
}

@Composable
fun RenderTable(table: MarkdownBlock.Table, modifier: Modifier = Modifier) {
    val layoutDirection = if (table.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Header row
                if (table.headers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(KhabarBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        table.headers.forEach { header ->
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 90.dp, max = 220.dp)
                                    .padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = parseInlineMarkdown(header),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = KhabarBlue
                                    )
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        thickness = 1.dp
                    )
                }

                // Data rows
                table.rows.forEachIndexed { rowIndex, row ->
                    val rowBg = if (rowIndex % 2 == 0) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    }
                    Row(
                        modifier = Modifier
                            .background(rowBg)
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { cell ->
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 90.dp, max = 220.dp)
                                    .padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = parseInlineMarkdown(cell),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                    if (rowIndex < table.rows.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenderBulletItem(bullet: MarkdownBlock.BulletItem, modifier: Modifier = Modifier) {
    val layoutDirection = if (bullet.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        val indentPadding = (bullet.indentLevel * 14).dp
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = indentPadding + 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (bullet.indentLevel == 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(KhabarBlue)
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(KhabarBlue.copy(alpha = 0.7f))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = parseInlineMarkdown(bullet.text),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RenderNumberedItem(numbered: MarkdownBlock.NumberedItem, modifier: Modifier = Modifier) {
    val layoutDirection = if (numbered.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = KhabarBlue.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 2.dp, end = 6.dp)
            ) {
                Text(
                    text = numbered.number,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KhabarBlue,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Text(
                text = parseInlineMarkdown(numbered.text),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RenderParagraph(paragraph: MarkdownBlock.Paragraph, modifier: Modifier = Modifier) {
    val layoutDirection = if (paragraph.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Text(
            text = parseInlineMarkdown(paragraph.text),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.fillMaxWidth()
        )
    }
}

@Composable
fun RenderBlockquote(quote: MarkdownBlock.Blockquote, modifier: Modifier = Modifier) {
    val layoutDirection = if (quote.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = KhabarBlue.copy(alpha = 0.08f),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(KhabarBlue)
                )
                Text(
                    text = parseInlineMarkdown(quote.text),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 21.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun RenderCodeBlock(codeBlock: MarkdownBlock.CodeBlock, modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E2E),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = codeBlock.language.ifBlank { "CODE" }.uppercase(),
                    color = Color(0xFF89B4FA),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(codeBlock.code))
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFF89B4FA),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        color = Color(0xFF89B4FA),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                text = codeBlock.code,
                color = Color(0xFFCDD6F4),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun RenderDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 1.dp,
        modifier = modifier.padding(vertical = 6.dp)
    )
}

/**
 * Main Composable for rendering chat messages with rich markdown typography.
 */
@Composable
fun FormattedChatMessage(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    if (isUser) {
        SelectionContainer {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = Color.White,
                modifier = modifier
            )
        }
        return
    }

    val blocks = remember(content) { parseMarkdownBlocks(content) }

    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (block in blocks) {
                when (block) {
                    is MarkdownBlock.Header -> RenderHeader(block)
                    is MarkdownBlock.Table -> RenderTable(block)
                    is MarkdownBlock.BulletItem -> RenderBulletItem(block)
                    is MarkdownBlock.NumberedItem -> RenderNumberedItem(block)
                    is MarkdownBlock.Blockquote -> RenderBlockquote(block)
                    is MarkdownBlock.CodeBlock -> RenderCodeBlock(block)
                    is MarkdownBlock.Paragraph -> RenderParagraph(block)
                    is MarkdownBlock.Divider -> RenderDivider()
                }
            }
        }
    }
}
