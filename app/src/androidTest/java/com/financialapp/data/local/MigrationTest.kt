package com.financialapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate2To3KeepsEveryExistingRow() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, type, displayOrder) VALUES ('cat_otros_gastos', 'Otros Gastos', 'x', '#607D8B', 'EXPENSE', 6)")
            db.execSQL("INSERT INTO transactions (id, amount, type, categoryId, note, date, source, createdAt) VALUES ('t1', 50000.0, 'EXPENSE', 'cat_otros_gastos', 'Préstamo a Camilo', 1000, 'MANUAL', 1000)")
            db.execSQL("INSERT INTO savings_accounts (id, name, bankName, teaRate, currency, initialBalance, plannedMonthlyDeposit, startDate, createdAt) VALUES ('s1', 'Ahorro', 'Banco', 10.0, 'COP', 2000000.0, 100000.0, 1000, 1000)")
            db.execSQL("INSERT INTO savings_deposits (id, accountId, amount, depositDate, isSimulated, note) VALUES ('d1', 's1', 100000.0, 1000, 0, NULL)")
        }

        // runMigrationsAndValidate falla si el esquema resultante no coincide con el de las entidades
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT amount, note, accountId, personName FROM transactions WHERE id = 't1'").use { row ->
                row.moveToFirst()
                assertEquals(50000.0, row.getDouble(0), 0.01)
                assertEquals("Préstamo a Camilo", row.getString(1))
                assertEquals(true, row.isNull(2))
                assertEquals(true, row.isNull(3))
            }
            db.query("SELECT COUNT(*) FROM savings_accounts").use { assertCount(it, 1) }
            db.query("SELECT COUNT(*) FROM savings_deposits").use { assertCount(it, 1) }
            db.query("SELECT COUNT(*) FROM categories WHERE type = 'SYSTEM'").use { assertCount(it, 4) }
            db.query("SELECT COUNT(*) FROM categories WHERE id = 'cat_comida'").use { assertCount(it, 1) }
        }
    }

    private fun assertCount(cursor: android.database.Cursor, expected: Int) {
        cursor.moveToFirst()
        assertEquals(expected, cursor.getInt(0))
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
