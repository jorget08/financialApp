package com.financialapp.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompoundInterestEngineTest {

    @Test
    fun testTeaToTemConversion() {
        // TEA = 7.5% -> TEM = (1 + 0.075)^(1/12) - 1 ≈ 0.6045%
        val tem = CompoundInterestEngine.teaToTemPercentage(7.5)
        assertEquals(0.6045, tem, 0.001)

        // TEA = 10% -> TEM ≈ 0.7974%
        val tem10 = CompoundInterestEngine.teaToTemPercentage(10.0)
        assertEquals(0.7974, tem10, 0.001)
    }

    @Test
    fun testCompoundInterestAcceleration() {
        // Con aporte constante, los intereses del mes N deben ser mayores a los del mes N-1 (crecimiento exponencial)
        val schedule = CompoundInterestEngine.generateSchedule(
            initialCapital = 1000.0,
            teaPercentage = 8.0,
            plannedMonthlyDeposit = 200.0,
            totalMonths = 12
        )

        assertEquals(12, schedule.records.size)

        var previousInterest = 0.0
        for (record in schedule.records) {
            assertTrue(
                "El interés del mes ${record.monthIndex} (${record.interestEarned}) debe ser mayor al del mes anterior ($previousInterest)",
                record.interestEarned > previousInterest
            )
            previousInterest = record.interestEarned
        }
    }
}
