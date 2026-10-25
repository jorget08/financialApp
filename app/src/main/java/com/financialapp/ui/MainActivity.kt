package com.financialapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financialapp.FinancialApplication
import com.financialapp.data.local.dao.CategoryExpenseSummary
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.ui.screens.advisor.AIAdvisorScreen
import com.financialapp.ui.screens.chat.QuickChatScreen
import com.financialapp.ui.screens.home.HomeScreen
import com.financialapp.ui.screens.quickentry.QuickExpenseDialogActivity
import com.financialapp.ui.screens.savings.SavingsCompoundScreen
import com.financialapp.ui.screens.settings.SettingsScreen
import com.financialapp.ui.theme.AccentPrimary
import com.financialapp.ui.theme.DarkBackground
import com.financialapp.ui.theme.DarkSurface
import com.financialapp.ui.theme.FinancialAppTheme
import com.financialapp.ui.theme.TextSecondary
import com.financialapp.widget.QuickExpenseGlanceWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Savings : Screen("savings", "Ahorros", Icons.Default.Savings)
    object Chat : Screen("chat", "Chat", Icons.Default.ChatBubbleOutline)
    object Advisor : Screen("advisor", "Asesor IA", Icons.Default.AutoAwesome)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private var sharedTextFromIntent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capturar si el usuario compartió texto desde WhatsApp u otra app
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedTextFromIntent = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        val app = application as FinancialApplication
        val db = app.database
        val aiService = app.aiAssistantService
        val settingsManager = app.settingsManager

        setContent {
            FinancialAppTheme {
                val navController = rememberNavController()

                // Recopilar flujos de datos en tiempo real desde Room
                val recentTransactions by db.transactionDao().getRecentTransactions().collectAsState(initial = emptyList())
                val allCategories by db.categoryDao().getAllCategoriesFlow().collectAsState(initial = emptyList())
                val primaryAccount by db.savingsDao().getPrimaryAccountFlow().collectAsState(initial = null)

                val startOfMonth = remember {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.timeInMillis
                }
                val endOfMonth = remember { System.currentTimeMillis() + 86400000L }

                val totalExpenses by db.transactionDao().getTotalExpensesPeriod(startOfMonth, endOfMonth).collectAsState(initial = 0.0)
                val totalIncome by db.transactionDao().getTotalIncomesPeriod(startOfMonth, endOfMonth).collectAsState(initial = 0.0)

                var expensesByCategory by remember { mutableStateOf<List<CategoryExpenseSummary>>(emptyList()) }

                LaunchedEffect(recentTransactions) {
                    expensesByCategory = db.transactionDao().getExpensesByCategorySince(startOfMonth)
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = DarkSurface) {
                            val items = listOf(
                                Screen.Home,
                                Screen.Savings,
                                Screen.Chat,
                                Screen.Advisor,
                                Screen.Settings
                            )
                            items.forEach { screen ->
                                val isSelected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentPrimary,
                                        selectedTextColor = AccentPrimary,
                                        indicatorColor = DarkBackground,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (sharedTextFromIntent != null) Screen.Chat.route else Screen.Home.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // 1. Pantalla de Inicio (Dashboard, Registro Rápido & Edición)
                        composable(Screen.Home.route) {
                            HomeScreen(
                                recentTransactions = recentTransactions,
                                categories = allCategories,
                                totalIncome = totalIncome,
                                totalExpenses = totalExpenses,
                                onQuickExpenseClick = {
                                    val intent = Intent(this@MainActivity, QuickExpenseDialogActivity::class.java).apply {
                                        putExtra(QuickExpenseDialogActivity.EXTRA_ENTRY_TYPE, "EXPENSE")
                                    }
                                    startActivity(intent)
                                },
                                onQuickIncomeClick = {
                                    val intent = Intent(this@MainActivity, QuickExpenseDialogActivity::class.java).apply {
                                        putExtra(QuickExpenseDialogActivity.EXTRA_ENTRY_TYPE, "INCOME")
                                    }
                                    startActivity(intent)
                                },
                                onUpdateTransaction = { id, amount, categoryId, note ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val existing = db.transactionDao().getTransactionById(id)
                                        if (existing != null) {
                                            db.transactionDao().updateTransaction(
                                                existing.copy(
                                                    amount = amount,
                                                    categoryId = categoryId,
                                                    note = note
                                                )
                                            )
                                            QuickExpenseGlanceWidget().updateAll(applicationContext)
                                        }
                                    }
                                },
                                onDeleteTransaction = { id ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val existing = db.transactionDao().getTransactionById(id)
                                        if (existing != null) {
                                            db.transactionDao().deleteTransaction(existing)
                                            QuickExpenseGlanceWidget().updateAll(applicationContext)
                                        }
                                    }
                                }
                            )
                        }

                        // 2. Pantalla de Ahorros e Interés Compuesto No Lineal
                        composable(Screen.Savings.route) {
                            SavingsCompoundScreen(
                                primaryAccount = primaryAccount,
                                onSaveAccountConfig = { name, bank, tea, initBal, monthlyDeposit ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val existing = db.savingsDao().getPrimaryAccount()
                                        val updated = (existing ?: SavingsAccountEntity(
                                            name = name,
                                            bankName = bank,
                                            teaRate = tea,
                                            initialBalance = initBal,
                                            plannedMonthlyDeposit = monthlyDeposit
                                        )).copy(
                                            name = name,
                                            bankName = bank,
                                            teaRate = tea,
                                            initialBalance = initBal,
                                            plannedMonthlyDeposit = monthlyDeposit
                                        )
                                        db.savingsDao().insertAccount(updated)
                                    }
                                },
                                onRecordNewDeposit = { amount ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val account = db.savingsDao().getPrimaryAccount()
                                        if (account != null) {
                                            db.savingsDao().insertDeposit(
                                                SavingsDepositEntity(
                                                    accountId = account.id,
                                                    amount = amount,
                                                    depositDate = System.currentTimeMillis()
                                                )
                                            )
                                            db.savingsDao().updateAccount(
                                                account.copy(initialBalance = account.initialBalance + amount)
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // 3. Pantalla de Chat Estilo WhatsApp / NLP
                        composable(Screen.Chat.route) {
                            QuickChatScreen(
                                initialSharedText = sharedTextFromIntent,
                                onSaveTransaction = { amount, type, categoryName, note ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val cat = db.categoryDao().findByName(categoryName)
                                            ?: db.categoryDao().getAllCategories().firstOrNull()

                                        if (cat != null) {
                                            db.transactionDao().insertTransaction(
                                                TransactionEntity(
                                                    amount = amount,
                                                    type = type,
                                                    categoryId = cat.id,
                                                    note = note,
                                                    source = "CHAT_NLP"
                                                )
                                            )
                                            QuickExpenseGlanceWidget().updateAll(applicationContext)
                                        }
                                    }
                                }
                            )
                        }

                        // 4. Asesor Financiero IA
                        composable(Screen.Advisor.route) {
                            AIAdvisorScreen(
                                totalIncome = totalIncome,
                                totalExpenses = totalExpenses,
                                expensesByCategory = expensesByCategory,
                                savingsAccount = primaryAccount,
                                aiService = aiService,
                                settingsManager = settingsManager,
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }

                        // 5. Ajustes y Configuración de API Key Segura
                        composable(Screen.Settings.route) {
                            SettingsScreen(settingsManager = settingsManager)
                        }
                    }
                }
            }
        }
    }
}
