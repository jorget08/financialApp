package com.financialapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financialapp.data.local.dao.AccountDao
import com.financialapp.data.local.dao.BackupDao
import com.financialapp.data.local.dao.BillDao
import com.financialapp.data.local.dao.BudgetDao
import com.financialapp.data.local.dao.CategoryDao
import com.financialapp.data.local.dao.GoalDao
import com.financialapp.data.local.dao.PendingMovementDao
import com.financialapp.data.local.dao.SavingsDao
import com.financialapp.data.local.dao.SuggestionDao
import com.financialapp.data.local.dao.TransactionDao
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.AiSuggestionEntity
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.PendingMovementEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import com.financialapp.data.local.entities.SystemCategory
import com.financialapp.data.local.entities.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        SavingsAccountEntity::class,
        SavingsDepositEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        RecurringBillEntity::class,
        GoalEntity::class,
        PendingMovementEntity::class,
        AiSuggestionEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingsDao(): SavingsDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun billDao(): BillDao
    abstract fun goalDao(): GoalDao
    abstract fun pendingMovementDao(): PendingMovementDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun backupDao(): BackupDao

    companion object {
        private const val DB_NAME = "financial_app.db"
        private const val SCHEMA_VERSION = 3

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val appContext = context.applicationContext
                    copyBeforeUpgrade(appContext)
                    val instance = Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                        .addCallback(DatabasePrepopulateCallback())
                        .addMigrations(MIGRATION_2_3)
                        // v1 no conservó esquema para migrarla; copyBeforeUpgrade ya guardó el archivo original
                        .fallbackToDestructiveMigrationFrom(1)
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }

        /**
         * Copia intacta de la base antes de abrirla con un esquema nuevo. Se hace una sola vez
         * por versión para que un fallo de migración nunca signifique perder el historial.
         */
        private fun copyBeforeUpgrade(context: Context) {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return
            val backupDir = File(context.filesDir, "pre_upgrade_v$SCHEMA_VERSION")
            if (backupDir.exists()) return
            runCatching {
                backupDir.mkdirs()
                listOf("", "-wal", "-shm").forEach { suffix ->
                    val source = File(dbFile.path + suffix)
                    if (source.exists()) source.copyTo(File(backupDir, source.name), overwrite = true)
                }
            }
        }

        // Categorías solicitadas por el usuario para Colombia (COP)
        private val defaultCategories = listOf(
            CategoryEntity(id = "cat_casa_servicios", name = "Casa y Servicios", iconName = "bolt", colorHex = "#9C27B0", type = "EXPENSE", displayOrder = 1),
            CategoryEntity(id = "cat_hogar", name = "Hogar", iconName = "home", colorHex = "#FF9800", type = "EXPENSE", displayOrder = 2),
            CategoryEntity(id = "cat_salud", name = "Salud y Bienestar", iconName = "medical_services", colorHex = "#4CAF50", type = "EXPENSE", displayOrder = 3),
            CategoryEntity(id = "cat_ocio", name = "Entretenimiento y Ocio", iconName = "movie", colorHex = "#E91E63", type = "EXPENSE", displayOrder = 4),
            CategoryEntity(id = "cat_transporte", name = "Transporte", iconName = "directions_car", colorHex = "#2196F3", type = "EXPENSE", displayOrder = 5),
            CategoryEntity(id = "cat_otros_gastos", name = "Otros Gastos", iconName = "more_horiz", colorHex = "#607D8B", type = "EXPENSE", displayOrder = 6),
            CategoryEntity(id = "cat_comida", name = "Comida", iconName = "restaurant", colorHex = "#FF5722", type = "EXPENSE", displayOrder = 7),
            CategoryEntity(id = "cat_sueldo", name = "Sueldo / Salario", iconName = "payments", colorHex = "#4CAF50", type = "INCOME", displayOrder = 1),
            CategoryEntity(id = "cat_honorarios", name = "Honorarios", iconName = "work", colorHex = "#2196F3", type = "INCOME", displayOrder = 2),
            CategoryEntity(id = "cat_inversion", name = "Inversiones", iconName = "trending_up", colorHex = "#9C27B0", type = "INCOME", displayOrder = 3),
            CategoryEntity(id = "cat_otros_ingresos", name = "Otros Ingresos", iconName = "add_circle", colorHex = "#8BC34A", type = "INCOME", displayOrder = 4)
        )

        private val systemCategories = listOf(
            CategoryEntity(id = SystemCategory.TRANSFER, name = "Transferencia", iconName = "swap", colorHex = "#58A6FF", type = "SYSTEM"),
            CategoryEntity(id = SystemCategory.ADJUSTMENT, name = "Ajuste de saldo", iconName = "tune", colorHex = "#8B949E", type = "SYSTEM"),
            CategoryEntity(id = SystemCategory.LOAN, name = "Préstamo", iconName = "handshake", colorHex = "#F0883E", type = "SYSTEM"),
            CategoryEntity(id = SystemCategory.SAVINGS, name = "Ahorro", iconName = "savings", colorHex = "#BC8CFF", type = "SYSTEM")
        )

        /**
         * Garantiza que existan las categorías por defecto y las internas.
         * Las internas se revisan siempre porque los movimientos que no son gasto dependen de ellas.
         */
        suspend fun ensureDefaultCategories(database: AppDatabase) {
            val dao = database.categoryDao()
            if (dao.count() == 0) {
                dao.insertCategories(defaultCategories)
            }
            dao.insertMissingCategories(systemCategories)
        }

        private class DatabasePrepopulateCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seed()
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                seed()
            }

            private fun seed() {
                CoroutineScope(Dispatchers.IO).launch {
                    // NOTA: NO se precarga ninguna cuenta de ahorro ficticia.
                    // El usuario configurará su banco, saldo real y tasa E.A. desde cero.
                    INSTANCE?.let { ensureDefaultCategories(it) }
                }
            }
        }
    }
}
