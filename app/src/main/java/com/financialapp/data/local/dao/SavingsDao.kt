package com.financialapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: SavingsAccountEntity)

    @Update
    suspend fun updateAccount(account: SavingsAccountEntity)

    @Query("SELECT * FROM savings_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<SavingsAccountEntity>>

    @Query("SELECT * FROM savings_accounts LIMIT 1")
    suspend fun getPrimaryAccount(): SavingsAccountEntity?

    @Query("SELECT * FROM savings_accounts LIMIT 1")
    fun getPrimaryAccountFlow(): Flow<SavingsAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: SavingsDepositEntity)

    @Delete
    suspend fun deleteDeposit(deposit: SavingsDepositEntity)

    @Query("SELECT * FROM savings_deposits WHERE accountId = :accountId ORDER BY depositDate ASC")
    fun getDepositsForAccount(accountId: String): Flow<List<SavingsDepositEntity>>

    @Query("SELECT * FROM savings_deposits WHERE accountId = :accountId ORDER BY depositDate ASC")
    suspend fun getDepositsListForAccount(accountId: String): List<SavingsDepositEntity>
}
