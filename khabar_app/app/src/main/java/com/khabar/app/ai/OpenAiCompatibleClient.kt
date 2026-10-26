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
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
            val url = if (cleanBaseUrl.endsWith("/chat/completions")) {
                cleanBaseUrl
            } else {
                "$cleanBaseUrl/chat/completions"
            }

            val messages = JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "role" to JsonPrimitive("system"),
                            "content" to JsonPrimitive(systemPrompt)
                        )
                    ),
                    JsonObject(
                        mapOf(
                            "role" to JsonPrimitive("user"),
                            "content" to JsonPrimitive(userPrompt)
                        )
                    )
                )
            )

            val payload = JsonObject(
                mapOf(
                    "model" to JsonPrimitive(model),
                    "messages" to messages,
                    "temperature" to JsonPrimitive(0.1)
                )
            )

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
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
}
