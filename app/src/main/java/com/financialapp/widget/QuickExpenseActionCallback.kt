package com.financialapp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.TransactionEntity

/**
 * Callback de acción de Jetpack Glance que se ejecuta en segundo plano
 * al presionar un botón del widget de escritorio de Xiaomi HyperOS / Android.
 * Inserta la transacción en Room en <30ms sin abrir la app.
 */
class QuickExpenseActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val amount = parameters[AmountKey] ?: return
        val categoryName = parameters[CategoryNameKey] ?: "General"
        val note = parameters[NoteKey] ?: "Gasto desde Widget"

        val db = AppDatabase.getInstance(context)
        val category = db.categoryDao().findByName(categoryName)
            ?: db.categoryDao().getAllCategories().firstOrNull()

        if (category != null) {
            val transaction = TransactionEntity(
                amount = amount,
                type = "EXPENSE",
                categoryId = category.id,
                note = note,
                source = "WIDGET",
                date = System.currentTimeMillis()
            )
            db.transactionDao().insertTransaction(transaction)
        }

        // Refrescar el widget inmediatamente en pantalla
        QuickExpenseGlanceWidget().update(context, glanceId)
    }

    companion object {
        val AmountKey = ActionParameters.Key<Double>("amount_key")
        val CategoryNameKey = ActionParameters.Key<String>("category_name_key")
        val NoteKey = ActionParameters.Key<String>("note_key")
    }
}
