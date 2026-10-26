package com.letdigitalfly.ibamail.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Modern Android Keystore AES-GCM wrapper for storing sensitive API keys.
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

        private const val PREF_AI_KEY = "encrypted_ai_api_key"
        private const val PREF_AI_IV = "encrypted_ai_api_iv"
        private const val PREF_AI_BASE_URL = "ai_base_url_custom"
        private const val PREF_AI_MODEL = "ai_model_custom"
        private const val PREF_AI_PROVIDER = "ai_provider_preset"
    }

    init {
        getOrCreateSecretKey()
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
            val encryptedBytes = cipher.doFinal(key.toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(PREF_AI_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(PREF_AI_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
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
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveAiSettings(provider: String, baseUrl: String, model: String) {
        prefs.edit()
            .putString(PREF_AI_PROVIDER, provider)
            .putString(PREF_AI_BASE_URL, baseUrl)
            .putString(PREF_AI_MODEL, model)
            .apply()
    }

    fun getAiBaseUrl(): String = prefs.getString(PREF_AI_BASE_URL, "") ?: ""
    fun getAiModel(): String = prefs.getString(PREF_AI_MODEL, "") ?: ""
    fun getAiProvider(): String = prefs.getString(PREF_AI_PROVIDER, "Groq") ?: "Groq"

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
