package com.financialapp.capture

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.financialapp.data.backup.BackupManager
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.MovementType
import com.financialapp.domain.engine.BillSchedule
import com.financialapp.domain.engine.BudgetCalculator
import com.financialapp.domain.engine.BudgetLevel
import com.financialapp.util.CurrencyFormatter
import com.financialapp.util.FinancialMonth
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Revisión de cada noche: copia automática, recordatorio de registrar, pendientes,
 * pagos fijos cercanos y, los domingos, cuadre de cuentas.
 */
class DailyCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val prefs = AppPreferences(applicationContext)
        val today = LocalDate.now()

        // La copia no debe impedir los recordatorios si el almacenamiento falla
        runCatching { BackupManager(applicationContext, db).writeDaily(today) }

        val pending = db.pendingMovementDao().count()
        if (pending > 0) {
            AppNotifier.reminder(
                applicationContext, AppNotifier.ID_DAILY,
                "Tienes $pending por confirmar",
                "Confírmalos para que tus cuentas sigan cuadrando.",
                "pending"
            )
        } else if (prefs.dailyReminderEnabled) {
            val zone = ZoneId.systemDefault()
            val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            if (db.transactionDao().countBetween(start, end) == 0) {
                AppNotifier.reminder(
                    applicationContext, AppNotifier.ID_DAILY,
                    "¿Registraste todo hoy?",
                    "Hoy no hay movimientos. Si gastaste algo, anótalo ahora que lo recuerdas.",
                    "home"
                )
            }
        }

        val dueSoon = db.billDao().all()
            .map { BillSchedule.nextDue(it, today) }
            .filter { it.daysUntil <= BILL_NOTICE_DAYS }
        if (dueSoon.isNotEmpty()) {
            val names = dueSoon.joinToString { "${it.bill.name} (${CurrencyFormatter.format(it.bill.amount)})" }
            val overdue = dueSoon.any { it.isOverdue }
            AppNotifier.reminder(
                applicationContext, AppNotifier.ID_BILLS,
                if (overdue) "Tienes pagos vencidos" else "Pagos por vencer",
                names,
                "bills"
            )
        }

        if (today.dayOfWeek == DayOfWeek.SUNDAY && db.accountDao().active().isNotEmpty()) {
            AppNotifier.reminder(
                applicationContext, AppNotifier.ID_RECONCILE,
                "Cuadre semanal",
                "Compara el saldo de la app con tu saldo real: si no coinciden, falta algo por registrar.",
                "accounts"
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_check"
        private const val BILL_NOTICE_DAYS = 2L
        private val RUN_AT: LocalTime = LocalTime.of(20, 30)

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextRun(LocalDateTime.now()).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun delayUntilNextRun(now: LocalDateTime): Duration {
            val todayRun = now.toLocalDate().atTime(RUN_AT)
            val next = if (now.isBefore(todayRun)) todayRun else todayRun.plusDays(1)
            return Duration.between(now, next)
        }
    }
}

/** Avisa una sola vez por periodo y categoría al llegar al 80 % y al 100 % del presupuesto. */
object BudgetMonitor {

    suspend fun check(context: Context, db: AppDatabase) {
        val budgets = db.budgetDao().all()
        if (budgets.isEmpty()) return
        val range = FinancialMonth.currentPeriodRange(FinancialMonth.getStartDay(context))
        val transactions = db.transactionDao().getAllTransactionsSince(range.first)
            .filter { it.type == MovementType.EXPENSE }
        val prefs = AppPreferences(context)
        BudgetCalculator.statuses(budgets, db.categoryDao().getAllCategories(), transactions, range).forEach { status ->
            val (level, title) = when (status.level) {
                BudgetLevel.OVER -> "over" to "Pasaste el presupuesto de ${status.category.name}"
                BudgetLevel.WARNING -> "warning" to "Vas en el 80 % de ${status.category.name}"
                BudgetLevel.OK -> return@forEach
            }
            if (prefs.markBudgetAlert("${range.first}_${status.category.id}_$level")) {
                AppNotifier.budget(
                    context,
                    status.category.id,
                    title,
                    "Gastado ${CurrencyFormatter.format(status.spent)} de ${CurrencyFormatter.format(status.limit)}."
                )
            }
        }
    }
}
