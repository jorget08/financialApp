package com.financialapp.domain.engine

import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.AccountType
import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.util.CurrencyFormatter
import java.util.Locale

data class DossierInput(
    val reviews: List<PeriodReview>,
    val accounts: List<Pair<AccountEntity, Double>>,
    val savings: SavingsAccountEntity?,
    val goals: List<GoalEntity>,
    val bills: List<RecurringBillEntity>,
    val budgets: List<BudgetStatus>,
    val loans: List<PersonLoan>
)

/**
 * Resumen que recibe la IA. Solo lleva agregados: ni observaciones de movimientos,
 * ni nombres de personas, ni textos de notificaciones salen del teléfono.
 */
object AdvisorDossier {

    private fun money(value: Double) = CurrencyFormatter.format(value)
    private fun percent(value: Double) = String.format(Locale.US, "%.0f%%", value * 100)

    fun build(input: DossierInput): String = buildString {
        val current = input.reviews.firstOrNull()
        if (current != null) {
            appendLine("Mes en curso (${current.label}, aún sin terminar):")
            appendLine("- Ingresos: ${money(current.income)} | Gastos: ${money(current.expenses)} | Apartado a ahorro: ${money(current.saved)}")
            if (current.byCategory.isNotEmpty()) {
                appendLine("- Gastos por categoría:")
                current.byCategory.entries.sortedByDescending { it.value }.forEach {
                    appendLine("  - ${it.key}: ${money(it.value)}")
                }
            }
        }

        val previous = input.reviews.drop(1).filter { it.income > 0 || it.expenses > 0 }
        if (previous.isNotEmpty()) {
            appendLine("Meses anteriores:")
            previous.forEach {
                appendLine("- ${it.label}: ingresos ${money(it.income)}, gastos ${money(it.expenses)}, tasa de ahorro ${percent(it.savingsRate)}")
            }
        }

        val averageExpenses = MonthlyReview.averageExpenses(input.reviews)
        if (input.accounts.isNotEmpty()) {
            appendLine("Cuentas:")
            input.accounts.forEach { (account, balance) ->
                val kind = if (account.type == AccountType.CREDIT_CARD) "tarjeta de crédito, deuda" else "saldo"
                appendLine("- ${account.name} ($kind): ${money(kotlin.math.abs(balance))}")
            }
        } else {
            appendLine("Cuentas: aún no ha registrado saldos de sus cuentas.")
        }

        val savings = input.savings
        if (savings != null) {
            appendLine("Cuenta de ahorro: ${savings.bankName}, saldo ${money(savings.initialBalance)}, tasa ${savings.teaRate}% E.A., aporte mensual planeado ${money(savings.plannedMonthlyDeposit)}")
            MonthlyReview.emergencyMonths(savings.initialBalance, averageExpenses)?.let {
                appendLine("El ahorro cubre ${String.format(Locale.US, "%.1f", it)} meses de gastos (promedio mensual ${money(averageExpenses)}).")
            }
        } else {
            appendLine("Cuenta de ahorro: no configurada.")
        }

        if (input.goals.isNotEmpty()) {
            appendLine("Metas, en su orden de prioridad:")
            input.goals.forEach { goal ->
                appendLine("- ${goal.name}: objetivo ${money(goal.targetAmount)}, lleva ${money(goal.savedAmount)}")
            }
        } else {
            appendLine("Metas: no ha registrado metas.")
        }

        if (input.bills.isNotEmpty()) {
            appendLine("Pagos fijos al mes: ${money(input.bills.sumOf { it.amount })} (${input.bills.size} pagos)")
        }
        if (input.budgets.isNotEmpty()) {
            appendLine("Presupuestos este mes:")
            input.budgets.forEach {
                appendLine("- ${it.category.name}: ${money(it.spent)} de ${money(it.limit)}")
            }
        }

        val owedToMe = input.loans.filter { it.balance > 0 }.sumOf { it.balance }
        val iOwe = input.loans.filter { it.balance < 0 }.sumOf { -it.balance }
        if (owedToMe > 0 || iOwe > 0) {
            appendLine("Préstamos: me deben ${money(owedToMe)}; debo a otras personas ${money(iOwe)}.")
        }
    }.trim()
}
