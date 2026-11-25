package com.khabar.app.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Modern Android Keystore AES-GCM wrapper for storing sensitive API keys per provider.
 * Does not use any deprecated androidx.security.crypto classes.
 */
class KeystoreSecretManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("khabar_secure_keys", Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private const val KEY_ALIAS = "KhabarAiKeyAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        // Canonical provider identifiers
        const val PROVIDER_AUTO = "Auto"
        const val PROVIDER_GEMINI = "Gemini"
        const val PROVIDER_GROQ = "Groq"
        const val PROVIDER_OPENAI = "OpenAI"
        const val PROVIDER_OPENROUTER = "OpenRouter"
        const val PROVIDER_CLAUDE = "Claude"
        const val PROVIDER_OLLAMA = "Ollama"

        private const val PREF_AI_KEY = "encrypted_ai_api_key"
        private const val PREF_AI_IV = "encrypted_ai_api_iv"
        private const val PREF_AI_BASE_URL = "ai_base_url_custom"
        private const val PREF_AI_MODEL = "ai_model_custom"
        private const val PREF_AI_PROVIDER = "ai_provider_preset"

        fun normalizeProvider(provider: String): String = when {
            provider.contains("auto", ignoreCase = true) -> "auto"
            provider.contains("gemini", ignoreCase = true) -> "gemini"
            provider.contains("groq", ignoreCase = true) -> "groq"
            provider.contains("openai", ignoreCase = true) -> "openai"
            provider.contains("openrouter", ignoreCase = true) -> "openrouter"
            provider.contains("claude", ignoreCase = true) || provider.contains("anthropic", ignoreCase = true) -> "claude"
            provider.contains("ollama", ignoreCase = true) -> "ollama"
            else -> provider.lowercase().trim()
        }

        fun canonicalProvider(provider: String): String = when (normalizeProvider(provider)) {
            "auto" -> PROVIDER_AUTO
            "gemini" -> PROVIDER_GEMINI
            "groq" -> PROVIDER_GROQ
            "openai" -> PROVIDER_OPENAI
            "openrouter" -> PROVIDER_OPENROUTER
            "claude" -> PROVIDER_CLAUDE
            "ollama" -> PROVIDER_OLLAMA
            else -> provider
        }

        fun getProviderWebsiteUrl(provider: String): String = when (normalizeProvider(provider)) {
            "gemini" -> "https://aistudio.google.com/app/apikey"
            "groq" -> "https://console.groq.com/keys"
            "openai" -> "https://platform.openai.com/api-keys"
            "openrouter" -> "https://openrouter.ai/keys"
            "claude" -> "https://console.anthropic.com/settings/keys"
            "ollama" -> "https://ollama.com"
            else -> "https://aistudio.google.com/app/apikey"
        }

        fun getDefaultBaseUrl(provider: String): String = when (normalizeProvider(provider)) {
            "gemini" -> "https://generativelanguage.googleapis.com/v1beta/openai"
            "groq" -> "https://api.groq.com/openai/v1"
            "openai" -> "https://api.openai.com/v1"
            "openrouter" -> "https://openrouter.ai/api/v1"
            "claude" -> "https://api.anthropic.com/v1"
            "ollama" -> "http://10.0.2.2:11434/v1"
            else -> "https://api.groq.com/openai/v1"
        }

        fun getDefaultModel(provider: String): String = when (normalizeProvider(provider)) {
            "gemini" -> "gemini-flash-lite-latest"
            "groq" -> "openai/gpt-oss-120b"
            "openai" -> "gpt-4o-mini"
            "openrouter" -> "meta-llama/llama-3.3-70b-instruct"
            "claude" -> "claude-3-5-sonnet-20241022"
            "ollama" -> "llama3.2"
            else -> "openai/gpt-oss-120b"
        }

        fun getFallbackModels(provider: String): List<String> = when (normalizeProvider(provider)) {
            "gemini" -> listOf(
                "gemini-flash-lite-latest",
                "gemini-3.5-flash-lite",
                "gemini-3.5-flash",
                "gemini-2.5-flash"
            )
            "groq" -> listOf(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant"
            )
            "openai" -> listOf("gpt-4o-mini", "gpt-4o")
            "openrouter" -> listOf(
                "meta-llama/llama-3.3-70b-instruct",
                "google/gemini-2.0-flash-001",
                "deepseek/deepseek-r1:free"
            )
            else -> listOf("openai/gpt-oss-120b")
        }

        fun getVisionModel(provider: String): String = when (normalizeProvider(provider)) {
            "gemini" -> "gemini-flash-lite-latest"
            "groq" -> "meta-llama/llama-4-scout-17b-16e-instruct"
            "openai" -> "gpt-4o-mini"
            "openrouter" -> "google/gemini-2.0-flash-001"
            "claude" -> "claude-3-5-sonnet-20241022"
            else -> "gemini-flash-lite-latest"
        }

        fun isVisionCapableProvider(provider: String): Boolean = when (normalizeProvider(provider)) {
            "gemini", "openai", "claude", "groq", "openrouter" -> true
            else -> false
        }

        fun detectProvider(key: String): String = when {
            key.startsWith("AIzaSy", ignoreCase = false) || key.startsWith("AQ.", ignoreCase = false) -> PROVIDER_GEMINI
            key.startsWith("gsk_", ignoreCase = false) -> PROVIDER_GROQ
            key.startsWith("sk-proj-", ignoreCase = false) -> PROVIDER_OPENAI
            key.startsWith("sk-or-", ignoreCase = false) -> PROVIDER_OPENROUTER
            key.startsWith("sk-ant-", ignoreCase = false) -> PROVIDER_CLAUDE
            key.startsWith("sk-", ignoreCase = false) -> PROVIDER_OPENAI
            else -> PROVIDER_GROQ
        }
    }

    init {
        getOrCreateSecretKey()
        ensureDefaultKeysConfigured()
    }

    private fun ensureDefaultKeysConfigured() {
        try {
            val configKey = com.khabar.app.BuildConfig.AI_API_KEY.trim()
            if (configKey.isNotBlank()) {
                val detectedProvider = detectProviderFromKey(configKey)
                val existingKey = getApiKeyForProvider(detectedProvider)
                if (existingKey.isBlank() || existingKey.contains("placeholder")) {
                    saveApiKeyForProvider(detectedProvider, configKey)
                }
            }

            // Ensure Groq has active working model default
            val currentGroqModel = getModelForProvider(PROVIDER_GROQ)
            if (currentGroqModel.isBlank() || currentGroqModel.contains("llama")) {
                saveAiSettings(PROVIDER_GROQ, getDefaultBaseUrl(PROVIDER_GROQ), "openai/gpt-oss-120b")
            }

            // Ensure Gemini has active working model default
            val currentGeminiModel = getModelForProvider(PROVIDER_GEMINI)
            if (currentGeminiModel.isBlank() || currentGeminiModel.contains("2.5") || currentGeminiModel.contains("2.0") || currentGeminiModel.contains("1.5") || currentGeminiModel.contains("3.8")) {
                saveAiSettings(PROVIDER_GEMINI, getDefaultBaseUrl(PROVIDER_GEMINI), "gemini-flash-lite-latest")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenParameterSpec = android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Save an encrypted API key specifically for a provider (e.g. Gemini, Groq, OpenRouter, Claude).
     */
    fun saveApiKeyForProvider(provider: String, key: String) {
        val norm = normalizeProvider(provider)
        val keyPref = "encrypted_ai_api_key_$norm"
        val ivPref = "encrypted_ai_api_iv_$norm"

        if (key.isBlank()) {
            prefs.edit().remove(keyPref).remove(ivPref).apply()
            return
        }

        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(key.trim().toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(keyPref, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(ivPref, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get the decrypted API key for the specified provider.
     * Falls back to the legacy single key if the provider matches.
     */
    fun getApiKeyForProvider(provider: String): String {
        val norm = normalizeProvider(provider)
        val keyPref = "encrypted_ai_api_key_$norm"
        val ivPref = "encrypted_ai_api_iv_$norm"

        val encryptedBase64 = prefs.getString(keyPref, null)
        val ivBase64 = prefs.getString(ivPref, null)

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                val decrypted = String(decryptedBytes, Charsets.UTF_8).trim()
                if (decrypted.isNotBlank()) return decrypted
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Check fallback legacy key
        val legacy = getAiApiKey()
        if (legacy.isNotBlank()) {
            val legacyDetected = detectProvider(legacy)
            if (legacyDetected.equals(provider, ignoreCase = true)) {
                return legacy
            }
        }

        return ""
    }

    fun hasApiKeyForProvider(provider: String): Boolean {
        if (provider.equals(PROVIDER_OLLAMA, ignoreCase = true)) return true
        return getApiKeyForProvider(provider).isNotBlank()
    }

    fun hasAnyApiKey(): Boolean {
        return hasApiKeyForProvider(PROVIDER_GEMINI) ||
                hasApiKeyForProvider(PROVIDER_GROQ) ||
                hasApiKeyForProvider(PROVIDER_OPENAI) ||
                hasApiKeyForProvider(PROVIDER_OPENROUTER) ||
                hasApiKeyForProvider(PROVIDER_CLAUDE) ||
                getAiApiKey().isNotBlank()
    }

    fun getConfiguredProviders(): List<String> {
        val list = mutableListOf<String>()
        if (hasApiKeyForProvider(PROVIDER_GROQ)) list.add(PROVIDER_GROQ)
        if (hasApiKeyForProvider(PROVIDER_GEMINI)) list.add(PROVIDER_GEMINI)
        if (hasApiKeyForProvider(PROVIDER_OPENAI)) list.add(PROVIDER_OPENAI)
        if (hasApiKeyForProvider(PROVIDER_OPENROUTER)) list.add(PROVIDER_OPENROUTER)
        if (hasApiKeyForProvider(PROVIDER_CLAUDE)) list.add(PROVIDER_CLAUDE)
        return list
    }

    // Legacy single key support
    fun saveAiApiKey(key: String) {
        if (key.isBlank()) {
            prefs.edit().remove(PREF_AI_KEY).remove(PREF_AI_IV).apply()
            return
        }
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(key.trim().toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(PREF_AI_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(PREF_AI_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()

            // Also mirror to provider-specific slot
            val provider = detectProvider(key)
            saveApiKeyForProvider(provider, key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAiApiKey(): String {
        val encryptedBase64 = prefs.getString(PREF_AI_KEY, null) ?: return ""
        val ivBase64 = prefs.getString(PREF_AI_IV, null) ?: return ""
        return try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveAiSettings(provider: String, baseUrl: String, model: String) {
        val canonical = canonicalProvider(provider)
        val norm = normalizeProvider(canonical)
        prefs.edit()
            .putString(PREF_AI_PROVIDER, canonical)
            .putString(PREF_AI_BASE_URL, baseUrl)
            .putString(PREF_AI_MODEL, model)
            .putString("ai_base_url_$norm", baseUrl)
            .putString("ai_model_$norm", model)
            .apply()
    }

    fun getAiBaseUrl(): String {
        val saved = prefs.getString(PREF_AI_BASE_URL, "") ?: ""
        if (saved.isNotBlank()) return saved
        return getDefaultBaseUrl(getAiProvider())
    }

    fun getAiModel(): String {
        val saved = prefs.getString(PREF_AI_MODEL, "") ?: ""
        if (saved.isNotBlank()) return saved
        return getDefaultModel(getAiProvider())
    }

    fun getAiProvider(): String = prefs.getString(PREF_AI_PROVIDER, PROVIDER_GROQ) ?: PROVIDER_GROQ

    fun isOllamaExplicitlyConfigured(): Boolean {
        val saved = prefs.getString("ai_base_url_ollama", null)
        return !saved.isNullOrBlank() && saved != getDefaultBaseUrl(PROVIDER_OLLAMA)
    }

    fun getBaseUrlForProvider(provider: String): String {
        val norm = normalizeProvider(provider)
        val saved = prefs.getString("ai_base_url_$norm", null)
        if (!saved.isNullOrBlank()) return saved
        return getDefaultBaseUrl(provider)
    }

    fun getModelForProvider(provider: String): String {
        val norm = normalizeProvider(provider)
        val saved = prefs.getString("ai_model_$norm", null)
        // Sanitize: reject truly deprecated/retired model names and reset to default
        val deprecated = setOf(
            "gemini-pro", "gemini-1.0-pro", "gpt-4-turbo", "mixtral-8x7b-32768", "llama-3.2-11b-vision-preview"
        )
        if (!saved.isNullOrBlank() && saved !in deprecated) return saved
        // If deprecated model was stored, clear it and use the correct default
        if (!saved.isNullOrBlank() && saved in deprecated) {
            val correct = getDefaultModel(provider)
            prefs.edit().putString("ai_model_$norm", correct).apply()
        }
        return getDefaultModel(provider)
    }

    fun clear() {
        prefs.edit().clear().apply()
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {}
    }

    fun saveSecret(keyAlias: String, secret: String) = saveAiApiKey(secret)
    fun getSecret(keyAlias: String): String? = getAiApiKey().ifBlank { null }
    fun clearAll() = clear()
}
