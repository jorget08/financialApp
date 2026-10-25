package com.financialapp.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formateador de Moneda para Pesos Colombianos (COP).
 * Ejemplo: 15000 -> "$ 15.000", 2500000 -> "$ 2.500.000"
 */
object CurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("$ #,###", symbols)

    fun format(amount: Double): String {
        return formatter.format(amount.toLong())
    }

    fun formatWithSign(amount: Double, isExpense: Boolean): String {
        val sign = if (isExpense) "-" else "+"
        return "$sign${formatter.format(amount.toLong())}"
    }

    fun formatCompact(amount: Double): String {
        val abs = Math.abs(amount)
        return when {
            abs >= 1_000_000 -> "$ ${String.format(Locale.US, "%.1f", amount / 1_000_000)}M"
            abs >= 1_000 -> "$ ${String.format(Locale.US, "%.0f", amount / 1_000)}k"
            else -> "$ ${amount.toLong()}"
        }
    }
}
