package com.financialapp.data

import android.content.Context
import com.financialapp.data.local.AppDatabase
import com.financialapp.domain.engine.AccountLedger
import com.financialapp.domain.engine.AdvisorDossier
import com.financialapp.domain.engine.BudgetCalculator
import com.financialapp.domain.engine.DossierInput
import com.financialapp.domain.engine.LoanLedger
import com.financialapp.domain.engine.MonthlyReview
import com.financialapp.util.FinancialMonth

/** Lee el estado actual de la base y arma el resumen para el asesor. */
class FinanceSnapshot(private val context: Context, private val db: AppDatabase) {

    suspend fun dossier(): String {
        val startDay = FinancialMonth.getStartDay(context)
        val transactions = db.transactionDao().getAllEntities()
        val categories = db.categoryDao().getAllCategories()
        val periods = (0 until REVIEW_MONTHS).map { FinancialMonth.periodAt(startDay, it) }
        val current = FinancialMonth.currentPeriodRange(startDay)
        return AdvisorDossier.build(
            DossierInput(
                reviews = MonthlyReview.build(transactions, categories.associate { it.id to it.name }, periods),
                accounts = db.accountDao().active().map { it to AccountLedger.balance(it, transactions) },
                savings = db.savingsDao().getPrimaryAccount(),
                goals = db.goalDao().all(),
                bills = db.billDao().all(),
                budgets = BudgetCalculator.statuses(db.budgetDao().all(), categories, transactions, current),
                loans = LoanLedger.summarize(transactions)
            )
        )
    }

    private companion object {
        const val REVIEW_MONTHS = 4
    }
}
