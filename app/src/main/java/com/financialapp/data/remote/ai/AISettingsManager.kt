package com.financialapp.data.remote.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestor Seguro de Preferencias y Claves de API.
 * Almacena una clave por proveedor utilizando cifrado por hardware (Android Keystore).
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

    init {
        migrateLegacyKey()
    }

    /** Antes había una sola clave: pertenece al proveedor que estaba elegido. */
    private fun migrateLegacyKey() {
        val legacy = securePrefs.getString(KEY_API_KEY, null) ?: return
        val provider = getProvider()
        val editor = securePrefs.edit().remove(KEY_API_KEY)
        if (legacy.isNotBlank() && securePrefs.getString(keyFor(provider), null).isNullOrBlank()) {
            editor.putString(keyFor(provider), legacy)
        }
        editor.apply()
    }

    fun saveApiKey(provider: String, apiKey: String) {
        securePrefs.edit().putString(keyFor(provider), apiKey.trim()).apply()
    }

    fun getApiKey(provider: String = getProvider()): String {
        return securePrefs.getString(keyFor(provider), "") ?: ""
    }

    fun saveProvider(provider: String) {
        securePrefs.edit().putString(KEY_PROVIDER, provider).apply()
    }

    fun getProvider(): String {
        return securePrefs.getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
    }

    fun saveModelName(model: String) {
        securePrefs.edit().putString(KEY_MODEL, model.trim()).apply()
    }

    fun getModelName(): String {
        val provider = getProvider()
        val saved = securePrefs.getString(KEY_MODEL, null)
        if (saved.isNullOrBlank() || RETIRED_MODELS.contains(saved)) return defaultModel(provider)
        return saved
    }

    fun isConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    private fun keyFor(provider: String) = "api_key_$provider"

    companion object {
        private const val KEY_API_KEY = "user_ai_api_key"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_MODEL = "ai_model"

        const val PROVIDER_GEMINI = "Google Gemini"
        const val PROVIDER_OPENAI = "OpenAI"
        const val PROVIDER_CLAUDE = "Claude"

        val PROVIDERS = listOf(PROVIDER_GEMINI, PROVIDER_OPENAI, PROVIDER_CLAUDE)

        // Modelos que versiones anteriores de la app guardaban por defecto
        private val RETIRED_MODELS = setOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gpt-4o", "gpt-4o-mini")

        fun defaultModel(provider: String): String = modelsFor(provider).first().first

        /** Id del modelo y etiqueta visible; el primero es el recomendado. */
        fun modelsFor(provider: String): List<Pair<String, String>> = when (provider) {
            PROVIDER_OPENAI -> listOf(
                "gpt-6.1-sol" to "GPT-6.1 Sol (Recomendado: equilibrio)",
                "gpt-6-astra" to "GPT-6 Astra (Máximo razonamiento)",
                "gpt-6-luna" to "GPT-6 Luna (Más económico)"
            )
            PROVIDER_CLAUDE -> listOf(
                "claude-opus-5-5" to "Claude Opus 5.5 (Recomendado: máximo razonamiento)",
                "claude-sonnet-5-5" to "Claude Sonnet 5.5 (Equilibrio)",
                "claude-haiku-5-5" to "Claude Haiku 5.5 (Más económico)"
            )
            else -> listOf(
                "gemini-flash-latest" to "Gemini Flash (Recomendado: siempre el más reciente)",
                "gemini-flash-lite-latest" to "Gemini Flash-Lite (Más económico)"
            )
        }

        fun keyHint(provider: String): String = when (provider) {
            PROVIDER_OPENAI -> "sk-... (platform.openai.com)"
            PROVIDER_CLAUDE -> "sk-ant-... (platform.claude.com)"
            else -> "AIza... (aistudio.google.com)"
        }
    }
}
