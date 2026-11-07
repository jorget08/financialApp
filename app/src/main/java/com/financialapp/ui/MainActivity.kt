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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financialapp.FinancialApplication
import com.financialapp.capture.AppNotifier
import com.financialapp.data.FinanceSnapshot
import com.financialapp.data.TransactionEdit
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.ui.navigation.ToolRoute
import com.financialapp.ui.navigation.financeToolRoutes
import com.financialapp.ui.screens.advisor.AIAdvisorScreen
import com.financialapp.ui.screens.chat.QuickChatScreen
import com.financialapp.ui.screens.expenses.ExpensesScreen
import com.financialapp.ui.screens.home.HomeScreen
import com.financialapp.ui.screens.quickentry.QuickExpenseDialogActivity
import com.financialapp.ui.screens.savings.SavingsCompoundScreen
import com.financialapp.ui.screens.settings.SettingsScreen
import com.financialapp.ui.theme.AccentPrimary
import com.financialapp.ui.theme.DarkBackground
import com.financialapp.ui.theme.DarkSurface
import com.financialapp.ui.theme.FinancialAppTheme
import com.financialapp.ui.theme.TextSecondary
import com.financialapp.util.FinancialMonth
import com.financialapp.widget.QuickExpenseGlanceWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Savings : Screen("savings", "Ahorros", Icons.Default.Savings)
    object Chat : Screen("chat", "Chat", Icons.Default.ChatBubbleOutline)
    object Advisor : Screen(ToolRoute.ADVISOR, "Asesor IA", Icons.Default.AutoAwesome)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
    object Expenses : Screen("expenses", "Gastos", Icons.Default.Search)
}

class MainActivity : ComponentActivity() {

    private var sharedTextFromIntent: String? = null
    private var routeFromNotification by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capturar si el usuario compartió texto desde WhatsApp u otra app
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedTextFromIntent = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        routeFromNotification = intent?.getStringExtra(AppNotifier.EXTRA_OPEN_ROUTE)

        val app = application as FinancialApplication
        val db = app.database
        val repository = app.repository
        val aiService = app.aiAssistantService
        val settingsManager = app.settingsManager
        val snapshot = FinanceSnapshot(applicationContext, db)

        setContent {
            FinancialAppTheme {
                val navController = rememberNavController()

                // Recopilar flujos de datos en tiempo real desde Room
                val recentTransactions by db.transactionDao().getRecentTransactions().collectAsState(initial = emptyList())
                val allCategories by db.categoryDao().getAllCategoriesFlow().collectAsState(initial = emptyList())
                val primaryAccount by db.savingsDao().getPrimaryAccountFlow().collectAsState(initial = null)
                val accounts by db.accountDao().activeFlow().collectAsState(initial = emptyList())
                val pendingMovements by db.pendingMovementDao().flow().collectAsState(initial = emptyList())
                val suggestions by db.suggestionDao().flow().collectAsState(initial = emptyList())

                // Mes financiero: puede empezar el día de pago en lugar del día 1
                var monthStartDay by remember { mutableStateOf(FinancialMonth.getStartDay(applicationContext)) }
                val (startOfMonth, endOfMonth) = remember(monthStartDay) { FinancialMonth.currentPeriodRange(monthStartDay) }
                val previousPeriodRange = remember(monthStartDay) { FinancialMonth.previousPeriodRange(monthStartDay) }
                val periodLabel = remember(monthStartDay) { FinancialMonth.currentPeriodLabel(monthStartDay) }

                val totalExpenses by remember(startOfMonth) {
                    db.transactionDao().getTotalExpensesPeriod(startOfMonth, endOfMonth)
                }.collectAsState(initial = 0.0)
                val totalIncome by remember(startOfMonth) {
                    db.transactionDao().getTotalIncomesPeriod(startOfMonth, endOfMonth)
                }.collectAsState(initial = 0.0)
                val totalSaved by remember(startOfMonth) {
                    db.transactionDao().getTotalSavedPeriod(startOfMonth, endOfMonth)
                }.collectAsState(initial = 0.0)

                val updateTransaction: (String, TransactionEdit) -> Unit = { id, edit ->
                    lifecycleScope.launch(Dispatchers.IO) { repository.updateTransaction(id, edit) }
                }
                val deleteTransaction: (String) -> Unit = { id ->
                    lifecycleScope.launch(Dispatchers.IO) { repository.deleteTransaction(id) }
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
                                accounts = accounts,
                                totalIncome = totalIncome,
                                totalExpenses = totalExpenses,
                                totalSaved = totalSaved,
                                pendingCount = pendingMovements.size,
                                periodLabel = periodLabel,
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
                                onOpenExpenses = { navController.navigate(Screen.Expenses.route) },
                                onOpenRoute = { route -> navController.navigate(route) },
                                onUpdateTransaction = updateTransaction,
                                onDeleteTransaction = deleteTransaction
                            )
                        }

                        // Gastos por Categoría y Búsqueda en todo el historial (se abre desde Inicio)
                        composable(Screen.Expenses.route) {
                            val allTransactions by remember {
                                db.transactionDao().getAllTransactionsWithCategory()
                            }.collectAsState(initial = emptyList())

                            ExpensesScreen(
                                transactions = allTransactions,
                                categories = allCategories,
                                accounts = accounts,
                                currentPeriodRange = startOfMonth to endOfMonth,
                                previousPeriodRange = previousPeriodRange,
                                onUpdateTransaction = updateTransaction,
                                onDeleteTransaction = deleteTransaction,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        financeToolRoutes(
                            navController = navController,
                            db = db,
                            repository = repository,
                            scope = lifecycleScope,
                            categories = { allCategories },
                            monthStartDay = { monthStartDay },
                            periodLabel = { periodLabel }
                        )

                        // 2. Pantalla de Ahorros e Interés Compuesto No Lineal
                        composable(Screen.Savings.route) {
                            SavingsCompoundScreen(
                                primaryAccount = primaryAccount,
                                accounts = accounts,
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
                                onRecordNewDeposit = { amount, fromAccountId ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        repository.recordSavingsDeposit(amount, fromAccountId)
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
                                            ?: db.categoryDao().getAllCategories().firstOrNull { it.type == type }

                                        if (cat != null) {
                                            repository.addMovement(amount, type, cat.id, note, source = "CHAT_NLP")
                                        }
                                    }
                                }
                            )
                        }

                        // 4. Asesor Financiero IA: sugerencias y conversación
                        composable(Screen.Advisor.route) {
                            AIAdvisorScreen(
                                aiService = aiService,
                                settingsManager = settingsManager,
                                suggestions = suggestions,
                                loadDossier = { withContext(Dispatchers.IO) { snapshot.dossier() } },
                                onSaveSuggestion = { withContext(Dispatchers.IO) { db.suggestionDao().insert(it) } },
                                onDeleteSuggestion = { id ->
                                    lifecycleScope.launch(Dispatchers.IO) { db.suggestionDao().delete(id) }
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }

                        // 5. Ajustes y Configuración de API Key Segura
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                settingsManager = settingsManager,
                                preferences = app.preferences,
                                backupManager = app.backupManager,
                                monthStartDay = monthStartDay,
                                onMonthStartDayChange = { day ->
                                    FinancialMonth.saveStartDay(applicationContext, day)
                                    monthStartDay = day
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        QuickExpenseGlanceWidget().updateAll(applicationContext)
                                    }
                                },
                                onOpenCategories = { navController.navigate(ToolRoute.CATEGORIES) }
                            )
                        }
                    }
                }

                // Al tocar un aviso se abre la pantalla correspondiente, también con la app ya abierta
                LaunchedEffect(routeFromNotification) {
                    val route = routeFromNotification ?: return@LaunchedEffect
                    routeFromNotification = null
                    if (route != Screen.Home.route) {
                        navController.navigate(route) { launchSingleTop = true }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeFromNotification = intent.getStringExtra(AppNotifier.EXTRA_OPEN_ROUTE)
    }
}
