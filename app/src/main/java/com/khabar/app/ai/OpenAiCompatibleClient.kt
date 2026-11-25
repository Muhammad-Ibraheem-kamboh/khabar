package com.khabar.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String,
    val content: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = "image/jpeg"
)

class OpenAiCompatibleClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        fun isRecoverableModelError(errorMsg: String?): Boolean {
            if (errorMsg.isNullOrBlank()) return false
            val lower = errorMsg.lowercase()
            return lower.contains("404") ||
                    lower.contains("410") ||
                    lower.contains("503") ||
                    lower.contains("429") ||
                    lower.contains("model_not_found") ||
                    lower.contains("not found") ||
                    lower.contains("does not exist") ||
                    lower.contains("not supported") ||
                    lower.contains("deprecated") ||
                    lower.contains("retired") ||
                    lower.contains("unavailable") ||
                    lower.contains("overloaded") ||
                    lower.contains("resource_exhausted") ||
                    lower.contains("resource exhausted") ||
                    lower.contains("rate limit") ||
                    lower.contains("quota")
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        val messages = mutableListOf<ChatMessage>()
        if (systemPrompt.isNotBlank()) {
            messages.add(ChatMessage(role = "system", content = systemPrompt))
        }
        messages.add(ChatMessage(role = "user", content = userPrompt))
        return multiTurnChatCompletion(baseUrl, apiKey, model, messages)
    }

    suspend fun multiTurnChatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBaseUrl = baseUrl.trim().removeSuffix("/")

            // 1. Direct Anthropic Claude API support
            if (cleanBaseUrl.contains("anthropic.com") || apiKey.startsWith("sk-ant-")) {
                return@withContext callAnthropicMessages(cleanBaseUrl, apiKey, model, messages)
            }

            // 2. Standard OpenAI compatible (Gemini, Groq, OpenRouter, Ollama)
            val effectiveBaseUrl = if (cleanBaseUrl.contains("generativelanguage.googleapis.com") && !cleanBaseUrl.contains("/openai")) {
                "https://generativelanguage.googleapis.com/v1beta/openai"
            } else {
                cleanBaseUrl
            }

            val url = if (effectiveBaseUrl.endsWith("/chat/completions")) {
                effectiveBaseUrl
            } else {
                "$effectiveBaseUrl/chat/completions"
            }

            val jsonMessages = JsonArray(
                messages.map { msg ->
                    if (msg.imageBase64.isNullOrBlank()) {
                        JsonObject(
                            mapOf(
                                "role" to JsonPrimitive(msg.role),
                                "content" to JsonPrimitive(msg.content)
                            )
                        )
                    } else {
                        val contentParts = JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("text"),
                                        "text" to JsonPrimitive(msg.content.ifBlank { "Describe this image in detail" })
                                    )
                                ),
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("image_url"),
                                        "image_url" to JsonObject(
                                            mapOf(
                                                "url" to JsonPrimitive("data:${msg.imageMimeType ?: "image/jpeg"};base64,${msg.imageBase64}")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                        JsonObject(
                            mapOf(
                                "role" to JsonPrimitive(msg.role),
                                "content" to contentParts
                            )
                        )
                    }
                }
            )

            val payload = JsonObject(
                mapOf(
                    "model" to JsonPrimitive(model),
                    "messages" to jsonMessages,
                    "temperature" to JsonPrimitive(0.3)
                )
            )

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }

                val jsonResponse = json.parseToJsonElement(body).jsonObject
                val choices = jsonResponse["choices"]?.jsonArray
                val firstChoice = choices?.firstOrNull()?.jsonObject
                val message = firstChoice?.get("message")?.jsonObject
                val content = message?.get("content")?.jsonPrimitive?.content ?: ""

                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callAnthropicMessages(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): Result<String> {
        return try {
            val url = if (baseUrl.endsWith("/messages")) baseUrl else "$baseUrl/messages"

            val systemMessage = messages.firstOrNull { it.role == "system" }?.content ?: ""
            val nonSystemMessages = messages.filter { it.role != "system" }

            val jsonMessages = JsonArray(
                nonSystemMessages.map { msg ->
                    if (msg.imageBase64.isNullOrBlank()) {
                        JsonObject(
                            mapOf(
                                "role" to JsonPrimitive(if (msg.role == "assistant") "assistant" else "user"),
                                "content" to JsonPrimitive(msg.content)
                            )
                        )
                    } else {
                        val contentParts = JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("text"),
                                        "text" to JsonPrimitive(msg.content.ifBlank { "Describe this image in detail" })
                                    )
                                ),
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("image"),
                                        "source" to JsonObject(
                                            mapOf(
                                                "type" to JsonPrimitive("base64"),
                                                "media_type" to JsonPrimitive(msg.imageMimeType ?: "image/jpeg"),
                                                "data" to JsonPrimitive(msg.imageBase64)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                        JsonObject(
                            mapOf(
                                "role" to JsonPrimitive(if (msg.role == "assistant") "assistant" else "user"),
                                "content" to contentParts
                            )
                        )
                    }
                }
            )

            val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>(
                "model" to JsonPrimitive(model),
                "max_tokens" to JsonPrimitive(4096),
                "messages" to jsonMessages
            )
            if (systemMessage.isNotBlank()) {
                map["system"] = JsonPrimitive(systemMessage)
            }

            val payload = JsonObject(map)
            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Anthropic HTTP ${response.code}: $body"))
                }

                val jsonResponse = json.parseToJsonElement(body).jsonObject
                val contentArray = jsonResponse["content"]?.jsonArray
                val firstContent = contentArray?.firstOrNull()?.jsonObject
                val text = firstContent?.get("text")?.jsonPrimitive?.content ?: ""

                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class AutoDetectResult(
        val success: Boolean,
        val workingModel: String?,
        val message: String
    )

    fun getCandidateModelsForProvider(provider: String): List<String> = when (provider.lowercase().trim()) {
        "gemini" -> listOf(
            "gemini-flash-lite-latest",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-3.6-flash"
        )
        "groq" -> listOf(
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant"
        )
        "openai" -> listOf(
            "gpt-4o-mini",
            "gpt-4o"
        )
        "openrouter" -> listOf(
            "meta-llama/llama-3.3-70b-instruct",
            "google/gemini-2.0-flash-001",
            "deepseek/deepseek-r1:free"
        )
        "claude" -> listOf(
            "claude-3-5-haiku-20241022",
            "claude-3-5-sonnet-20241022"
        )
        else -> listOf("llama3.2")
    }

    suspend fun autoDetectAndTestWorkingModel(
        provider: String,
        baseUrl: String,
        apiKey: String
    ): AutoDetectResult {
        val candidates = getCandidateModelsForProvider(provider)
        var lastErr = "No models responded"
        for (m in candidates) {
            val res = testConnectionDetailed(baseUrl, m, apiKey)
            if (res.isSuccess) {
                return AutoDetectResult(
                    success = true,
                    workingModel = m,
                    message = "Connected cleanly! Model auto-selected: $m"
                )
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error"
                lastErr = err
                // If it was 404 or 503, immediately try the next model candidate!
                if (!err.contains("404") && !err.contains("503") && !err.contains("model_not_found")) {
                    // If it's a fatal key error (like 401 unauthorized or 429 quota exhausted), don't waste time retrying
                    if (err.contains("401") || err.contains("invalid_api_key") || err.contains("429") || err.contains("insufficient_quota")) {
                        break
                    }
                }
            }
        }
        return AutoDetectResult(
            success = false,
            workingModel = null,
            message = lastErr
        )
    }

    suspend fun testConnection(baseUrl: String, model: String, apiKey: String): Boolean {
        val result = chatCompletion(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            systemPrompt = "You are a test assistant.",
            userPrompt = "Say ok"
        )
        return result.isSuccess
    }

    suspend fun testConnectionDetailed(baseUrl: String, model: String, apiKey: String): Result<String> {
        return chatCompletion(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            systemPrompt = "You are a test assistant.",
            userPrompt = "Say ok"
        )
    }
}
