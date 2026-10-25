package com.financialapp.data.remote.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestor Seguro de Preferencias y Claves de API.
 * Almacena la API Key del usuario utilizando cifrado por hardware (Android Keystore).
 */
class AISettingsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_ai_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(apiKey: String) {
        securePrefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun getApiKey(): String {
        return securePrefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun saveProvider(provider: String) {
        securePrefs.edit().putString(KEY_PROVIDER, provider).apply()
    }

    fun getProvider(): String {
        return securePrefs.getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
    }

    fun saveModelName(model: String) {
        securePrefs.edit().putString(KEY_MODEL, model).apply()
    }

    fun getModelName(): String {
        return securePrefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun isConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    companion object {
        private const val KEY_API_KEY = "user_ai_api_key"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_MODEL = "ai_model"

        const val DEFAULT_MODEL = "gemini-2.5-flash"

        const val PROVIDER_GEMINI = "Google Gemini"
        const val PROVIDER_OPENAI = "OpenAI"
    }
}
