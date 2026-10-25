package com.financialapp.domain.model

import java.time.LocalDate

/**
 * Representa el desglose financiero de un mes en la cuenta de ahorros.
 * Permite visualizar el efecto multiplicador del interés compuesto no lineal
 * y contrastar la realidad frente a la meta proyectada.
 */
data class MonthlyCompoundRecord(
    val monthIndex: Int,                   // Mes 1, 2, 3...
    val date: LocalDate,                   // Fecha del período
    val startBalance: Double,              // Saldo disponible al inicio del mes
    val deposit: Double,                   // Aporte/depósito realizado este mes
    val interestEarned: Double,            // Intereses reales generados este mes
    val endBalance: Double,                // Saldo acumulado real al cierre
    val idealProjectedBalance: Double,     // Proyección matemática ideal esperada
    val variance: Double = endBalance - idealProjectedBalance // Desvío (+ si ahorró más, - si faltó)
) {
    val totalContributionsToDate: Double
        get() = startBalance + deposit // Capital base acumulado
}

/**
 * Resumen consolidado del horizonte de ahorros.
 */
data class SavingsSummary(
    val initialCapital: Double,
    val monthlyContribution: Double,
    val teaPercentage: Double,
    val temPercentage: Double,
    val totalMonths: Int,
    val finalBalance: Double,
    val totalDeposited: Double,
    val totalInterestEarned: Double,
    val records: List<MonthlyCompoundRecord>
)
