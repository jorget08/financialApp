package com.financialapp.domain.engine

import com.financialapp.data.local.dao.TransactionWithCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseAnalyzerTest {

    private fun movement(
        id: String,
        amount: Double,
        note: String?,
        type: String = "EXPENSE",
        categoryId: String = "cat_otros_gastos",
        categoryName: String = "Otros Gastos",
        date: Long = 1_000L
    ) = TransactionWithCategory(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = "more_horiz",
        categoryColor = "#607D8B",
        note = note,
        date = date,
        source = "MANUAL"
    )

    private fun ids(items: List<TransactionWithCategory>) = items.map { it.id }

    @Test
    fun testSearchFindsEveryLoanToAPersonAcrossHistoryAndComputesWhatTheyOwe() {
        val movements = listOf(
            movement("a", 50_000.0, "Préstamo a Camilo", date = 1L),
            movement("b", 30_000.0, "CAMILO taxi", categoryId = "cat_transporte", categoryName = "Transporte", date = 2_000_000_000_000L),
            movement("c", 20_000.0, "camilo me devolvió", type = "INCOME", categoryId = "cat_otros_ingresos", categoryName = "Otros Ingresos"),
            movement("d", 15_000.0, "Almuerzo")
        )

        val matches = ExpenseAnalyzer.filter(movements, "camilo", range = null)
        val totals = ExpenseAnalyzer.totals(matches)

        assertEquals(listOf("a", "b", "c"), ids(matches))
        assertEquals(80_000.0, totals.expenses, 0.01)
        assertEquals(2, totals.expenseCount)
        assertEquals(20_000.0, totals.income, 0.01)
        assertEquals(60_000.0, totals.outstanding, 0.01)
    }

    @Test
    fun testSearchIgnoresAccentsCaseAndWordOrder() {
        val movements = listOf(
            movement("a", 5_000.0, "Café con Ana"),
            movement("b", 5_000.0, "Cafe solo")
        )

        assertEquals(listOf("a", "b"), ids(ExpenseAnalyzer.filter(movements, "CAFÉ", null)))
        assertEquals(listOf("a"), ids(ExpenseAnalyzer.filter(movements, "  ana   cafe ", null)))
    }

    @Test
    fun testSearchAlsoMatchesCategoryNameAndHandlesMissingNote() {
        val movements = listOf(
            movement("a", 9_000.0, null, categoryId = "cat_transporte", categoryName = "Transporte"),
            movement("b", 9_000.0, null)
        )

        assertEquals(listOf("a"), ids(ExpenseAnalyzer.filter(movements, "transporte", null)))
    }

    @Test
    fun testBlankQueryKeepsEverything() {
        val movements = listOf(movement("a", 1.0, "x"), movement("b", 2.0, null))

        assertEquals(listOf("a", "b"), ids(ExpenseAnalyzer.filter(movements, "   ", null)))
    }

    @Test
    fun testRangeIncludesStartAndExcludesEnd() {
        val movements = listOf(
            movement("before", 1.0, null, date = 99L),
            movement("start", 1.0, null, date = 100L),
            movement("inside", 1.0, null, date = 199L),
            movement("end", 1.0, null, date = 200L)
        )

        assertEquals(listOf("start", "inside"), ids(ExpenseAnalyzer.filter(movements, "", 100L to 200L)))
    }

    @Test
    fun testSpendingByCategorySortsLargestFirstAndIgnoresIncome() {
        val movements = listOf(
            movement("a", 25_000.0, null, categoryId = "cat_hogar", categoryName = "Hogar"),
            movement("b", 60_000.0, null, categoryId = "cat_transporte", categoryName = "Transporte"),
            movement("c", 15_000.0, null, categoryId = "cat_hogar", categoryName = "Hogar"),
            movement("d", 900_000.0, null, type = "INCOME", categoryId = "cat_sueldo", categoryName = "Sueldo")
        )

        val spending = ExpenseAnalyzer.spendingByCategory(movements)

        assertEquals(listOf("Transporte", "Hogar"), spending.map { it.categoryName })
        assertEquals(60_000.0, spending[0].total, 0.01)
        assertEquals(0.6, spending[0].share, 0.0001)
        assertEquals(40_000.0, spending[1].total, 0.01)
        assertEquals(2, spending[1].count)
    }

    @Test
    fun testOutstandingCountsConvertedLoansButTheyAreNotExpenses() {
        val movements = listOf(
            movement("a", 50_000.0, "x", type = "LOAN_OUT"),
            movement("b", 10_000.0, "y", type = "LOAN_IN"),
            movement("c", 5_000.0, "z"),
            movement("d", 700.0, "transfer", type = "TRANSFER")
        )

        val totals = ExpenseAnalyzer.totals(movements)

        assertEquals(5_000.0, totals.expenses, 0.01)
        assertEquals(0.0, totals.income, 0.01)
        assertEquals(45_000.0, totals.outstanding, 0.01)
    }

    @Test
    fun testSpendingByCategoryWithNoExpensesIsEmpty() {
        val income = listOf(movement("a", 10.0, null, type = "INCOME"))

        assertEquals(emptyList<CategorySpending>(), ExpenseAnalyzer.spendingByCategory(income))
    }
}
