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

data class ChatMessage(val role: String, val content: String)

class OpenAiCompatibleClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
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
            val url = if (cleanBaseUrl.endsWith("/chat/completions")) {
                cleanBaseUrl
            } else {
                "$cleanBaseUrl/chat/completions"
            }

            val jsonMessages = JsonArray(
                messages.map { msg ->
                    JsonObject(
                        mapOf(
                            "role" to JsonPrimitive(msg.role),
                            "content" to JsonPrimitive(msg.content)
                        )
                    )
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
                    JsonObject(
                        mapOf(
                            "role" to JsonPrimitive(if (msg.role == "assistant") "assistant" else "user"),
                            "content" to JsonPrimitive(msg.content)
                        )
                    )
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
