package com.letdigitalfly.ibamail.ai

import android.content.Context
import com.letdigitalfly.ibamail.domain.model.ExtractedEntities
import com.letdigitalfly.ibamail.domain.model.MailCategory
import com.letdigitalfly.ibamail.domain.model.SummaryLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SummaryResult(
    val headline: String,
    val bullets: List<String>,
    val facts: Map<String, String>,
    val actionSuggestion: String
)

class OfflineSummarizer(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun summarize(
        category: MailCategory,
        subject: String,
        bodyText: String,
        entities: ExtractedEntities,
        language: SummaryLanguage = SummaryLanguage.ENGLISH
    ): SummaryResult {
        val headline = buildHeadline(category, subject, entities, language)
        val bullets = extractBestKeyPoints(bodyText, entities, language)
        val facts = buildKeyFacts(entities, language)
        val action = getActionSuggestion(category, language)

        return SummaryResult(
            headline = headline,
            bullets = bullets,
            facts = facts,
            actionSuggestion = action
        )
    }

    private fun buildHeadline(
        category: MailCategory,
        subject: String,
        entities: ExtractedEntities,
        language: SummaryLanguage
    ): String {
        val templateFileName = when (language) {
            SummaryLanguage.ENGLISH -> "templates_en.json"
            SummaryLanguage.ROMAN_URDU -> "templates_roman_urdu.json"
            SummaryLanguage.URDU -> "templates_urdu.json"
        }

        var templateString = try {
            val content = context.assets.open(templateFileName).bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(content).jsonObject
            val headlinesObj = root["headlines"]?.jsonObject
            headlinesObj?.get(category.idString)?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }

        if (templateString == null) {
            templateString = when (language) {
                SummaryLanguage.ENGLISH -> "{course} notice: {when}"
                SummaryLanguage.ROMAN_URDU -> "{course} ahem notice {when}"
                SummaryLanguage.URDU -> "{course} اہم اطلاع {when}"
            }
        }

        val coursePart = entities.courseCode ?: ""
        val whenPart = entities.dateText ?: ""
        val wherePart = entities.room ?: entities.campus ?: ""

        var headline = templateString
            .replace("{course}", coursePart)
            .replace("{when}", whenPart)
            .replace("{where}", wherePart)
            .replace("{topic}", subject.take(30))
            .trim()

        // Clean up excess spaces or dangling colons
        headline = headline.replace("  ", " ").replace(" :", ":").replace(" :", "").trim()
        if (headline.isBlank() || headline == ":" || headline == "-") {
            headline = subject.take(60)
        }
        return headline
    }

    private fun extractBestKeyPoints(
        bodyText: String,
        entities: ExtractedEntities,
        language: SummaryLanguage
    ): List<String> {
        val rawSentences = bodyText.split(Regex("[.!?\n]+"))
            .map { it.trim() }
            .filter { it.length > 10 }

        if (rawSentences.isEmpty()) {
            return when (language) {
                SummaryLanguage.ENGLISH -> listOf("Important academic notification for your attention.")
                SummaryLanguage.ROMAN_URDU -> listOf("Aap ki academic obligations ke mutalliq ahem notice.")
                SummaryLanguage.URDU -> listOf("آپ کی تعلیمی ذمہ داریوں کے متعلق اہم نوٹس۔")
            }
        }

        val actionVerbs = listOf("submit", "cancel", "rescheduled", "postponed", "attend", "bring", "register", "apply", "contact", "pay", "due")

        val scored = rawSentences.map { sentence ->
            val lower = sentence.lowercase()
            var score = 0
            if (entities.courseCode != null && lower.contains(entities.courseCode.lowercase())) score += 5
            if (entities.room != null && lower.contains(entities.room.lowercase())) score += 4
            if (actionVerbs.any { lower.contains(it) }) score += 3
            if (lower.contains("am") || lower.contains("pm") || lower.contains("baje") || lower.contains("date")) score += 3
            if (lower.contains("http://") || lower.contains("https://")) score += 2
            if (sentence.length in 25..120) score += 2
            sentence to score
        }.sortedByDescending { it.second }

        val best = scored.take(3).map { pair ->
            // Shorten to ~16 words max
            val words = pair.first.split(Regex("\\s+"))
            if (words.size > 16) {
                words.take(16).joinToString(" ") + "..."
            } else {
                pair.first
            }
        }

        return best.ifEmpty { listOf(bodyText.take(100)) }
    }

    private fun buildKeyFacts(entities: ExtractedEntities, language: SummaryLanguage): Map<String, String> {
        val facts = mutableMapOf<String, String>()

        val whenLabel = when (language) {
            SummaryLanguage.ENGLISH -> "When"
            SummaryLanguage.ROMAN_URDU -> "Kab"
            SummaryLanguage.URDU -> "کب"
        }
        val whereLabel = when (language) {
            SummaryLanguage.ENGLISH -> "Where"
            SummaryLanguage.ROMAN_URDU -> "Kahan"
            SummaryLanguage.URDU -> "کہاں"
        }
        val courseLabel = when (language) {
            SummaryLanguage.ENGLISH -> "Course"
            SummaryLanguage.ROMAN_URDU -> "Course"
            SummaryLanguage.URDU -> "کورس"
        }
        val linkLabel = when (language) {
            SummaryLanguage.ENGLISH -> "Link"
            SummaryLanguage.ROMAN_URDU -> "Portal link"
            SummaryLanguage.URDU -> "لنک"
        }

        if (entities.dateText != null) {
            facts[whenLabel] = entities.dateText
        }
        if (entities.room != null || entities.campus != null) {
            val place = listOfNotNull(entities.room, entities.campus?.let { "$it Campus" }).joinToString(", ")
            facts[whereLabel] = place
        }
        if (entities.courseCode != null) {
            val sec = if (!entities.section.isNullOrBlank()) " Section ${entities.section}" else ""
            facts[courseLabel] = "${entities.courseCode}$sec"
        }
        if (entities.link != null) {
            facts[linkLabel] = entities.link
        }

        return facts
    }

    private fun getActionSuggestion(category: MailCategory, language: SummaryLanguage): String {
        return when (category) {
            MailCategory.CLASS_CHANGE -> when (language) {
                SummaryLanguage.ENGLISH -> "Check your timetable"
                SummaryLanguage.ROMAN_URDU -> "Timetable check karein"
                SummaryLanguage.URDU -> "ٹائم ٹیبل دیکھیں"
            }
            MailCategory.EXAM -> when (language) {
                SummaryLanguage.ENGLISH -> "Prepare chapters and set alarm"
                SummaryLanguage.ROMAN_URDU -> "Tayyari karein aur alarm lagayein"
                SummaryLanguage.URDU -> "تیاری کریں اور الارم لگائیں"
            }
            MailCategory.DEADLINE -> when (language) {
                SummaryLanguage.ENGLISH -> "Upload assignment before cutoff"
                SummaryLanguage.ROMAN_URDU -> "Deadline se pehle submit karein"
                SummaryLanguage.URDU -> "ڈیڈ لائن سے قبل جمع کروائیں"
            }
            MailCategory.FEE -> when (language) {
                SummaryLanguage.ENGLISH -> "Submit voucher to avoid fine"
                SummaryLanguage.ROMAN_URDU -> "Challan jama karwayein"
                SummaryLanguage.URDU -> "چالان جمع کروائیں"
            }
            MailCategory.REGISTRATION -> when (language) {
                SummaryLanguage.ENGLISH -> "Login to portal during window"
                SummaryLanguage.ROMAN_URDU -> "Registration portal par jayein"
                SummaryLanguage.URDU -> "رجسٹریشن پورٹل پر جائیں"
            }
            MailCategory.RESULT -> when (language) {
                SummaryLanguage.ENGLISH -> "View grade breakdown on ERP"
                SummaryLanguage.ROMAN_URDU -> "ERP par grades check karein"
                SummaryLanguage.URDU -> "گریڈز چیک کریں"
            }
            MailCategory.ATTENDANCE -> when (language) {
                SummaryLanguage.ENGLISH -> "Meet instructor or program office"
                SummaryLanguage.ROMAN_URDU -> "Teacher ya office se baat karein"
                SummaryLanguage.URDU -> "دفتر سے رجوع کریں"
            }
            MailCategory.CAREER -> when (language) {
                SummaryLanguage.ENGLISH -> "Apply with updated resume"
                SummaryLanguage.ROMAN_URDU -> "Resume submit karein"
                SummaryLanguage.URDU -> "درخواست جمع کروائیں"
            }
            MailCategory.EVENT -> when (language) {
                SummaryLanguage.ENGLISH -> "Add to calendar and attend"
                SummaryLanguage.ROMAN_URDU -> "Calendar mein add karein"
                SummaryLanguage.URDU -> "کیلنڈر میں شامل کریں"
            }
            MailCategory.GENERAL -> when (language) {
                SummaryLanguage.ENGLISH -> "Read full announcement"
                SummaryLanguage.ROMAN_URDU -> "Poori mail parhein"
                SummaryLanguage.URDU -> "مکمل اطلاع پڑھیں"
            }
        }
    }
}
