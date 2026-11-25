package com.khabar.app.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.ai.OpenAiCompatibleClient
import com.khabar.app.data.local.UserPreferences
import com.khabar.app.data.local.dao.ChatSessionInfo
import com.khabar.app.data.local.entities.ChatMessageEntity
import com.khabar.app.data.repository.ChatRepository
import com.khabar.app.util.VoiceAssistantManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.util.Base64
import java.util.UUID

class KhabriChatViewModel(
    private val chatRepository: ChatRepository,
    val voiceAssistantManager: VoiceAssistantManager,
    private val secretManager: KeystoreSecretManager,
    private val openAiClient: OpenAiCompatibleClient,
    private val userPreferences: UserPreferences,
    private val aiDefectManager: com.khabar.app.ai.AiDefectManager? = null
) : ViewModel() {

    // Fresh chat session by default (like ChatGPT)
    private val _currentSessionId = MutableStateFlow("chat_" + UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { sid ->
        chatRepository.getMessagesForSessionFlow(sid)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val sessions: StateFlow<List<ChatSessionInfo>> = chatRepository.sessionsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _autoVoiceEnabled = MutableStateFlow(false)
    val autoVoiceEnabled: StateFlow<Boolean> = _autoVoiceEnabled.asStateFlow()

    private val initialProvider = secretManager.getAiProvider().ifBlank { KeystoreSecretManager.PROVIDER_GEMINI }
    private val _activeProvider = MutableStateFlow(initialProvider)
    val activeProvider: StateFlow<String> = _activeProvider.asStateFlow()

    private val _activeModel = MutableStateFlow(secretManager.getModelForProvider(initialProvider))
    val activeModel: StateFlow<String> = _activeModel.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastFailedCanRetry = MutableStateFlow(false)
    val lastFailedCanRetry: StateFlow<Boolean> = _lastFailedCanRetry.asStateFlow()

    private data class PendingRetryPayload(
        val query: String,
        val imageUri: String?,
        val imageBase64: String?,
        val ocrText: String?
    )
    private var lastFailedPayload: PendingRetryPayload? = null
    private var activeGenerationJob: Job? = null

    // Attached Image & OCR
    private val _attachedBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedBitmap: StateFlow<Bitmap?> = _attachedBitmap.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    private val _attachedOcrText = MutableStateFlow<String?>(null)
    val attachedOcrText: StateFlow<String?> = _attachedOcrText.asStateFlow()

    private val _attachedIsPdf = MutableStateFlow(false)
    val attachedIsPdf: StateFlow<Boolean> = _attachedIsPdf.asStateFlow()

    private val _isAttachingDocument = MutableStateFlow(false)
    val isAttachingDocument: StateFlow<Boolean> = _isAttachingDocument.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Language Preference: "en", "ur", "roman_urdu"
    private val _chatLanguage = MutableStateFlow("en")
    val chatLanguage: StateFlow<String> = _chatLanguage.asStateFlow()

    // Dialog state for setting up API keys
    private val _showKeyDialog = MutableStateFlow(false)
    val showKeyDialog: StateFlow<Boolean> = _showKeyDialog.asStateFlow()

    private val _keyDialogProvider = MutableStateFlow(initialProvider)
    val keyDialogProvider: StateFlow<String> = _keyDialogProvider.asStateFlow()

    private val _showHistoryDrawer = MutableStateFlow(false)
    val showHistoryDrawer: StateFlow<Boolean> = _showHistoryDrawer.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.summaryLanguageFlow.collect { lang ->
                _chatLanguage.value = lang
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun setChatLanguage(lang: String) {
        _chatLanguage.value = lang
        viewModelScope.launch {
            userPreferences.setSummaryLanguage(lang)
        }
    }

    fun hasAnyApiKey(): Boolean = secretManager.hasAnyApiKey()

    fun hasKeyForProvider(provider: String): Boolean = secretManager.hasApiKeyForProvider(provider)

    fun getKeyForProvider(provider: String): String = secretManager.getApiKeyForProvider(provider)

    fun getBaseUrlForProvider(provider: String): String = secretManager.getBaseUrlForProvider(provider)

    fun getModelForProvider(provider: String): String = secretManager.getModelForProvider(provider)

    fun openKeyDialog(provider: String = _activeProvider.value) {
        _keyDialogProvider.value = provider
        _showKeyDialog.value = true
    }

    fun dismissKeyDialog() {
        _showKeyDialog.value = false
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
        _lastFailedCanRetry.value = false
    }

    fun toggleHistoryDrawer() {
        _showHistoryDrawer.value = !_showHistoryDrawer.value
    }

    fun dismissHistoryDrawer() {
        _showHistoryDrawer.value = false
    }

    fun startNewChat() {
        stopGeneration()
        _currentSessionId.value = "chat_" + UUID.randomUUID().toString()
        _inputText.value = ""
        _statusMessage.value = null
        _lastFailedCanRetry.value = false
        lastFailedPayload = null
        removeAttachedImage()
        _showHistoryDrawer.value = false
    }

    fun startNewChatIfNotEmpty() {
        if (messages.value.isNotEmpty()) {
            startNewChat()
        }
    }

    fun switchSession(sessionId: String) {
        stopGeneration()
        _currentSessionId.value = sessionId
        _statusMessage.value = null
        _lastFailedCanRetry.value = false
        lastFailedPayload = null
        removeAttachedImage()
        _showHistoryDrawer.value = false
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val cleaned = newTitle.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, cleaned)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.clearSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewChat()
            }
        }
    }

    fun attachImage(context: Context, uri: Uri) {
        attachDocumentOrImage(context, uri)
    }

    fun attachDocumentOrImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isAttachingDocument.value = true
            _statusMessage.value = null
            try {
                val result = com.khabar.app.util.DocumentScannerHelper.processDocumentUri(context, uri)
                _attachedIsPdf.value = result.isPdf
                // Only store displayable imageUri when not a PDF so ChatBubble never renders a broken Coil image
                _attachedImageUri.value = if (result.isPdf) null else uri.toString()
                _attachedBitmap.value = result.previewBitmap
                _attachedOcrText.value = if (result.rawText.isNotBlank()) {
                    "[Document Content: ${result.detectedTitle}]\n${result.rawText}"
                } else null
            } catch (e: Exception) {
                _statusMessage.value = "Could not load attachment: ${e.message ?: "Unsupported file"}"
                removeAttachedImage()
            } finally {
                _isAttachingDocument.value = false
            }
        }
    }

    fun removeAttachedImage() {
        _attachedBitmap.value = null
        _attachedImageUri.value = null
        _attachedOcrText.value = null
        _attachedIsPdf.value = false
    }

    fun attachPortalPageContext(title: String, extractedText: String) {
        _attachedOcrText.value = "[PORTAL / LMS SYNCED CONTENT: $title]\n$extractedText"
        _inputText.value = "Analyze portal page: summarize active announcements, deadlines, and schedule."
    }

    suspend fun testProviderConfig(
        provider: String,
        apiKey: String,
        baseUrl: String,
        model: String
    ): Result<String> {
        return openAiClient.testConnectionDetailed(
            baseUrl = baseUrl,
            model = model,
            apiKey = apiKey
        )
    }

    fun saveProviderConfig(
        provider: String,
        apiKey: String,
        customBaseUrl: String? = null,
        customModel: String? = null
    ) {
        val canonical = KeystoreSecretManager.canonicalProvider(provider)
        secretManager.saveApiKeyForProvider(canonical, apiKey)

        val url = if (!customBaseUrl.isNullOrBlank()) customBaseUrl else KeystoreSecretManager.getDefaultBaseUrl(canonical)
        val model = if (!customModel.isNullOrBlank()) customModel else KeystoreSecretManager.getDefaultModel(canonical)

        secretManager.saveAiSettings(canonical, url, model)
        _activeProvider.value = canonical
        _activeModel.value = model
        _showKeyDialog.value = false
        _statusMessage.value = null
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _isGenerating.value = false
    }

    fun retryLastMessage() {
        val pending = lastFailedPayload ?: return
        if (_isGenerating.value) return

        _statusMessage.value = null
        _lastFailedCanRetry.value = false

        activeGenerationJob = viewModelScope.launch {
            _isGenerating.value = true
            try {
                val result = chatRepository.sendMessage(
                    userQuery = pending.query,
                    sessionId = _currentSessionId.value,
                    imageUri = pending.imageUri,
                    imageBase64 = pending.imageBase64,
                    ocrExtractedText = pending.ocrText,
                    explicitLanguage = _chatLanguage.value,
                    skipUserInsert = true
                )
                result.onSuccess { reply ->
                    lastFailedPayload = null
                    _statusMessage.value = null
                    _lastFailedCanRetry.value = false
                    syncActiveModelFromSecretManager()
                    if (_autoVoiceEnabled.value && reply.isNotBlank()) {
                        voiceAssistantManager.speak(reply, _chatLanguage.value)
                    }
                }.onFailure { err ->
                    _lastFailedCanRetry.value = true
                    _statusMessage.value = err.message ?: "Request failed. Tap Retry."
                }
            } catch (_: CancellationException) {
                // User stopped generation cleanly
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun regenerateLastResponse() {
        if (_isGenerating.value) return
        _statusMessage.value = null
        _lastFailedCanRetry.value = false

        activeGenerationJob = viewModelScope.launch {
            _isGenerating.value = true
            try {
                val result = chatRepository.regenerateLastResponse(
                    sessionId = _currentSessionId.value,
                    explicitLanguage = _chatLanguage.value
                )
                result.onSuccess { reply ->
                    syncActiveModelFromSecretManager()
                    if (_autoVoiceEnabled.value && reply.isNotBlank()) {
                        voiceAssistantManager.speak(reply, _chatLanguage.value)
                    }
                }.onFailure { err ->
                    _lastFailedCanRetry.value = true
                    _statusMessage.value = err.message ?: "Could not regenerate response."
                }
            } catch (_: CancellationException) {
                // Cancelled cleanly
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun syncActiveModelFromSecretManager() {
        val currentProv = secretManager.getAiProvider()
        _activeProvider.value = currentProv
        if (currentProv != KeystoreSecretManager.PROVIDER_AUTO) {
            _activeModel.value = secretManager.getModelForProvider(currentProv)
        }
    }

    fun sendMessage(customText: String? = null) {
        val textToSend = (customText ?: _inputText.value).trim()
        val imageUri = _attachedImageUri.value
        val ocrText = _attachedOcrText.value
        val bitmap = _attachedBitmap.value
        val isPdf = _attachedIsPdf.value

        if (textToSend.isBlank() && ocrText.isNullOrBlank() && bitmap == null) return
        if (_isGenerating.value) return

        if (!hasAnyApiKey() && !_activeProvider.value.equals(KeystoreSecretManager.PROVIDER_OLLAMA, ignoreCase = true)) {
            openKeyDialog(_activeProvider.value)
            return
        }

        _inputText.value = ""
        removeAttachedImage()
        _statusMessage.value = null
        _lastFailedCanRetry.value = false

        activeGenerationJob = viewModelScope.launch {
            _isGenerating.value = true
            try {
                // Encode image off the main UI thread to prevent jank on high-res photos
                val imageBase64 = if (bitmap != null && !isPdf) {
                    withContext(Dispatchers.Default) {
                        try {
                            encodeBitmapToBase64(bitmap, maxDim = 1280, quality = 85)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                } else null

                val effectiveQuery = if (textToSend.isBlank() && isPdf) {
                    "Summarize key details, dates, amounts, and actions from this PDF document."
                } else {
                    textToSend
                }

                lastFailedPayload = PendingRetryPayload(
                    query = effectiveQuery,
                    imageUri = imageUri,
                    imageBase64 = imageBase64,
                    ocrText = ocrText
                )

                val result = chatRepository.sendMessage(
                    userQuery = effectiveQuery,
                    sessionId = _currentSessionId.value,
                    imageUri = imageUri,
                    imageBase64 = imageBase64,
                    ocrExtractedText = ocrText,
                    explicitLanguage = _chatLanguage.value
                )

                result.onSuccess { reply ->
                    lastFailedPayload = null
                    _statusMessage.value = null
                    _lastFailedCanRetry.value = false
                    syncActiveModelFromSecretManager()
                    if (_autoVoiceEnabled.value && reply.isNotBlank()) {
                        voiceAssistantManager.speak(reply, _chatLanguage.value)
                    }
                }.onFailure { err ->
                    _lastFailedCanRetry.value = true
                    _statusMessage.value = err.message ?: "Request failed. Tap Retry."
                }
            } catch (_: CancellationException) {
                // Cancelled cleanly by user tapping Stop
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun startVoiceInput() {
        voiceAssistantManager.startListening(
            onResult = { recognizedText ->
                _inputText.value = recognizedText
                if (recognizedText.isNotBlank()) {
                    sendMessage(recognizedText)
                }
            },
            onError = { errMsg ->
                _statusMessage.value = errMsg
            }
        )
    }

    fun stopVoiceInput() {
        voiceAssistantManager.stopListening()
    }

    fun toggleAutoVoice() {
        _autoVoiceEnabled.value = !_autoVoiceEnabled.value
        if (!_autoVoiceEnabled.value) {
            voiceAssistantManager.stopSpeaking()
        }
    }

    fun stopSpeaking() {
        voiceAssistantManager.stopSpeaking()
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearSession(_currentSessionId.value)
            voiceAssistantManager.stopSpeaking()
        }
    }

    fun switchAiModel(provider: String, baseUrl: String? = null, model: String? = null) {
        val canonical = KeystoreSecretManager.canonicalProvider(provider)
        val targetUrl = baseUrl ?: KeystoreSecretManager.getDefaultBaseUrl(canonical)
        val targetModel = model ?: KeystoreSecretManager.getDefaultModel(canonical)

        _activeProvider.value = canonical
        _activeModel.value = targetModel
        secretManager.saveAiSettings(canonical, targetUrl, targetModel)

        if (canonical != KeystoreSecretManager.PROVIDER_AUTO &&
            !secretManager.hasApiKeyForProvider(canonical) &&
            !canonical.equals(KeystoreSecretManager.PROVIDER_OLLAMA, ignoreCase = true)
        ) {
            openKeyDialog(canonical)
        }
    }

    fun reportMessageMistake(message: ChatMessageEntity, reason: String) {
        viewModelScope.launch {
            val history = messages.value
            val msgIndex = history.indexOfFirst { it.id == message.id }
            val userQuery = if (msgIndex > 0) {
                history.take(msgIndex).lastOrNull { it.role == "user" }?.content ?: ""
            } else ""

            aiDefectManager?.recordUserCorrection(
                userPrompt = userQuery.ifBlank { "User query" },
                aiPreviousResponse = message.content,
                userCritique = reason,
                provider = _activeProvider.value,
                model = _activeModel.value
            )
            _statusMessage.value = "Mistake logged to AI Quality Vault! Khabri will adapt."
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistantManager.stopSpeaking()
        voiceAssistantManager.stopListening()
    }

    companion object {
        fun encodeBytesToBase64(bytes: ByteArray): String {
            return try {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (_: Throwable) {
                java.util.Base64.getEncoder().encodeToString(bytes)
            }
        }

        fun encodeBitmapToBase64(bitmap: Bitmap, maxDim: Int = 1280, quality: Int = 85): String {
            val width = bitmap.width
            val height = bitmap.height
            val scaledBitmap = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val targetWidth: Int
                val targetHeight: Int
                if (ratio > 1f) {
                    targetWidth = maxDim
                    targetHeight = (maxDim / ratio).toInt().coerceAtLeast(1)
                } else {
                    targetHeight = maxDim
                    targetWidth = (maxDim * ratio).toInt().coerceAtLeast(1)
                }
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            return encodeBytesToBase64(outputStream.toByteArray())
        }
    }
}
