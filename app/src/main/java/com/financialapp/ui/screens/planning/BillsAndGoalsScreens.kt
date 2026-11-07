package com.financialapp.ui.screens.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.domain.engine.BillDue
import com.financialapp.domain.engine.BillSchedule
import com.financialapp.ui.components.*
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dueFormat = DateTimeFormatter.ofPattern("d MMM", Locale("es", "CO"))

private fun dueLabel(due: BillDue): String = when {
    due.isOverdue -> "Venció hace ${-due.daysUntil} días"
    due.daysUntil == 0L -> "Vence hoy"
    due.daysUntil == 1L -> "Vence mañana"
    else -> "Vence el ${due.dueDate.format(dueFormat)}"
}

@Composable
fun BillsScreen(
    bills: List<RecurringBillEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onSave: (RecurringBillEntity) -> Unit,
    onDelete: (RecurringBillEntity) -> Unit,
    onPay: (RecurringBillEntity, amount: Double, accountId: String?) -> Unit,
    onBack: () -> Unit
) {
    val today = LocalDate.now()
    val dues = remember(bills, today) { bills.map { BillSchedule.nextDue(it, today) }.sortedBy { it.dueDate } }
    var editing by remember { mutableStateOf<RecurringBillEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<RecurringBillEntity?>(null) }

    ToolScaffold("Pagos fijos", onBack, onAdd = { creating = true }, addLabel = "Pago fijo") {
        if (dues.isEmpty()) {
            item {
                EmptyCard(
                    "Registra lo que pagas cada mes",
                    "Arriendo, servicios, suscripciones... Te avisamos 2 días antes y lo registras con un toque."
                )
            }
        } else {
            item {
                FinanceCard {
                    Text("Total fijo al mes", fontSize = 11.sp, color = TextSecondary)
                    Text(CurrencyFormatter.format(bills.sumOf { it.amount }), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
        items(dues, key = { it.bill.id }) { due ->
            FinanceCard(onClick = { editing = due.bill }) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(due.bill.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "${CurrencyFormatter.format(due.bill.amount)} • ${dueLabel(due)}",
                            color = if (due.isOverdue) AccentRed else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    FilledTonalButton(onClick = { paying = due.bill }) { Text("Pagué", fontSize = 12.sp) }
                }
            }
        }
    }

    if (creating || editing != null) {
        BillDialog(
            bill = editing,
            categories = categories.filter { it.type == "EXPENSE" },
            accounts = accounts,
            onDismiss = { creating = false; editing = null },
            onSave = { onSave(it); creating = false; editing = null },
            onDelete = editing?.let { bill -> { onDelete(bill); editing = null } }
        )
    }
    paying?.let { bill ->
        var amountText by remember(bill.id) { mutableStateOf(bill.amount.toLong().toString()) }
        var accountId by remember(bill.id) { mutableStateOf(bill.accountId) }
        val amount = parseMoney(amountText)
        FormDialog(
            title = "Pagar ${bill.name}",
            canSave = amount != null,
            onDismiss = { paying = null },
            saveLabel = "Registrar pago",
            onSave = { onPay(bill, amount!!, accountId); paying = null }
        ) {
            MoneyField(amountText, { amountText = it }, "Monto de este mes en COP ($)")
            AccountPicker(accounts, accountId, { accountId = it })
        }
    }
}

@Composable
private fun BillDialog(
    bill: RecurringBillEntity?,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (RecurringBillEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var amountText by remember { mutableStateOf(bill?.amount?.toLong()?.toString() ?: "") }
    var dayText by remember { mutableStateOf(bill?.dayOfMonth?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(bill?.categoryId ?: categories.firstOrNull()?.id) }
    var accountId by remember { mutableStateOf(bill?.accountId) }
    val amount = parseMoney(amountText)
    val day = dayText.toIntOrNull()?.takeIf { it in 1..31 }

    FormDialog(
        title = if (bill == null) "Nuevo pago fijo" else "Editar pago fijo",
        canSave = name.isNotBlank() && amount != null && day != null && categoryId != null,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onSave = {
            onSave(
                (bill ?: RecurringBillEntity(name = name.trim(), amount = amount!!, dayOfMonth = day!!, categoryId = categoryId!!))
                    .copy(name = name.trim(), amount = amount!!, dayOfMonth = day!!, categoryId = categoryId!!, accountId = accountId)
            )
        }
    ) {
        FinanceTextField(name, { name = it }, "Nombre", "Ej: Arriendo, Internet, Netflix")
        MoneyField(amountText, { amountText = it })
        OutlinedTextField(
            value = dayText,
            onValueChange = { text -> dayText = text.filter { it.isDigit() }.take(2) },
            label = { Text("Día del mes en que vence (1-31)") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            colors = financeFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        CategoryPicker(categories, categoryId) { categoryId = it }
        AccountPicker(accounts, accountId, { accountId = it })
    }
}

@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    savingsBalance: Double?,
    averageMonthlyExpenses: Double,
    emergencyMonths: Double?,
    onSave: (GoalEntity) -> Unit,
    onDelete: (GoalEntity) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    ToolScaffold("Metas y ahorro", onBack, onAdd = { creating = true }, addLabel = "Meta") {
        item {
            FinanceCard {
                Text("Fondo de emergencia", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                if (savingsBalance == null) {
                    Text("Configura tu cuenta de ahorro en la pestaña Ahorros para calcularlo.", color = TextSecondary, fontSize = 12.sp)
                } else if (emergencyMonths == null) {
                    Text("Registra tus gastos del mes para calcular cuántos meses cubre tu ahorro.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Text(
                        "Tu ahorro de ${CurrencyFormatter.format(savingsBalance)} cubre ${String.format(Locale.US, "%.1f", emergencyMonths)} meses de gastos.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ProgressBar(emergencyMonths / EMERGENCY_TARGET_MONTHS)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (emergencyMonths >= EMERGENCY_TARGET_MONTHS) {
                            "Ya cubres los 6 meses recomendados."
                        } else {
                            "Meta: 6 meses = ${CurrencyFormatter.format(averageMonthlyExpenses * EMERGENCY_TARGET_MONTHS)}."
                        },
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        if (goals.isEmpty()) {
            item {
                EmptyCard(
                    "¿Qué quieres lograr?",
                    "Agrega el terreno, la casa, la moto... con su valor. El asesor los usa para decirte qué va primero."
                )
            }
        }
        items(goals, key = { it.id }) { goal ->
            val ratio = if (goal.targetAmount > 0) goal.savedAmount / goal.targetAmount else 0.0
            FinanceCard(onClick = { editing = goal }) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(goal.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("${(ratio * 100).roundToInt()}%", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                ProgressBar(ratio)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${CurrencyFormatter.format(goal.savedAmount)} de ${CurrencyFormatter.format(goal.targetAmount)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }

    if (creating || editing != null) {
        GoalDialog(
            goal = editing,
            nextPriority = goals.size,
            onDismiss = { creating = false; editing = null },
            onSave = { onSave(it); creating = false; editing = null },
            onDelete = editing?.let { goal -> { onDelete(goal); editing = null } }
        )
    }
}

private const val EMERGENCY_TARGET_MONTHS = 6.0

@Composable
private fun GoalDialog(
    goal: GoalEntity?,
    nextPriority: Int,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetText by remember { mutableStateOf(goal?.targetAmount?.toLong()?.toString() ?: "") }
    var savedText by remember { mutableStateOf(goal?.savedAmount?.toLong()?.toString() ?: "") }
    val target = parseMoney(targetText)

    FormDialog(
        title = if (goal == null) "Nueva meta" else "Editar meta",
        canSave = name.isNotBlank() && target != null,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onSave = {
            val saved = savedText.toDoubleOrNull() ?: 0.0
            onSave(
                (goal ?: GoalEntity(name = name.trim(), targetAmount = target!!, priority = nextPriority))
                    .copy(name = name.trim(), targetAmount = target!!, savedAmount = saved)
            )
        }
    ) {
        FinanceTextField(name, { name = it }, "Meta", "Ej: Terreno, Moto, Casa prefabricada")
        MoneyField(targetText, { targetText = it }, "Cuánto cuesta (COP)")
        OutlinedTextField(
            value = savedText,
            onValueChange = { text -> savedText = text.filter { it.isDigit() } },
            label = { Text("Cuánto llevas (COP)") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            colors = financeFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
