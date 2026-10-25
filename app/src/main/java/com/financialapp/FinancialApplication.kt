package com.financialapp

import android.app.Application
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.remote.ai.AIAssistantService
import com.financialapp.data.remote.ai.AISettingsManager

class FinancialApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsManager: AISettingsManager
        private set

    lateinit var aiAssistantService: AIAssistantService
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        settingsManager = AISettingsManager(this)
        aiAssistantService = AIAssistantService(settingsManager)
    }
}
