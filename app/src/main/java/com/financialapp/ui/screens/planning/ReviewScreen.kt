package com.financialapp.ui.screens.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.domain.engine.MonthlyReview
import com.financialapp.domain.engine.PeriodReview
import com.financialapp.ui.components.*
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import kotlin.math.roundToInt

/** Comparación mes a mes: de dónde viene el cambio y cuánto queda de lo que entra. */
@Composable
fun ReviewScreen(reviews: List<PeriodReview>, onAskAdvisor: () -> Unit, onBack: () -> Unit) {
    val current = reviews.firstOrNull()
    val previous = reviews.getOrNull(1)
    val changes = remember(current, previous) {
        if (current != null && previous != null) MonthlyReview.changes(current, previous).take(MAX_CHANGES) else emptyList()
    }
    val maxExpenses = reviews.maxOfOrNull { it.expenses } ?: 0.0

    ToolScaffold("Revisión mensual", onBack) {
        if (current == null || reviews.all { it.income == 0.0 && it.expenses == 0.0 }) {
            item { EmptyCard("Aún no hay datos", "Cuando registres movimientos verás aquí cómo cambia cada mes.") }
            return@ToolScaffold
        }
        item {
            FinanceCard {
                Text("Este mes (${current.label}, aún en curso)", fontSize = 11.sp, color = TextSecondary)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Stat("Ingresos", CurrencyFormatter.format(current.income), Modifier.weight(1f))
                    Stat("Gastos", CurrencyFormatter.format(current.expenses), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Stat("Apartado a ahorro", CurrencyFormatter.format(current.saved), Modifier.weight(1f))
                    Stat("Tasa de ahorro", if (current.income > 0) "${(current.savingsRate * 100).roundToInt()}%" else "Sin ingresos", Modifier.weight(1f))
                }
            }
        }
        if (changes.isNotEmpty()) {
            item { SectionLabel("Qué cambió frente a ${previous?.label}") }
            items(changes, key = { it.categoryName }) { change ->
                FinanceCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(change.categoryName, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            (if (change.difference > 0) "+" else "-") + CurrencyFormatter.format(kotlin.math.abs(change.difference)),
                            color = if (change.difference > 0) AccentRed else AccentPrimaryLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "${CurrencyFormatter.format(change.current)} ahora, ${CurrencyFormatter.format(change.previous)} antes",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        item { SectionLabel("Gastos por mes") }
        items(reviews, key = { it.label }) { review ->
            FinanceCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(review.label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(CurrencyFormatter.format(review.expenses), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                ProgressBar(if (maxExpenses > 0) review.expenses / maxExpenses else 0.0)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Ingresos ${CurrencyFormatter.format(review.income)}" +
                        if (review.income > 0) " • tasa de ahorro ${(review.savingsRate * 100).roundToInt()}%" else "",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        item {
            Button(
                onClick = onAskAdvisor,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Pedirle sugerencias al asesor") }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

private const val MAX_CHANGES = 4
