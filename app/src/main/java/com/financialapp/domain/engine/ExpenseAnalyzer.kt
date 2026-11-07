package com.financialapp.domain.engine

import com.financialapp.data.local.dao.TransactionWithCategory
import com.financialapp.data.local.entities.MovementType
import java.text.Normalizer

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val total: Double,
    val count: Int,
    val share: Double // Fracción 0..1 del total de gastos recibido
)

data class MovementTotals(
    val expenses: Double,
    val expenseCount: Int,
    val income: Double,
    val incomeCount: Int,
    val loanOut: Double = 0.0,
    val loanIn: Double = 0.0
) {
    /** Lo que salió menos lo que entró. Ej: prestado a una persona menos lo que ya devolvió. */
    val outstanding: Double get() = expenses + loanOut - income - loanIn
}

/**
 * Búsqueda y totales sobre el historial completo de movimientos.
 * Se ejecuta en memoria porque LOWER() de SQLite no entiende tildes:
 * "cafe" debe encontrar "Café" y "camilo" debe encontrar "CAMILO".
 */
object ExpenseAnalyzer {

    private val diacritics = Regex("\\p{M}+")
    private val whitespace = Regex("\\s+")

    /**
     * Filtra por rango [inicio, fin) y por texto. Todas las palabras de la búsqueda deben
     * aparecer en la observación o en el nombre de la categoría. Rango null = todo el historial.
     */
    fun filter(
        transactions: List<TransactionWithCategory>,
        query: String,
        range: Pair<Long, Long>?
    ): List<TransactionWithCategory> {
        val terms = normalize(query).split(whitespace).filter { it.isNotEmpty() }
        return transactions.filter { item ->
            val inRange = range == null || (item.date >= range.first && item.date < range.second)
            inRange && matchesAll(item, terms)
        }
    }

    /** Gastos agrupados por categoría, de mayor a menor. Los ingresos no cuentan. */
    fun spendingByCategory(transactions: List<TransactionWithCategory>): List<CategorySpending> {
        val expenses = transactions.filter { it.type == "EXPENSE" }
        val grandTotal = expenses.sumOf { it.amount }
        return expenses.groupBy { it.categoryId }
            .map { (categoryId, items) ->
                val total = items.sumOf { it.amount }
                CategorySpending(
                    categoryId = categoryId,
                    categoryName = items.first().categoryName,
                    total = total,
                    count = items.size,
                    share = if (grandTotal > 0.0) total / grandTotal else 0.0
                )
            }
            .sortedWith(compareByDescending<CategorySpending> { it.total }.thenBy { it.categoryName })
    }

    fun totals(transactions: List<TransactionWithCategory>): MovementTotals {
        val expenses = transactions.filter { it.type == MovementType.EXPENSE }
        val income = transactions.filter { it.type == MovementType.INCOME }
        return MovementTotals(
            expenses = expenses.sumOf { it.amount },
            expenseCount = expenses.size,
            income = income.sumOf { it.amount },
            incomeCount = income.size,
            loanOut = transactions.filter { it.type == MovementType.LOAN_OUT }.sumOf { it.amount },
            loanIn = transactions.filter { it.type == MovementType.LOAN_IN }.sumOf { it.amount }
        )
    }

    private fun matchesAll(item: TransactionWithCategory, terms: List<String>): Boolean {
        if (terms.isEmpty()) return true
        val searchable = normalize("${item.note.orEmpty()} ${item.categoryName} ${item.personName.orEmpty()}")
        return terms.all { searchable.contains(it) }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase()
            .trim()
}
