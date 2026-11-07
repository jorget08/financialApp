package com.financialapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.AiSuggestionEntity
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.PendingMovementEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import com.financialapp.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY createdAt ASC")
    fun activeFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY createdAt ASC")
    suspend fun active(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun all(): List<AccountEntity>
}

@Dao
interface BudgetDao {
    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun delete(categoryId: String)

    @Query("SELECT * FROM budgets")
    fun flow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun all(): List<BudgetEntity>
}

@Dao
interface BillDao {
    @Upsert
    suspend fun upsert(bill: RecurringBillEntity)

    @Delete
    suspend fun delete(bill: RecurringBillEntity)

    @Query("SELECT * FROM recurring_bills ORDER BY dayOfMonth ASC")
    fun flow(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills ORDER BY dayOfMonth ASC")
    suspend fun all(): List<RecurringBillEntity>
}

@Dao
interface GoalDao {
    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals ORDER BY priority ASC, createdAt ASC")
    fun flow(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY priority ASC, createdAt ASC")
    suspend fun all(): List<GoalEntity>
}

@Dao
interface PendingMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pending: PendingMovementEntity)

    @Query("DELETE FROM pending_movements WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM pending_movements ORDER BY postedAt DESC")
    fun flow(): Flow<List<PendingMovementEntity>>

    @Query("SELECT COUNT(*) FROM pending_movements")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM pending_movements WHERE dedupeKey = :key AND postedAt >= :since")
    suspend fun countRecent(key: String, since: Long): Int
}

@Dao
interface SuggestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suggestion: AiSuggestionEntity)

    @Query("DELETE FROM ai_suggestions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM ai_suggestions ORDER BY createdAt DESC LIMIT 30")
    fun flow(): Flow<List<AiSuggestionEntity>>
}

/** Lectura y reemplazo completo de los datos para las copias de seguridad. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM categories") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM transactions") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM savings_accounts") suspend fun savingsAccounts(): List<SavingsAccountEntity>
    @Query("SELECT * FROM savings_deposits") suspend fun savingsDeposits(): List<SavingsDepositEntity>

    @Query("DELETE FROM recurring_bills") suspend fun clearBills()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM savings_deposits") suspend fun clearSavingsDeposits()
    @Query("DELETE FROM savings_accounts") suspend fun clearSavingsAccounts()

    @Insert suspend fun insertCategories(items: List<CategoryEntity>)
    @Insert suspend fun insertTransactions(items: List<TransactionEntity>)
    @Insert suspend fun insertAccounts(items: List<AccountEntity>)
    @Insert suspend fun insertBudgets(items: List<BudgetEntity>)
    @Insert suspend fun insertBills(items: List<RecurringBillEntity>)
    @Insert suspend fun insertGoals(items: List<GoalEntity>)
    @Insert suspend fun insertSavingsAccounts(items: List<SavingsAccountEntity>)
    @Insert suspend fun insertSavingsDeposits(items: List<SavingsDepositEntity>)
}
