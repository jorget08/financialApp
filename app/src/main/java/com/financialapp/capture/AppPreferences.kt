package com.financialapp.capture

import android.content.Context

/** Preferencias no sensibles de captura y recordatorios. */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("capture_prefs", Context.MODE_PRIVATE)

    var captureEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAPTURE, true)
        set(value) = prefs.edit().putBoolean(KEY_CAPTURE, value).apply()

    var captureKeywords: String
        get() = prefs.getString(KEY_KEYWORDS, DEFAULT_KEYWORDS) ?: DEFAULT_KEYWORDS
        set(value) = prefs.edit().putString(KEY_KEYWORDS, value).apply()

    var dailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER, true)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER, value).apply()

    var lastAccountId: String?
        get() = prefs.getString(KEY_LAST_ACCOUNT, null)
        set(value) = prefs.edit().putString(KEY_LAST_ACCOUNT, value).apply()

    fun keywordList(): List<String> =
        captureKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    /** Devuelve true solo la primera vez, para no repetir el mismo aviso de presupuesto. */
    fun markBudgetAlert(key: String): Boolean {
        val full = "budget_alert_$key"
        if (prefs.getBoolean(full, false)) return false
        prefs.edit().putBoolean(full, true).apply()
        return true
    }

    private companion object {
        const val KEY_CAPTURE = "capture_enabled"
        const val KEY_KEYWORDS = "capture_keywords"
        const val KEY_REMINDER = "daily_reminder"
        const val KEY_LAST_ACCOUNT = "last_account"
        const val DEFAULT_KEYWORDS = "nequi, falabella, cmr"
    }
}
