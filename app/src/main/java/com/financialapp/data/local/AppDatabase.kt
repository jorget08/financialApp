package com.financialapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financialapp.data.local.dao.CategoryDao
import com.financialapp.data.local.dao.SavingsDao
import com.financialapp.data.local.dao.TransactionDao
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import com.financialapp.data.local.entities.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        SavingsAccountEntity::class,
        SavingsDepositEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingsDao(): SavingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financial_app.db"
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabasePrepopulateCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        // Categorías solicitadas por el usuario para Colombia (COP)
                        val defaultCategories = listOf(
                            CategoryEntity(id = "cat_casa_servicios", name = "Casa y Servicios", iconName = "bolt", colorHex = "#9C27B0", type = "EXPENSE", displayOrder = 1),
                            CategoryEntity(id = "cat_hogar", name = "Hogar", iconName = "home", colorHex = "#FF9800", type = "EXPENSE", displayOrder = 2),
                            CategoryEntity(id = "cat_salud", name = "Salud y Bienestar", iconName = "medical_services", colorHex = "#4CAF50", type = "EXPENSE", displayOrder = 3),
                            CategoryEntity(id = "cat_ocio", name = "Entretenimiento y Ocio", iconName = "movie", colorHex = "#E91E63", type = "EXPENSE", displayOrder = 4),
                            CategoryEntity(id = "cat_transporte", name = "Transporte", iconName = "directions_car", colorHex = "#2196F3", type = "EXPENSE", displayOrder = 5),
                            CategoryEntity(id = "cat_otros_gastos", name = "Otros Gastos", iconName = "more_horiz", colorHex = "#607D8B", type = "EXPENSE", displayOrder = 6),
                            CategoryEntity(id = "cat_sueldo", name = "Sueldo / Salario", iconName = "payments", colorHex = "#4CAF50", type = "INCOME", displayOrder = 1),
                            CategoryEntity(id = "cat_honorarios", name = "Honorarios", iconName = "work", colorHex = "#2196F3", type = "INCOME", displayOrder = 2),
                            CategoryEntity(id = "cat_inversion", name = "Inversiones", iconName = "trending_up", colorHex = "#9C27B0", type = "INCOME", displayOrder = 3),
                            CategoryEntity(id = "cat_otros_ingresos", name = "Otros Ingresos", iconName = "add_circle", colorHex = "#8BC34A", type = "INCOME", displayOrder = 4)
                        )
                        database.categoryDao().insertCategories(defaultCategories)
                        // NOTA: NO se precarga ninguna cuenta de ahorro ficticia.
                        // El usuario configurará su banco, saldo real y tasa E.A. desde cero.
                    }
                }
            }
        }
    }
}
