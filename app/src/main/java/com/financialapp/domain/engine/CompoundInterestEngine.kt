package com.financialapp.domain.engine

import com.financialapp.domain.model.MonthlyCompoundRecord
import com.financialapp.domain.model.SavingsSummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.pow

/**
 * Motor Financiero de Alta Precisión para Cálculos de Interés Compuesto No Lineal.
 * 
 * Implementa la conversión estandarizada de Tasa Efectiva Anual (TEA) a
 * Tasa Efectiva Mensual (TEM) según la normativa bancaria internacional:
 * 
 * TEM = (1 + TEA)^(1/12) - 1
 */
object CompoundInterestEngine {

    /**
     * Convierte una TEA expresada en porcentaje (ej. 7.5 para 7.5%) a
     * TEM en porcentaje (ej. ~0.6045%).
     */
    fun teaToTemPercentage(teaPercentage: Double): Double {
        if (teaPercentage <= 0.0) return 0.0
        val teaDecimal = teaPercentage / 100.0
        val temDecimal = (1.0 + teaDecimal).pow(1.0 / 12.0) - 1.0
        return round(temDecimal * 100.0, 4)
    }

    /**
     * Convierte una TEA en porcentaje a factor decimal mensual directo.
     */
    fun teaToTemDecimal(teaPercentage: Double): Double {
        if (teaPercentage <= 0.0) return 0.0
        val teaDecimal = teaPercentage / 100.0
        return (1.0 + teaDecimal).pow(1.0 / 12.0) - 1.0
    }

    /**
     * Genera la tabla de amortización y crecimiento mes a mes.
     * 
     * @param initialCapital Capital con el que se abre la cuenta.
     * @param teaPercentage Tasa Efectiva Anual ofrecida por la entidad bancaria.
     * @param plannedMonthlyDeposit Aporte mensual presupuestado.
     * @param totalMonths Horizonte de ahorro en meses (ej. 12, 24, 60).
     * @param actualDepositsMap Mapa opcional de aportes reales mes a mes (mes -> monto real aportado).
     * @param depositAtStart Si el depósito se realiza al inicio del mes (capitaliza interés ese mismo mes).
     */
    fun generateSchedule(
        initialCapital: Double,
        teaPercentage: Double,
        plannedMonthlyDeposit: Double,
        totalMonths: Int,
        actualDepositsMap: Map<Int, Double> = emptyMap(),
        depositAtStart: Boolean = true,
        startDate: LocalDate = LocalDate.now()
    ): SavingsSummary {
        val temDecimal = teaToTemDecimal(teaPercentage)
        val temPercentage = teaToTemPercentage(teaPercentage)

        val records = mutableListOf<MonthlyCompoundRecord>()

        var currentRealBalance = initialCapital
        var currentIdealBalance = initialCapital
        var totalDeposited = initialCapital
        var totalInterestAccumulated = 0.0

        for (month in 1..totalMonths) {
            val periodDate = startDate.plusMonths((month - 1).toLong())
            val actualDeposit = actualDepositsMap[month] ?: plannedMonthlyDeposit

            val startBalanceReal = currentRealBalance
            val startBalanceIdeal = currentIdealBalance

            // Cálculo de intereses para el saldo Real
            val interestEarnedReal = if (depositAtStart) {
                // Si el depósito entra el día 1, rinde intereses junto al saldo inicial
                (startBalanceReal + actualDeposit) * temDecimal
            } else {
                // Si entra al final del mes, solo rinde el saldo previo
                startBalanceReal * temDecimal
            }

            // Cálculo de intereses para la curva Ideal esperada
            val interestEarnedIdeal = if (depositAtStart) {
                (startBalanceIdeal + plannedMonthlyDeposit) * temDecimal
            } else {
                startBalanceIdeal * temDecimal
            }

            currentRealBalance = round(startBalanceReal + actualDeposit + interestEarnedReal, 2)
            currentIdealBalance = round(startBalanceIdeal + plannedMonthlyDeposit + interestEarnedIdeal, 2)

            totalDeposited += actualDeposit
            totalInterestAccumulated += interestEarnedReal

            records.add(
                MonthlyCompoundRecord(
                    monthIndex = month,
                    date = periodDate,
                    startBalance = round(startBalanceReal, 2),
                    deposit = round(actualDeposit, 2),
                    interestEarned = round(interestEarnedReal, 2),
                    endBalance = currentRealBalance,
                    idealProjectedBalance = currentIdealBalance,
                    variance = round(currentRealBalance - currentIdealBalance, 2)
                )
            )
        }

        return SavingsSummary(
            initialCapital = initialCapital,
            monthlyContribution = plannedMonthlyDeposit,
            teaPercentage = teaPercentage,
            temPercentage = temPercentage,
            totalMonths = totalMonths,
            finalBalance = currentRealBalance,
            totalDeposited = round(totalDeposited, 2),
            totalInterestEarned = round(totalInterestAccumulated, 2),
            records = records
        )
    }

    /**
     * Utilidad para redondeo financiero seguro.
     */
    fun round(value: Double, places: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(places, RoundingMode.HALF_UP)
            .toDouble()
    }
}
