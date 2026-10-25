package com.financialapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.financialapp.data.local.AppDatabase
import com.financialapp.ui.screens.quickentry.QuickExpenseDialogActivity
import com.financialapp.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Widget de Escritorio para Android / Xiaomi HyperOS construido con Jetpack Glance.
 * Adaptado a Pesos Colombianos (COP) para registro directo en 1 toque.
 */
class QuickExpenseGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val startOfMonth = getStartOfMonthTimestamp()

        val (totalSpentThisMonth, totalToday) = withContext(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactionsSince(startOfMonth)
            val monthSum = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val startOfDay = getStartOfDayTimestamp()
            val todaySum = transactions.filter { it.type == "EXPENSE" && it.date >= startOfDay }.sumOf { it.amount }
            Pair(monthSum, todaySum)
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF161B22))
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                ) {
                    // Barra Superior con Totales en COP
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Finanzas Pro (COP)",
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFF8B949E), night = Color(0xFF8B949E)),
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "Hoy: ${CurrencyFormatter.format(totalToday)}",
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFFF0F6FC), night = Color(0xFFF0F6FC)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Botón Flotante para Entrada Personalizada Rápida
                        Box(
                            modifier = GlanceModifier
                                .background(Color(0xFF238636))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .clickable(actionStartActivity(Intent(context, QuickExpenseDialogActivity::class.java)))
                        ) {
                            Text(
                                text = "⚡ + Gasto",
                                style = TextStyle(
                                    color = ColorProvider(day = Color.White, night = Color.White),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Fila de Botones de Registro Inmediato en COP
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QuickActionButton(
                            label = "☕ 5k",
                            category = "Entretenimiento y Ocio",
                            amount = 5000.0,
                            note = "Café rápido",
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        QuickActionButton(
                            label = "🍔 25k",
                            category = "Entretenimiento y Ocio",
                            amount = 25000.0,
                            note = "Almuerzo / Comida",
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        QuickActionButton(
                            label = "🚕 15k",
                            category = "Transporte",
                            amount = 15000.0,
                            note = "Taxi / Transporte",
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun QuickActionButton(
        label: String,
        category: String,
        amount: Double,
        note: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .background(Color(0xFF21262D))
                .cornerRadius(8.dp)
                .padding(vertical = 8.dp)
                .clickable(
                    actionRunCallback<QuickExpenseActionCallback>(
                        actionParametersOf(
                            QuickExpenseActionCallback.AmountKey to amount,
                            QuickExpenseActionCallback.CategoryNameKey to category,
                            QuickExpenseActionCallback.NoteKey to note
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF58A6FF), night = Color(0xFF58A6FF)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    private fun getStartOfMonthTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfDayTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
