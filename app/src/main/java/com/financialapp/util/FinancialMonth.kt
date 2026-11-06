package com.financialapp.util

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Mes financiero configurable: permite que el "mes" empiece el día de pago
 * (ej. día 27) en lugar del día 1, para que el sueldo y los gastos de fin de mes
 * cuenten en el mes que realmente financian.
 */
object FinancialMonth {

    private const val PREFS_NAME = "financial_prefs"
    private const val KEY_START_DAY = "month_start_day"

    // Máximo 28 para que el día exista en todos los meses
    const val MAX_START_DAY = 28

    private val locale = Locale("es", "CO")

    fun getStartDay(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_START_DAY, 1)
            .coerceIn(1, MAX_START_DAY)

    fun saveStartDay(context: Context, day: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_START_DAY, day.coerceIn(1, MAX_START_DAY)).apply()
    }

    /** Primer día del periodo actual (inclusive). */
    fun currentPeriodStart(startDay: Int, today: LocalDate = LocalDate.now()): LocalDate {
        val thisMonthStart = today.withDayOfMonth(startDay)
        return if (today.dayOfMonth >= startDay) thisMonthStart else thisMonthStart.minusMonths(1)
    }

    /** Timestamps [inicio, fin) del periodo actual. */
    fun currentPeriodRange(startDay: Int): Pair<Long, Long> = rangeFrom(currentPeriodStart(startDay))

    /** Timestamps [inicio, fin) del periodo anterior al actual. */
    fun previousPeriodRange(startDay: Int): Pair<Long, Long> =
        rangeFrom(currentPeriodStart(startDay).minusMonths(1))

    /** Periodo que empezó [monthsBack] meses antes del actual, con su nombre. */
    fun periodAt(startDay: Int, monthsBack: Int, today: LocalDate = LocalDate.now()): Pair<String, Pair<Long, Long>> {
        val start = currentPeriodStart(startDay, today).minusMonths(monthsBack.toLong())
        val named = if (startDay > 15) start.plusMonths(1) else start
        val label = named.format(DateTimeFormatter.ofPattern("MMM yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
        return label to rangeFrom(start)
    }

    private fun rangeFrom(start: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        return Pair(
            start.atStartOfDay(zone).toInstant().toEpochMilli(),
            start.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }

    /**
     * Nombre del periodo: si empieza en la segunda mitad del mes, lleva el nombre
     * del mes siguiente (27 oct – 26 nov = "Noviembre").
     */
    fun currentPeriodLabel(startDay: Int): String {
        val start = currentPeriodStart(startDay)
        val named = if (startDay > 15) start.plusMonths(1) else start
        val month = named.format(DateTimeFormatter.ofPattern("MMMM", locale))
            .replaceFirstChar { it.titlecase(locale) }
        if (startDay == 1) return month
        val rangeFormat = DateTimeFormatter.ofPattern("d MMM", locale)
        val end = start.plusMonths(1).minusDays(1)
        return "$month (${start.format(rangeFormat)} – ${end.format(rangeFormat)})"
    }
}
