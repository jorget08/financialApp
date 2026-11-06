package com.financialapp.domain.engine

import com.financialapp.capture.DailyCheckWorker
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.AccountType
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinanceCalculatorsTest {

    private fun movement(
        type: String,
        amount: Double,
        accountId: String? = "nequi",
        toAccountId: String? = null,
        person: String? = null,
        date: Long = 1_000L,
        categoryId: String = "c"
    ) = TransactionEntity(
        amount = amount, type = type, categoryId = categoryId, accountId = accountId,
        toAccountId = toAccountId, personName = person, date = date
    )

    private fun account(id: String, opening: Double, openingDate: Long = 0L, type: String = AccountType.BANK) =
        AccountEntity(id = id, name = id, type = type, openingBalance = opening, openingDate = openingDate)

    @Test
    fun testBalanceAppliesEveryMovementType() {
        val nequi = account("nequi", 100_000.0)
        val movements = listOf(
            movement(MovementType.EXPENSE, 10_000.0),
            movement(MovementType.INCOME, 50_000.0),
            movement(MovementType.LOAN_OUT, 20_000.0),
            movement(MovementType.LOAN_IN, 5_000.0),
            movement(MovementType.SAVINGS, 15_000.0),
            movement(MovementType.ADJUSTMENT, -1_000.0),
            movement(MovementType.EXPENSE, 999.0, accountId = "otra")
        )

        assertEquals(109_000.0, AccountLedger.balance(nequi, movements), 0.01)
    }

    @Test
    fun testCardPaymentLowersCashAndDebtWithoutBeingAnExpense() {
        val nequi = account("nequi", 500_000.0)
        val card = account("cmr", -200_000.0, type = AccountType.CREDIT_CARD)
        val movements = listOf(
            movement(MovementType.EXPENSE, 80_000.0, accountId = "cmr"),
            movement(MovementType.TRANSFER, 150_000.0, accountId = "nequi", toAccountId = "cmr")
        )

        assertEquals(350_000.0, AccountLedger.balance(nequi, movements), 0.01)
        assertEquals(-130_000.0, AccountLedger.balance(card, movements), 0.01)
    }

    @Test
    fun testForgottenExpenseFromBeforeOpeningBalanceIsNotSubtractedTwice() {
        val nequi = account("nequi", 100_000.0, openingDate = 5_000L)
        val movements = listOf(
            movement(MovementType.EXPENSE, 30_000.0, date = 4_999L),
            movement(MovementType.EXPENSE, 10_000.0, date = 5_000L)
        )

        assertEquals(90_000.0, AccountLedger.balance(nequi, movements), 0.01)
    }

    @Test
    fun testLoansGroupSpellingsAndSupportBothDirections() {
        val movements = listOf(
            movement(MovementType.LOAN_OUT, 50_000.0, person = "Camilo", date = 1L),
            movement(MovementType.LOAN_OUT, 30_000.0, person = "CAMILO ", date = 2L),
            movement(MovementType.LOAN_IN, 25_000.0, person = "camilo", date = 3L),
            movement(MovementType.LOAN_IN, 200_000.0, person = "Andrés", date = 4L),
            movement(MovementType.EXPENSE, 9_000.0, person = "Ignorado")
        )

        val loans = LoanLedger.summarize(movements)

        assertEquals(2, loans.size)
        assertEquals("Andrés", loans[0].personName)
        assertEquals(-200_000.0, loans[0].balance, 0.01)
        assertEquals(55_000.0, loans[1].balance, 0.01)
        assertEquals(3, loans[1].movementCount)
    }

    @Test
    fun testBudgetLevelsAtThresholdsAndOnlyCountExpensesInRange() {
        val categories = listOf("a", "b", "c").map { CategoryEntity(id = it, name = it, iconName = "", colorHex = "", type = "EXPENSE") }
        val budgets = categories.map { BudgetEntity(it.id, 100.0) }
        val movements = listOf(
            movement(MovementType.EXPENSE, 79.0, categoryId = "a"),
            movement(MovementType.EXPENSE, 80.0, categoryId = "b"),
            movement(MovementType.EXPENSE, 100.0, categoryId = "c"),
            movement(MovementType.EXPENSE, 500.0, categoryId = "a", date = 9_000L),
            movement(MovementType.LOAN_OUT, 500.0, categoryId = "a")
        )

        val byId = BudgetCalculator.statuses(budgets, categories, movements, 0L to 2_000L).associateBy { it.category.id }

        assertEquals(BudgetLevel.OK, byId.getValue("a").level)
        assertEquals(BudgetLevel.WARNING, byId.getValue("b").level)
        assertEquals(BudgetLevel.OVER, byId.getValue("c").level)
    }

    private fun bill(day: Int, lastPaid: String? = null) =
        RecurringBillEntity(name = "x", amount = 1.0, dayOfMonth = day, categoryId = "c", lastPaidMonth = lastPaid)

    @Test
    fun testBillDueDatesHandleOverdueAndPaidMonths() {
        val today = LocalDate.of(2026, 11, 15)

        val upcoming = BillSchedule.nextDue(bill(20), today)
        assertEquals(LocalDate.of(2026, 11, 20), upcoming.dueDate)
        assertEquals(5L, upcoming.daysUntil)

        val overdue = BillSchedule.nextDue(bill(10), today)
        assertTrue(overdue.isOverdue)

        val paid = BillSchedule.nextDue(bill(10, "2026-11"), today)
        assertEquals(LocalDate.of(2026, 12, 10), paid.dueDate)
        assertFalse(paid.isOverdue)
    }

    @Test
    fun testBillDay31FallsOnLastDayOfShortMonths() {
        val due = BillSchedule.nextDue(bill(31), LocalDate.of(2027, 2, 1))

        assertEquals(LocalDate.of(2027, 2, 28), due.dueDate)
    }

    @Test
    fun testReviewExcludesLoansFromExpensesAndComparesPeriods() {
        val movements = listOf(
            movement(MovementType.INCOME, 1_000.0, date = 150L),
            movement(MovementType.EXPENSE, 300.0, date = 150L, categoryId = "food"),
            movement(MovementType.SAVINGS, 100.0, date = 150L),
            movement(MovementType.LOAN_OUT, 400.0, date = 150L),
            movement(MovementType.EXPENSE, 100.0, date = 50L, categoryId = "food"),
            movement(MovementType.EXPENSE, 200.0, date = 50L, categoryId = "fun")
        )
        val names = mapOf("food" to "Comida", "fun" to "Ocio")

        val reviews = MonthlyReview.build(movements, names, listOf("Nov" to (100L to 200L), "Oct" to (0L to 100L)))

        assertEquals(300.0, reviews[0].expenses, 0.01)
        assertEquals(100.0, reviews[0].saved, 0.01)
        assertEquals(0.7, reviews[0].savingsRate, 0.001)
        val changes = MonthlyReview.changes(reviews[0], reviews[1])
        assertEquals("Comida", changes[0].categoryName)
        assertEquals(200.0, changes[0].difference, 0.01)
        assertEquals(-200.0, changes[1].difference, 0.01)
    }

    @Test
    fun testNightlyCheckRunsTodayBeforeTheHourAndTomorrowAfterIt() {
        val day = LocalDate.of(2026, 11, 6)

        assertEquals(30L, DailyCheckWorker.delayUntilNextRun(day.atTime(20, 0)).toMinutes())
        assertEquals(24 * 60L, DailyCheckWorker.delayUntilNextRun(day.atTime(20, 30)).toMinutes())
        assertEquals(23 * 60L + 30, DailyCheckWorker.delayUntilNextRun(day.atTime(21, 0)).toMinutes())
    }

    @Test
    fun testEmergencyFundUsesCompletePeriodsOnly() {
        val reviews = listOf(
            PeriodReview("Actual", 0.0, 50.0, 0.0, emptyMap()),
            PeriodReview("Ant", 0.0, 1_000.0, 0.0, emptyMap()),
            PeriodReview("Ant2", 0.0, 2_000.0, 0.0, emptyMap())
        )

        val average = MonthlyReview.averageExpenses(reviews)

        assertEquals(1_500.0, average, 0.01)
        assertEquals(2.0, MonthlyReview.emergencyMonths(3_000.0, average)!!, 0.01)
        assertNull(MonthlyReview.emergencyMonths(3_000.0, 0.0))
    }
}
