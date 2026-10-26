package com.khabar.app.sources

import android.content.Context
import com.khabar.app.config.AppConfig
import com.khabar.app.domain.model.RawMail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class GmailMailSource(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient()
) : MailSource {

    override val id: String = "gmail_source"
    override val displayName: String = "Gmail"

    override val isConfigured: Boolean
        get() = AppConfig.buildGmailEnabled

    override var isConnected: Boolean = false
        private set

    private var accessToken: String? = null
    private var lastHistoryId: String? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun setAccessToken(token: String) {
        accessToken = token
        isConnected = token.isNotBlank()
    }

    fun getSignInIntent(): android.content.Intent {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/gmail.readonly"))
            .apply {
                if (AppConfig.GMAIL_CLIENT_ID.isNotBlank()) {
                    requestIdToken(AppConfig.GMAIL_CLIENT_ID)
                }
            }
            .build()
        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    suspend fun acquireTokenFromAccount(account: android.accounts.Account): String? = withContext(Dispatchers.IO) {
        try {
            val scope = "oauth2:https://www.googleapis.com/auth/gmail.readonly"
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account, scope)
            setAccessToken(token)
            token
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun fetchNew(): List<RawMail> = withContext(Dispatchers.IO) {
        if (!isConfigured || accessToken == null) return@withContext emptyList()

        try {
            // If historyId is stored, use users.history.list
            val listUrl = if (lastHistoryId != null) {
                "https://gmail.googleapis.com/gmail/v1/users/me/history?startHistoryId=$lastHistoryId&historyTypes=messageAdded"
            } else {
                "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=20"
            }

            val request = Request.Builder()
                .url(listUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val root = json.parseToJsonElement(bodyStr).jsonObject

                val newHistoryId = root["historyId"]?.jsonPrimitive?.content
                if (newHistoryId != null) lastHistoryId = newHistoryId

                val messageIds = mutableListOf<String>()
                root["messages"]?.jsonArray?.forEach {
                    it.jsonObject["id"]?.jsonPrimitive?.content?.let { id -> messageIds.add(id) }
                }

                // Fetch new message details
                messageIds.mapNotNull { msgId ->
                    fetchMessageDetails(msgId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun fetchMessageDetails(msgId: String): RawMail? {
        val token = accessToken ?: return null
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$msgId?format=full"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val root = json.parseToJsonElement(bodyStr).jsonObject

                val snippet = root["snippet"]?.jsonPrimitive?.content ?: ""
                val internalDate = root["internalDate"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis()

                val headers = root["payload"]?.jsonObject?.get("headers")?.jsonArray
                var subject = ""
                var from = ""
                headers?.forEach { headerElem ->
                    val name = headerElem.jsonObject["name"]?.jsonPrimitive?.content
                    val value = headerElem.jsonObject["value"]?.jsonPrimitive?.content ?: ""
                    if (name.equals("Subject", ignoreCase = true)) subject = value
                    if (name.equals("From", ignoreCase = true)) from = value
                }

                RawMail(
                    id = "gmail_$msgId",
                    sourceId = id,
                    sender = from.ifBlank { "Gmail Sender" },
                    senderEmail = from,
                    subject = subject.ifBlank { "No Subject" },
                    bodyText = snippet,
                    receivedAt = internalDate,
                    isPreviewOnly = false,
                    hasAttachments = false
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
