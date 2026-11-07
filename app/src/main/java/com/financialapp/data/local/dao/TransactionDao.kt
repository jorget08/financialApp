package com.financialapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financialapp.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithCategory(
    val id: String,
    val amount: Double,
    val type: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val note: String?,
    val date: Long,
    val source: String,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val personName: String? = null
)

data class CategoryExpenseSummary(
    val categoryName: String,
    val totalAmount: Double,
    val transactionCount: Int
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("""
        SELECT t.id, t.amount, t.type, t.categoryId, c.name as categoryName, 
               c.iconName as categoryIcon, c.colorHex as categoryColor, 
               t.note, t.date, t.source, t.accountId, t.toAccountId, t.personName
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC
        LIMIT 50
    """)
    fun getRecentTransactions(): Flow<List<TransactionWithCategory>>

    @Query("""
        SELECT t.id, t.amount, t.type, t.categoryId, c.name as categoryName,
               c.iconName as categoryIcon, c.colorHex as categoryColor,
               t.note, t.date, t.source, t.accountId, t.toAccountId, t.personName
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC
    """)
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) 
        FROM transactions 
        WHERE type = 'EXPENSE' AND date >= :startTimestamp AND date <= :endTimestamp
    """)
    fun getTotalExpensesPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) 
        FROM transactions 
        WHERE type = 'INCOME' AND date >= :startTimestamp AND date <= :endTimestamp
    """)
    fun getTotalIncomesPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("""
        SELECT c.name as categoryName, SUM(t.amount) as totalAmount, COUNT(t.id) as transactionCount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' AND t.date >= :startTimestamp
        GROUP BY c.name
        ORDER BY totalAmount DESC
    """)
    suspend fun getExpensesByCategorySince(startTimestamp: Long): List<CategoryExpenseSummary>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM transactions
        WHERE type = 'SAVINGS' AND date >= :startTimestamp AND date < :endTimestamp
    """)
    fun getTotalSavedPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllEntitiesFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllEntities(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE date >= :startTimestamp AND date < :endTimestamp")
    suspend fun countBetween(startTimestamp: Long, endTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun countForAccount(accountId: String): Int

    @Query("SELECT * FROM transactions WHERE date >= :startTimestamp ORDER BY date DESC")
    suspend fun getAllTransactionsSince(startTimestamp: Long): List<TransactionEntity>
}
