package com.khabar.app.ai

import com.khabar.app.config.AppConfig
import com.khabar.app.domain.model.SummaryLanguage
import com.khabar.app.rules.Normalizer
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AiSummaryResponse(
    val headline: String,
    val bullets: List<String>,
    val categoryId: String,
    val whenText: String?,
    val place: String?,
    val courses: List<String>,
    val link: String?,
    val action: String
) {
    val bulletPoints: List<String> get() = bullets
    val facts: Map<String, String> get() = buildMap {
        whenText?.let { put("when", it) }
        place?.let { put("where", it) }
        if (courses.isNotEmpty()) put("courses", courses.joinToString(", "))
        link?.let { put("link", it) }
    }
}

class AiSummarizer(
    private val client: OpenAiCompatibleClient,
    private val secretManager: KeystoreSecretManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var lastCallTimeMs = 0L

    // In-memory cache: "mailId_languageCode" -> AiSummaryResponse
    private val memoryCache = mutableMapOf<String, AiSummaryResponse>()

    fun isConfigured(): Boolean {
        val apiKey = secretManager.getAiApiKey().ifBlank { AppConfig.buildAiApiKey }
        val baseUrl = secretManager.getAiBaseUrl().ifBlank { AppConfig.buildAiBaseUrl }
        return apiKey.isNotBlank() && baseUrl.isNotBlank()
    }

    fun getEffectiveBaseUrl(): String {
        return secretManager.getAiBaseUrl().ifBlank {
            AppConfig.buildAiBaseUrl.ifBlank { "https://api.groq.com/openai/v1" }
        }
    }

    fun getEffectiveModel(): String {
        return secretManager.getAiModel().ifBlank {
            AppConfig.buildAiModel.ifBlank { "llama-3.3-70b-versatile" }
        }
    }

    suspend fun summarize(
        subject: String,
        body: String,
        language: SummaryLanguage,
        mailId: String = "temp_${subject.hashCode()}"
    ): AiSummaryResponse? {
        val res = summarizeWithAi(mailId, subject, body, language)
        return res.getOrNull()
    }

    suspend fun summarizeWithAi(
        mailId: String,
        subject: String,
        bodyText: String,
        language: SummaryLanguage
    ): Result<AiSummaryResponse> {
        val cacheKey = "${mailId}_${language.code}"
        memoryCache[cacheKey]?.let { return Result.success(it) }

        if (!isConfigured()) {
            return Result.failure(IllegalStateException("AI provider not configured"))
        }

        // Rate limit: space calls at least 2 seconds apart
        val now = System.currentTimeMillis()
        val elapsed = now - lastCallTimeMs
        if (elapsed < 2000) {
            delay(2000 - elapsed)
        }
        lastCallTimeMs = System.currentTimeMillis()

        val apiKey = secretManager.getAiApiKey().ifBlank { AppConfig.buildAiApiKey }
        val baseUrl = getEffectiveBaseUrl()
        val model = getEffectiveModel()

        val langName = when (language) {
            SummaryLanguage.ENGLISH -> "simple English"
            SummaryLanguage.ROMAN_URDU -> "Roman Urdu (easy spoken Urdu in English script)"
            SummaryLanguage.URDU -> "Urdu (اردو script)"
        }

        val systemPrompt = "You explain university emails to students in $langName using very simple words. Never invent facts. Reply with JSON only."

        val sanitizedBody = Normalizer.cleanForAi(bodyText)
        val userPrompt = """
            Mail Subject: $subject
            Mail Content:
            $sanitizedBody

            Return a valid JSON object matching this schema:
            {
              "headline": "Short title, max 10 words",
              "bullets": ["Bullet 1 max 16 words", "Bullet 2 max 16 words", "Bullet 3 max 16 words"],
              "category": "one of: CLASS_CHANGE, EXAM, DEADLINE, FEE, REGISTRATION, RESULT, ATTENDANCE, CAREER, EVENT, GENERAL",
              "when": "date/time or null",
              "place": "venue or null",
              "courses": ["Course codes like FIN201"],
              "link": "url or null",
              "action": "one short actionable sentence"
            }
        """.trimIndent()

        val callResult = client.chatCompletion(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt
        )

        return callResult.mapCatching { responseText ->
            val parsed = parseResponse(responseText)
            memoryCache[cacheKey] = parsed
            parsed
        }
    }

    private fun parseResponse(raw: String): AiSummaryResponse {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }

        val obj = json.parseToJsonElement(clean).jsonObject
        val headline = obj["headline"]?.jsonPrimitive?.content ?: "Email Summary"
        val bullets = obj["bullets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val category = obj["category"]?.jsonPrimitive?.content ?: "GENERAL"
        val whenText = obj["when"]?.jsonPrimitive?.content
        val place = obj["place"]?.jsonPrimitive?.content
        val courses = obj["courses"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val link = obj["link"]?.jsonPrimitive?.content
        val action = obj["action"]?.jsonPrimitive?.content ?: "Check mail details"

        return AiSummaryResponse(
            headline = headline,
            bullets = bullets,
            categoryId = category,
            whenText = whenText,
            place = place,
            courses = courses,
            link = link,
            action = action
        )
    }
}
