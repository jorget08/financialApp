package com.financialapp.domain.engine

import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.TransactionEntity
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Saldo de cuentas y tarjetas a partir de sus movimientos. */
object AccountLedger {

    /**
     * Saldo = saldo inicial + movimientos desde la fecha de ese saldo. Lo anterior a esa fecha
     * ya estaba incluido en el saldo inicial: registrar hoy un gasto olvidado de la semana
     * pasada no debe restarlo dos veces.
     */
    fun balance(account: AccountEntity, transactions: List<TransactionEntity>): Double =
        account.openingBalance + transactions
            .filter { it.date >= account.openingDate }
            .sumOf { effectOn(account.id, it) }

    private fun effectOn(accountId: String, t: TransactionEntity): Double {
        var effect = 0.0
        if (t.accountId == accountId) {
            effect += when (t.type) {
                MovementType.EXPENSE, MovementType.LOAN_OUT, MovementType.SAVINGS, MovementType.TRANSFER -> -t.amount
                MovementType.INCOME, MovementType.LOAN_IN -> t.amount
                MovementType.ADJUSTMENT -> t.amount
                else -> 0.0
            }
        }
        if (t.type == MovementType.TRANSFER && t.toAccountId == accountId) effect += t.amount
        return effect
    }
}

data class PersonLoan(
    val personName: String,
    val paidOut: Double,
    val received: Double,
    val movementCount: Int,
    val lastMovementAt: Long
) {
    /** Positivo: esa persona te debe. Negativo: tú le debes. */
    val balance: Double get() = paidOut - received
}

object LoanLedger {

    /** Agrupa ignorando mayúsculas y tildes: "CAMILO" y "Camilo" son la misma persona. */
    fun summarize(transactions: List<TransactionEntity>): List<PersonLoan> =
        transactions
            .filter { !it.personName.isNullOrBlank() && (it.type == MovementType.LOAN_OUT || it.type == MovementType.LOAN_IN) }
            .groupBy { normalizeName(it.personName!!) }
            .map { (_, items) ->
                PersonLoan(
                    personName = items.maxByOrNull { it.date }!!.personName!!.trim(),
                    paidOut = items.filter { it.type == MovementType.LOAN_OUT }.sumOf { it.amount },
                    received = items.filter { it.type == MovementType.LOAN_IN }.sumOf { it.amount },
                    movementCount = items.size,
                    lastMovementAt = items.maxOf { it.date }
                )
            }
            .sortedWith(compareByDescending<PersonLoan> { kotlin.math.abs(it.balance) }.thenBy { it.personName })

    fun normalizeName(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
}

enum class BudgetLevel { OK, WARNING, OVER }

data class BudgetStatus(
    val category: CategoryEntity,
    val limit: Double,
    val spent: Double
) {
    val ratio: Double get() = if (limit > 0) spent / limit else 0.0
    val remaining: Double get() = limit - spent
    val level: BudgetLevel
        get() = when {
            spent >= limit -> BudgetLevel.OVER
            ratio >= BudgetCalculator.WARNING_RATIO -> BudgetLevel.WARNING
            else -> BudgetLevel.OK
        }
}

object BudgetCalculator {
    const val WARNING_RATIO = 0.8

    fun statuses(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        range: Pair<Long, Long>
    ): List<BudgetStatus> {
        val spentByCategory = transactions
            .filter { it.type == MovementType.EXPENSE && it.date >= range.first && it.date < range.second }
            .groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
        return budgets.mapNotNull { budget ->
            categories.firstOrNull { it.id == budget.categoryId }?.let { category ->
                BudgetStatus(category, budget.monthlyLimit, spentByCategory[budget.categoryId] ?: 0.0)
            }
        }.sortedByDescending { it.ratio }
    }
}

data class BillDue(val bill: RecurringBillEntity, val dueDate: LocalDate, val daysUntil: Long) {
    val isOverdue: Boolean get() = daysUntil < 0
}

object BillSchedule {

    /** Próximo vencimiento: el de este mes si aún no se pagó, si no el del mes siguiente. */
    fun nextDue(bill: RecurringBillEntity, today: LocalDate): BillDue {
        val thisMonth = YearMonth.from(today)
        val month = if (bill.lastPaidMonth == thisMonth.toString()) thisMonth.plusMonths(1) else thisMonth
        val due = month.atDay(bill.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
        return BillDue(bill, due, ChronoUnit.DAYS.between(today, due))
    }

    fun monthKey(date: LocalDate): String = YearMonth.from(date).toString()
}

data class PeriodReview(
    val label: String,
    val income: Double,
    val expenses: Double,
    val saved: Double,
    val byCategory: Map<String, Double>
) {
    /** Lo que queda de los ingresos, incluido lo apartado a ahorro. */
    val savingsRate: Double get() = if (income > 0) (income - expenses) / income else 0.0
}

data class CategoryChange(val categoryName: String, val current: Double, val previous: Double) {
    val difference: Double get() = current - previous
}

object MonthlyReview {

    /** Periodos de más reciente a más antiguo. */
    fun build(
        transactions: List<TransactionEntity>,
        categoryNames: Map<String, String>,
        periods: List<Pair<String, Pair<Long, Long>>>
    ): List<PeriodReview> = periods.map { (label, range) ->
        val inside = transactions.filter { it.date >= range.first && it.date < range.second }
        val expenses = inside.filter { it.type == MovementType.EXPENSE }
        PeriodReview(
            label = label,
            income = inside.filter { it.type == MovementType.INCOME }.sumOf { it.amount },
            expenses = expenses.sumOf { it.amount },
            saved = inside.filter { it.type == MovementType.SAVINGS }.sumOf { it.amount },
            byCategory = expenses.groupBy { categoryNames[it.categoryId] ?: "Sin categoría" }
                .mapValues { (_, items) -> items.sumOf { it.amount } }
        )
    }

    /** Categorías que más cambiaron frente al periodo anterior, de mayor a menor cambio. */
    fun changes(current: PeriodReview, previous: PeriodReview): List<CategoryChange> =
        (current.byCategory.keys + previous.byCategory.keys)
            .map { CategoryChange(it, current.byCategory[it] ?: 0.0, previous.byCategory[it] ?: 0.0) }
            .filter { it.difference != 0.0 }
            .sortedByDescending { kotlin.math.abs(it.difference) }

    /** Promedio de gasto de los periodos completos; el actual va a medias y bajaría la cifra. */
    fun averageExpenses(reviews: List<PeriodReview>): Double {
        val complete = reviews.drop(1).filter { it.expenses > 0 }
        if (complete.isNotEmpty()) return complete.map { it.expenses }.average()
        return reviews.firstOrNull()?.expenses ?: 0.0
    }

    /** Cuántos meses de gastos cubre el ahorro. Null si aún no hay gastos para calcularlo. */
    fun emergencyMonths(savings: Double, averageExpenses: Double): Double? =
        if (averageExpenses > 0) savings / averageExpenses else null
}
