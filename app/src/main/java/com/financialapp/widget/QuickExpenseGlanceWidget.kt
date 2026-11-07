package com.financialapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.financialapp.data.local.AppDatabase
import com.financialapp.ui.screens.quickentry.QuickExpenseDialogActivity
import com.financialapp.util.CurrencyFormatter
import com.financialapp.util.FinancialMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Widget de Escritorio para Android / Xiaomi HyperOS construido con Jetpack Glance.
 * Adaptado a Pesos Colombianos (COP) para registro directo en 1 toque.
 */
class QuickExpenseGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val (startOfMonth, endOfMonth) = FinancialMonth.currentPeriodRange(FinancialMonth.getStartDay(context))

        val (totalSpentThisMonth, totalToday) = withContext(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactionsSince(startOfMonth)
            val monthSum = transactions.filter { it.type == "EXPENSE" && it.date < endOfMonth }.sumOf { it.amount }
            val startOfDay = getStartOfDayTimestamp()
            val todaySum = transactions.filter { it.type == "EXPENSE" && it.date >= startOfDay }.sumOf { it.amount }
            Pair(monthSum, todaySum)
        }

        provideContent {
            GlanceTheme {
                // Una sola fila para que quepa en un widget de 1 fila de alto
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF161B22))
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Resumen de gastos en COP
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Hoy: ${CurrencyFormatter.format(totalToday)}",
                            style = TextStyle(
                                color = ColorProvider(day = Color(0xFFF0F6FC), night = Color(0xFFF0F6FC)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "Mes: ${CurrencyFormatter.format(totalSpentThisMonth)}",
                            style = TextStyle(
                                color = ColorProvider(day = Color(0xFF8B949E), night = Color(0xFF8B949E)),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }

                    // Botones para abrir el registro rápido en modo Gasto o Ingreso
                    EntryButton(
                        context = context,
                        label = "+ Gasto",
                        entryType = "EXPENSE",
                        color = Color(0xFF238636)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    EntryButton(
                        context = context,
                        label = "+ Ingreso",
                        entryType = "INCOME",
                        color = Color(0xFF1F6FEB)
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun EntryButton(
        context: Context,
        label: String,
        entryType: String,
        color: Color,
        modifier: GlanceModifier = GlanceModifier
    ) {
        // La acción distinta evita que Android reutilice el mismo PendingIntent para ambos botones
        val intent = Intent(context, QuickExpenseDialogActivity::class.java).apply {
            action = "com.financialapp.widget.ENTRY_$entryType"
            putExtra(QuickExpenseDialogActivity.EXTRA_ENTRY_TYPE, entryType)
        }
        Box(
            modifier = modifier
                .background(color)
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
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
