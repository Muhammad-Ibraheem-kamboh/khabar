package com.khabar.app.sources

import android.content.Context
import com.khabar.app.config.AppConfig
import com.khabar.app.domain.model.RawMail
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

class OutlookMailSource(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient()
) : MailSource {

    override val id: String = "outlook_source"
    override val displayName: String = "Outlook (Microsoft 365)"

    override val isConfigured: Boolean
        get() = AppConfig.buildMsClientId.isNotBlank() && AppConfig.buildMsSignatureHash.isNotBlank()

    override var isConnected: Boolean = false
        private set

    private var pca: ISingleAccountPublicClientApplication? = null
    private var accessToken: String? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun initializeMsal() {
        if (!isConfigured) return
        try {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                com.khabar.app.R.raw.auth_config_single_account,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        pca = application
                    }
                    override fun onError(exception: com.microsoft.identity.client.exception.MsalException?) {
                        exception?.printStackTrace()
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun fetchNew(): List<RawMail> = withContext(Dispatchers.IO) {
        if (!isConfigured || accessToken == null) return@withContext emptyList()

        try {
            // Delta query / list with $select and top=50
            val url = "https://graph.microsoft.com/v1.0/me/mailFolders/inbox/messages?\$select=id,subject,from,receivedDateTime,bodyPreview,body,hasAttachments,isRead&\$top=50"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()

                val rootObj = json.parseToJsonElement(bodyStr).jsonObject
                val values = rootObj["value"]?.jsonArray ?: return@withContext emptyList()

                values.mapNotNull { item ->
                    try {
                        val obj = item.jsonObject
                        val mailId = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val subject = obj["subject"]?.jsonPrimitive?.content ?: ""
                        val fromObj = obj["from"]?.jsonObject?.get("emailAddress")?.jsonObject
                        val senderName = fromObj?.get("name")?.jsonPrimitive?.content ?: "Outlook Sender"
                        val senderEmail = fromObj?.get("address")?.jsonPrimitive?.content
                        val receivedDateTimeStr = obj["receivedDateTime"]?.jsonPrimitive?.content
                        val receivedAt = if (receivedDateTimeStr != null) {
                            Instant.parse(receivedDateTimeStr).toEpochMilli()
                        } else System.currentTimeMillis()

                        val bodyPreview = obj["bodyPreview"]?.jsonPrimitive?.content ?: ""
                        val bodyContent = obj["body"]?.jsonObject?.get("content")?.jsonPrimitive?.content ?: bodyPreview
                        val hasAttachments = obj["hasAttachments"]?.jsonPrimitive?.booleanOrNull ?: false

                        RawMail(
                            id = "ms_$mailId",
                            sourceId = id,
                            sender = senderName,
                            senderEmail = senderEmail,
                            subject = subject,
                            bodyText = bodyPreview.ifBlank { bodyContent },
                            receivedAt = receivedAt,
                            isPreviewOnly = false,
                            hasAttachments = hasAttachments
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
