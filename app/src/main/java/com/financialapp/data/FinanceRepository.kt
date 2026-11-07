package com.financialapp.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.PendingMovementEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import com.financialapp.data.local.entities.SystemCategory
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.domain.engine.BillSchedule
import com.financialapp.widget.QuickExpenseGlanceWidget
import java.time.LocalDate

/** Cambios que permite el diálogo de edición de un movimiento. */
data class TransactionEdit(
    val amount: Double,
    val type: String,
    val categoryId: String,
    val note: String,
    val date: Long,
    val accountId: String?,
    val personName: String?
)

/** Todas las escrituras de movimientos pasan por aquí para que el widget quede siempre al día. */
class FinanceRepository(
    private val context: Context,
    private val db: AppDatabase
) {

    private suspend fun refreshWidget() {
        runCatching { QuickExpenseGlanceWidget().updateAll(context) }
    }

    suspend fun addMovement(
        amount: Double,
        type: String,
        categoryId: String,
        note: String?,
        date: Long = System.currentTimeMillis(),
        accountId: String? = null,
        source: String = "MANUAL"
    ) {
        db.transactionDao().insertTransaction(
            TransactionEntity(
                amount = amount, type = type, categoryId = categoryId, note = note,
                date = date, accountId = accountId, source = source
            )
        )
        refreshWidget()
    }

    suspend fun updateTransaction(id: String, edit: TransactionEdit) {
        val existing = db.transactionDao().getTransactionById(id) ?: return
        db.transactionDao().updateTransaction(
            existing.copy(
                amount = edit.amount,
                type = edit.type,
                categoryId = edit.categoryId,
                note = edit.note,
                date = edit.date,
                accountId = edit.accountId,
                personName = edit.personName
            )
        )
        refreshWidget()
    }

    suspend fun deleteTransaction(id: String) {
        val existing = db.transactionDao().getTransactionById(id) ?: return
        db.transactionDao().deleteTransaction(existing)
        refreshWidget()
    }

    /** También sirve para pagar una tarjeta: mueve la deuda sin contar como gasto. */
    suspend fun transfer(fromAccountId: String, toAccountId: String, amount: Double, date: Long, note: String?) {
        addTransaction(
            TransactionEntity(
                amount = amount,
                type = MovementType.TRANSFER,
                categoryId = SystemCategory.TRANSFER,
                note = note,
                date = date,
                accountId = fromAccountId,
                toAccountId = toAccountId
            )
        )
    }

    /** Lleva el saldo calculado al saldo real con un ajuste visible que no cuenta como gasto ni ingreso. */
    suspend fun reconcile(account: AccountEntity, calculatedBalance: Double, realBalance: Double) {
        val difference = realBalance - calculatedBalance
        if (difference != 0.0) {
            addTransaction(
                TransactionEntity(
                    amount = difference,
                    type = MovementType.ADJUSTMENT,
                    categoryId = SystemCategory.ADJUSTMENT,
                    note = "Ajuste al cuadrar ${account.name}",
                    accountId = account.id
                )
            )
        }
        db.accountDao().upsert(account.copy(lastReconciledAt = System.currentTimeMillis()))
    }

    suspend fun recordLoan(person: String, amount: Double, moneyOut: Boolean, accountId: String?, date: Long, note: String?) {
        addTransaction(
            TransactionEntity(
                amount = amount,
                type = if (moneyOut) MovementType.LOAN_OUT else MovementType.LOAN_IN,
                categoryId = SystemCategory.LOAN,
                note = note,
                date = date,
                accountId = accountId,
                personName = person.trim()
            )
        )
    }

    /** Convierte gastos e ingresos ya registrados en movimientos de préstamo con esa persona. */
    suspend fun convertToLoans(ids: List<String>, person: String) {
        ids.forEach { id ->
            val existing = db.transactionDao().getTransactionById(id) ?: return@forEach
            val type = when (existing.type) {
                MovementType.EXPENSE -> MovementType.LOAN_OUT
                MovementType.INCOME -> MovementType.LOAN_IN
                else -> return@forEach
            }
            db.transactionDao().updateTransaction(
                existing.copy(type = type, categoryId = SystemCategory.LOAN, personName = person.trim())
            )
        }
        refreshWidget()
    }

    suspend fun recordSavingsDeposit(amount: Double, fromAccountId: String?) {
        val account = db.savingsDao().getPrimaryAccount() ?: return
        db.savingsDao().insertDeposit(SavingsDepositEntity(accountId = account.id, amount = amount))
        db.savingsDao().updateAccount(account.copy(initialBalance = account.initialBalance + amount))
        addTransaction(
            TransactionEntity(
                amount = amount,
                type = MovementType.SAVINGS,
                categoryId = SystemCategory.SAVINGS,
                note = "Ahorro en ${account.bankName}",
                accountId = fromAccountId
            )
        )
    }

    suspend fun confirmPending(pending: PendingMovementEntity, edit: TransactionEdit) {
        addTransaction(
            TransactionEntity(
                amount = edit.amount,
                type = edit.type,
                categoryId = edit.categoryId,
                note = edit.note,
                date = edit.date,
                accountId = edit.accountId,
                personName = edit.personName,
                source = "NOTIFICATION"
            )
        )
        db.pendingMovementDao().delete(pending.id)
    }

    suspend fun discardPending(id: String) = db.pendingMovementDao().delete(id)

    suspend fun saveAccount(account: AccountEntity) = db.accountDao().upsert(account)

    /** Una cuenta con movimientos se archiva para no alterar el historial. */
    suspend fun removeAccount(account: AccountEntity) {
        if (db.transactionDao().countForAccount(account.id) > 0) {
            db.accountDao().upsert(account.copy(archived = true))
        } else {
            db.accountDao().delete(account.id)
        }
    }

    suspend fun saveBudget(categoryId: String, limit: Double) = db.budgetDao().upsert(BudgetEntity(categoryId, limit))
    suspend fun deleteBudget(categoryId: String) = db.budgetDao().delete(categoryId)

    suspend fun saveBill(bill: RecurringBillEntity) = db.billDao().upsert(bill)
    suspend fun deleteBill(bill: RecurringBillEntity) = db.billDao().delete(bill)

    /** Registra el gasto real y marca el vencimiento como pagado. */
    suspend fun payBill(bill: RecurringBillEntity, amount: Double, accountId: String?, today: LocalDate = LocalDate.now()) {
        val due = BillSchedule.nextDue(bill, today)
        addMovement(amount, MovementType.EXPENSE, bill.categoryId, bill.name, accountId = accountId)
        db.billDao().upsert(bill.copy(lastPaidMonth = BillSchedule.monthKey(due.dueDate)))
    }

    suspend fun saveGoal(goal: GoalEntity) = db.goalDao().upsert(goal)
    suspend fun deleteGoal(goal: GoalEntity) = db.goalDao().delete(goal)

    suspend fun saveCategory(category: CategoryEntity) = db.categoryDao().insertCategories(listOf(category))

    /** Devuelve false si la categoría está en uso: no se borra para no dejar movimientos sin categoría. */
    suspend fun deleteCategory(category: CategoryEntity): Boolean {
        if (db.categoryDao().countUses(category.id) > 0) return false
        db.categoryDao().deleteCategory(category.id)
        return true
    }

    private suspend fun addTransaction(transaction: TransactionEntity) {
        db.transactionDao().insertTransaction(transaction)
        refreshWidget()
    }
}
