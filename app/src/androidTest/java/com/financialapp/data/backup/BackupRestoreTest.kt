package com.financialapp.data.backup

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import com.financialapp.data.local.entities.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRestoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seed() {
        val categoryDao = db.categoryDao()
        categoryDao.insertCategories(listOf(CategoryEntity(id = "c", name = "Comida", iconName = "", colorHex = "#000000", type = "EXPENSE")))
        db.accountDao().upsert(AccountEntity(id = "a", name = "Nequi", type = "BANK", openingBalance = 100.0))
        db.transactionDao().insertTransaction(TransactionEntity(id = "t", amount = 10.0, type = "EXPENSE", categoryId = "c", accountId = "a"))
        db.budgetDao().upsert(BudgetEntity("c", 500.0))
        db.billDao().upsert(RecurringBillEntity(id = "b", name = "Arriendo", amount = 1.0, dayOfMonth = 5, categoryId = "c"))
        db.goalDao().upsert(GoalEntity(id = "g", name = "Moto", targetAmount = 9.0))
        db.savingsDao().insertAccount(SavingsAccountEntity(id = "s", name = "Ahorro", bankName = "Banco", teaRate = 10.0))
        db.savingsDao().insertDeposit(SavingsDepositEntity(id = "d", accountId = "s", amount = 5.0))
    }

    @Test
    fun restoreReplacesEverythingAndKeepsReferencesValid() = runBlocking {
        seed()
        val manager = BackupManager(context, db)
        val backup = manager.snapshot()

        // Cambios posteriores a la copia que la restauración debe deshacer
        db.transactionDao().insertTransaction(TransactionEntity(id = "t2", amount = 99.0, type = "EXPENSE", categoryId = "c"))
        db.goalDao().upsert(GoalEntity(id = "g2", name = "Otra", targetAmount = 1.0))

        manager.restore(BackupCodec.decode(BackupCodec.encode(backup)).getOrThrow())

        assertEquals(listOf("t"), db.transactionDao().getAllEntities().map { it.id })
        assertEquals(listOf("g"), db.goalDao().all().map { it.id })
        assertEquals(1, db.billDao().all().size)
        assertEquals(1, db.budgetDao().all().size)
        assertEquals(1, db.savingsDao().getDepositsListForAccount("s").size)
        // Las categorías internas vuelven aunque la copia no las traiga
        assertTrue(db.categoryDao().getAllCategories().any { it.type == "SYSTEM" })
    }

    @Test
    fun invalidBackupLeavesCurrentDataUntouched() = runBlocking {
        seed()
        val manager = BackupManager(context, db)
        val broken = manager.snapshot().copy(
            transactions = listOf(TransactionEntity(amount = 1.0, type = "EXPENSE", categoryId = "no-existe"))
        )

        val failed = runCatching { manager.restore(broken) }.isFailure

        assertTrue(failed)
        assertEquals(listOf("t"), db.transactionDao().getAllEntities().map { it.id })
    }
}
