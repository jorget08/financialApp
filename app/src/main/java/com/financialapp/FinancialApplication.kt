package com.financialapp

import android.app.Application
import com.financialapp.capture.AppNotifier
import com.financialapp.capture.AppPreferences
import com.financialapp.capture.BudgetMonitor
import com.financialapp.capture.DailyCheckWorker
import com.financialapp.data.FinanceRepository
import com.financialapp.data.backup.BackupManager
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.remote.ai.AIAssistantService
import com.financialapp.data.remote.ai.AISettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FinancialApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsManager: AISettingsManager
        private set

    lateinit var aiAssistantService: AIAssistantService
        private set

    lateinit var repository: FinanceRepository
        private set

    lateinit var backupManager: BackupManager
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = FinanceRepository(this, database)
        backupManager = BackupManager(this, database)
        preferences = AppPreferences(this)
        settingsManager = AISettingsManager(this)
        aiAssistantService = AIAssistantService(settingsManager)
        AppNotifier.createChannels(this)
        DailyCheckWorker.schedule(this)

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch { AppDatabase.ensureDefaultCategories(database) }
        scope.launch {
            database.transactionDao().getAllEntitiesFlow().collectLatest {
                BudgetMonitor.check(this@FinancialApplication, database)
            }
        }
    }
}
