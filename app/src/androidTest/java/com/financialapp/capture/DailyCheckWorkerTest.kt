package com.financialapp.capture

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DailyCheckWorkerTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun nightlyRunWritesBackupAndRemindsAboutBillsAndEmptyDay() = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val db = AppDatabase.getInstance(context)
        db.transactionDao().getAllEntities().forEach { db.transactionDao().deleteTransaction(it) }
        db.categoryDao().insertMissingCategories(
            listOf(CategoryEntity(id = "cat_prueba", name = "Prueba", iconName = "", colorHex = "#000000", type = "EXPENSE"))
        )
        db.billDao().upsert(
            RecurringBillEntity(
                id = "factura_prueba",
                name = "Internet de prueba",
                amount = 85_000.0,
                dayOfMonth = LocalDate.now().dayOfMonth,
                categoryId = "cat_prueba"
            )
        )

        val worker = TestListenableWorkerBuilder<DailyCheckWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        val backups = File(context.filesDir, "auto_backups").listFiles().orEmpty()
        assertTrue("Debe existir la copia del día", backups.any { it.name.startsWith("dia_") && it.length() > 0 })

        // Android publica las notificaciones de forma asíncrona
        val manager = context.getSystemService(NotificationManager::class.java)
        var titles = emptyList<String>()
        for (attempt in 1..25) {
            titles = manager.activeNotifications
                .map { it.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString() }
            if (titles.contains("Pagos por vencer") && titles.contains("¿Registraste todo hoy?")) break
            delay(200)
        }
        assertTrue("Títulos: $titles", titles.contains("Pagos por vencer"))
        assertTrue("Títulos: $titles", titles.contains("¿Registraste todo hoy?"))
    }
}
