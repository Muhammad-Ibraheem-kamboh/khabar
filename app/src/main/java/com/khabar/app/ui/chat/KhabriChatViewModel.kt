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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class KhabriChatViewModel(
    private val chatRepository: ChatRepository,
    val voiceAssistantManager: VoiceAssistantManager,
    private val secretManager: KeystoreSecretManager,
    private val openAiClient: OpenAiCompatibleClient,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _currentSessionId = MutableStateFlow("default")
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

    // Attached Image & OCR
    private val _attachedBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedBitmap: StateFlow<Bitmap?> = _attachedBitmap.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    private val _attachedOcrText = MutableStateFlow<String?>(null)
    val attachedOcrText: StateFlow<String?> = _attachedOcrText.asStateFlow()

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
    }

    fun toggleHistoryDrawer() {
        _showHistoryDrawer.value = !_showHistoryDrawer.value
    }

    fun dismissHistoryDrawer() {
        _showHistoryDrawer.value = false
    }

    fun startNewChat() {
        _currentSessionId.value = "chat_" + UUID.randomUUID().toString().take(8)
        _inputText.value = ""
        _statusMessage.value = null
        removeAttachedImage()
        _showHistoryDrawer.value = false
    }

    fun switchSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _statusMessage.value = null
        removeAttachedImage()
        _showHistoryDrawer.value = false
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
        viewModelScope.launch {
            try {
                _attachedImageUri.value = uri.toString()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    _attachedBitmap.value = bitmap
                    if (bitmap != null) {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        textRecognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                _attachedOcrText.value = visionText.text
                            }
                            .addOnFailureListener {
                                _attachedOcrText.value = null
                            }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeAttachedImage() {
        _attachedBitmap.value = null
        _attachedImageUri.value = null
        _attachedOcrText.value = null
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

    fun sendMessage(customText: String? = null) {
        val textToSend = (customText ?: _inputText.value).trim()
        val imageUri = _attachedImageUri.value
        val ocrText = _attachedOcrText.value

        if (textToSend.isBlank() && ocrText.isNullOrBlank()) return
        if (_isGenerating.value) return

        if (!hasAnyApiKey() && !_activeProvider.value.equals(KeystoreSecretManager.PROVIDER_OLLAMA, ignoreCase = true)) {
            openKeyDialog(_activeProvider.value)
            return
        }

        _inputText.value = ""
        removeAttachedImage()
        _statusMessage.value = null

        viewModelScope.launch {
            _isGenerating.value = true
            val result = chatRepository.sendMessage(
                userQuery = textToSend,
                sessionId = _currentSessionId.value,
                imageUri = imageUri,
                ocrExtractedText = ocrText,
                explicitLanguage = _chatLanguage.value
            )
            _isGenerating.value = false

            result.onSuccess { reply ->
                if (_autoVoiceEnabled.value && reply.isNotBlank()) {
                    voiceAssistantManager.speak(reply, _chatLanguage.value)
                }
            }.onFailure { err ->
                _statusMessage.value = err.message
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

    override fun onCleared() {
        super.onCleared()
        voiceAssistantManager.stopSpeaking()
        voiceAssistantManager.stopListening()
    }
}
