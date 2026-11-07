package com.financialapp.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.financialapp.data.FinanceRepository
import com.financialapp.data.TransactionEdit
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.domain.engine.MonthlyReview
import com.financialapp.ui.screens.accounts.AccountsScreen
import com.financialapp.ui.screens.loans.LoansScreen
import com.financialapp.ui.screens.pending.PendingScreen
import com.financialapp.ui.screens.planning.BillsScreen
import com.financialapp.ui.screens.planning.BudgetsScreen
import com.financialapp.ui.screens.planning.CategoriesScreen
import com.financialapp.ui.screens.planning.GoalsScreen
import com.financialapp.ui.screens.planning.ReviewScreen
import com.financialapp.util.FinancialMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ToolRoute {
    const val PENDING = "pending"
    const val ACCOUNTS = "accounts"
    const val LOANS = "loans"
    const val BUDGETS = "budgets"
    const val BILLS = "bills"
    const val GOALS = "goals"
    const val REVIEW = "review"
    const val CATEGORIES = "categories"
    const val ADVISOR = "advisor"
}

private const val REVIEW_MONTHS = 6

/** Pantallas que se abren desde Inicio. Cada una lee de Room solo lo que necesita mientras está abierta. */
fun NavGraphBuilder.financeToolRoutes(
    navController: NavController,
    db: AppDatabase,
    repository: FinanceRepository,
    scope: CoroutineScope,
    categories: () -> List<CategoryEntity>,
    monthStartDay: () -> Int,
    periodLabel: () -> String
) {
    fun launchIo(block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) { block() }
    }
    val back: () -> Unit = { navController.popBackStack() }

    composable(ToolRoute.PENDING) {
        val pending by remember { db.pendingMovementDao().flow() }.collectAsState(initial = emptyList())
        val transactions by remember { db.transactionDao().getAllEntitiesFlow() }.collectAsState(initial = emptyList())
        val accounts by remember { db.accountDao().activeFlow() }.collectAsState(initial = emptyList())
        PendingScreen(
            pending = pending,
            transactions = transactions,
            categories = categories(),
            accounts = accounts,
            onConfirm = { item, edit: TransactionEdit -> launchIo { repository.confirmPending(item, edit) } },
            onDiscard = { id -> launchIo { repository.discardPending(id) } },
            onBack = back
        )
    }

    composable(ToolRoute.ACCOUNTS) {
        val accounts by remember { db.accountDao().activeFlow() }.collectAsState(initial = emptyList())
        val transactions by remember { db.transactionDao().getAllEntitiesFlow() }.collectAsState(initial = emptyList())
        AccountsScreen(
            accounts = accounts,
            transactions = transactions,
            onSaveAccount = { launchIo { repository.saveAccount(it) } },
            onRemoveAccount = { launchIo { repository.removeAccount(it) } },
            onReconcile = { account, calculated, real -> launchIo { repository.reconcile(account, calculated, real) } },
            onTransfer = { from, to, amount, date, note -> launchIo { repository.transfer(from, to, amount, date, note) } },
            onBack = back
        )
    }

    composable(ToolRoute.LOANS) {
        val transactions by remember { db.transactionDao().getAllEntitiesFlow() }.collectAsState(initial = emptyList())
        val movements by remember { db.transactionDao().getAllTransactionsWithCategory() }.collectAsState(initial = emptyList())
        val accounts by remember { db.accountDao().activeFlow() }.collectAsState(initial = emptyList())
        LoansScreen(
            transactions = transactions,
            allMovements = movements,
            accounts = accounts,
            onRecordLoan = { person, amount, moneyOut, accountId, date, note ->
                launchIo { repository.recordLoan(person, amount, moneyOut, accountId, date, note) }
            },
            onConvert = { ids, person -> launchIo { repository.convertToLoans(ids, person) } },
            onBack = back
        )
    }

    composable(ToolRoute.BUDGETS) {
        val budgets by remember { db.budgetDao().flow() }.collectAsState(initial = emptyList())
        val transactions by remember { db.transactionDao().getAllEntitiesFlow() }.collectAsState(initial = emptyList())
        val range = remember(monthStartDay()) { FinancialMonth.currentPeriodRange(monthStartDay()) }
        BudgetsScreen(
            budgets = budgets,
            categories = categories(),
            transactions = transactions,
            periodRange = range,
            periodLabel = periodLabel(),
            onSave = { id, limit -> launchIo { repository.saveBudget(id, limit) } },
            onDelete = { id -> launchIo { repository.deleteBudget(id) } },
            onBack = back
        )
    }

    composable(ToolRoute.BILLS) {
        val bills by remember { db.billDao().flow() }.collectAsState(initial = emptyList())
        val accounts by remember { db.accountDao().activeFlow() }.collectAsState(initial = emptyList())
        BillsScreen(
            bills = bills,
            categories = categories(),
            accounts = accounts,
            onSave = { launchIo { repository.saveBill(it) } },
            onDelete = { launchIo { repository.deleteBill(it) } },
            onPay = { bill, amount, accountId -> launchIo { repository.payBill(bill, amount, accountId) } },
            onBack = back
        )
    }

    composable(ToolRoute.GOALS) {
        val goals by remember { db.goalDao().flow() }.collectAsState(initial = emptyList())
        val savings by remember { db.savingsDao().getPrimaryAccountFlow() }.collectAsState(initial = null)
        val transactions by remember { db.transactionDao().getAllEntitiesFlow() }.collectAsState(initial = emptyList())
        val average = remember(transactions, monthStartDay()) {
            MonthlyReview.averageExpenses(reviews(transactions, categories(), monthStartDay()))
        }
        GoalsScreen(
            goals = goals,
            savingsBalance = savings?.initialBalance,
            averageMonthlyExpenses = average,
            emergencyMonths = savings?.let { MonthlyReview.emergencyMonths(it.initialBalance, average) },
            onSave = { launchIo { repository.saveGoal(it) } },
            onDelete = { launchIo { repository.deleteGoal(it) } },
            onBack = back
        )
    }

    composable(ToolRoute.REVIEW) {
        val transactions by remember { db.transactionDao().getAllEntitiesFlow() }.collectAsState(initial = emptyList())
        ReviewScreen(
            reviews = remember(transactions, monthStartDay()) { reviews(transactions, categories(), monthStartDay()) },
            onAskAdvisor = { navController.navigate(ToolRoute.ADVISOR) },
            onBack = back
        )
    }

    composable(ToolRoute.CATEGORIES) {
        CategoriesScreen(
            categories = categories(),
            onSave = { launchIo { repository.saveCategory(it) } },
            onDelete = { category -> withContext(Dispatchers.IO) { repository.deleteCategory(category) } },
            onBack = back
        )
    }
}

private fun reviews(
    transactions: List<com.financialapp.data.local.entities.TransactionEntity>,
    categories: List<CategoryEntity>,
    startDay: Int
) = MonthlyReview.build(
    transactions,
    categories.associate { it.id to it.name },
    (0 until REVIEW_MONTHS).map { FinancialMonth.periodAt(startDay, it) }
)
