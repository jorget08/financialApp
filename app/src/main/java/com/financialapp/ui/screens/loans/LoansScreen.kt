package com.financialapp.ui.screens.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.dao.TransactionWithCategory
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.domain.engine.ExpenseAnalyzer
import com.financialapp.domain.engine.LoanLedger
import com.financialapp.domain.engine.PersonLoan
import com.financialapp.ui.components.*
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoansScreen(
    transactions: List<TransactionEntity>,
    allMovements: List<TransactionWithCategory>,
    accounts: List<AccountEntity>,
    onRecordLoan: (person: String, amount: Double, moneyOut: Boolean, accountId: String?, date: Long, note: String?) -> Unit,
    onConvert: (ids: List<String>, person: String) -> Unit,
    onBack: () -> Unit
) {
    val loans = remember(transactions) { LoanLedger.summarize(transactions) }
    var creating by remember { mutableStateOf(false) }
    var presetPerson by remember { mutableStateOf<String?>(null) }
    var converting by remember { mutableStateOf(false) }
    val owedToMe = loans.filter { it.balance > 0 }.sumOf { it.balance }
    val iOwe = loans.filter { it.balance < 0 }.sumOf { -it.balance }

    ToolScaffold("Préstamos", onBack, onAdd = { presetPerson = null; creating = true }, addLabel = "Movimiento") {
        item {
            FinanceCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Te deben", fontSize = 11.sp, color = TextSecondary)
                        Text(CurrencyFormatter.format(owedToMe), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Debes", fontSize = 11.sp, color = TextSecondary)
                        Text(CurrencyFormatter.format(iOwe), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Los préstamos no cuentan como gasto ni ingreso del mes.", fontSize = 11.sp, color = TextTertiary)
            }
        }
        item {
            OutlinedButton(onClick = { converting = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Pasar gastos antiguos a préstamos", color = TextPrimary)
            }
        }
        if (loans.isEmpty()) {
            item {
                EmptyCard(
                    "Sin préstamos todavía",
                    "Si ya prestaste plata y la registraste como gasto, usa el botón de arriba: busca el nombre y conviértelos de una vez."
                )
            }
        }
        items(loans, key = { it.personName }) { loan ->
            PersonRow(loan, onClick = { presetPerson = loan.personName; creating = true })
        }
    }

    if (creating) {
        LoanDialog(
            presetPerson = presetPerson,
            accounts = accounts,
            onDismiss = { creating = false },
            onConfirm = { person, amount, moneyOut, accountId, date, note ->
                onRecordLoan(person, amount, moneyOut, accountId, date, note)
                creating = false
            }
        )
    }
    if (converting) {
        ConvertDialog(
            movements = allMovements,
            onDismiss = { converting = false },
            onConfirm = { ids, person ->
                onConvert(ids, person)
                converting = false
            }
        )
    }
}

@Composable
private fun PersonRow(loan: PersonLoan, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "CO")) }
    FinanceCard(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(loan.personName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Salió ${CurrencyFormatter.format(loan.paidOut)} • Entró ${CurrencyFormatter.format(loan.received)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    "${loan.movementCount} movimientos • último ${dateFormat.format(Date(loan.lastMovementAt))}",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyFormatter.format(kotlin.math.abs(loan.balance)), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    when {
                        loan.balance > 0 -> "te debe"
                        loan.balance < 0 -> "le debes"
                        else -> "a paz y salvo"
                    },
                    color = if (loan.balance == 0.0) AccentPrimaryLight else TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun LoanDialog(
    presetPerson: String?,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Boolean, String?, Long, String?) -> Unit
) {
    var person by remember { mutableStateOf(presetPerson ?: "") }
    var amountText by remember { mutableStateOf("") }
    var moneyOut by remember { mutableStateOf(true) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    val amount = parseMoney(amountText)

    FormDialog(
        title = "Movimiento de préstamo",
        canSave = amount != null && person.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onConfirm(person, amount!!, moneyOut, accountId, date, note.ifBlank { null }) }
    ) {
        FinanceTextField(person, { person = it }, "Persona", "Ej: Camilo")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChoiceChip("Salió plata", moneyOut) { moneyOut = true }
            ChoiceChip("Entró plata", !moneyOut) { moneyOut = false }
        }
        Text(
            if (moneyOut) "Le prestaste, o le estás pagando lo que le debes." else "Te pagó, o esa persona te prestó a ti.",
            fontSize = 11.sp,
            color = TextTertiary
        )
        MoneyField(amountText, { amountText = it })
        FinanceTextField(note, { note = it }, "Observación (opcional)")
        Text("Fecha", fontSize = 12.sp, color = TextSecondary)
        DateSelector(date) { date = it }
        AccountPicker(accounts, accountId, { accountId = it })
    }
}

/** Busca por nombre en todo el historial y deja desmarcar lo que no sea un préstamo. */
@Composable
private fun ConvertDialog(
    movements: List<TransactionWithCategory>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val matches = remember(name, movements) {
        if (name.isBlank()) emptyList() else ExpenseAnalyzer.filter(movements, name, null).filter {
            it.type == MovementType.EXPENSE || it.type == MovementType.INCOME
        }
    }
    val excluded = remember { mutableStateMapOf<String, Boolean>() }
    val selected = matches.filter { excluded[it.id] != true }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "CO")) }

    FormDialog(
        title = "Pasar a préstamos",
        canSave = selected.isNotEmpty() && name.isNotBlank(),
        onDismiss = onDismiss,
        saveLabel = "Convertir ${selected.size}",
        onSave = { onConfirm(selected.map { it.id }, name) }
    ) {
        FinanceTextField(name, { name = it }, "Nombre de la persona", "Ej: Camilo")
        if (name.isNotBlank() && matches.isEmpty()) {
            Text("Ningún gasto o ingreso tiene ese nombre en su observación.", fontSize = 12.sp, color = TextSecondary)
        }
        matches.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { excluded[item.id] = excluded[item.id] != true }
            ) {
                Checkbox(checked = excluded[item.id] != true, onCheckedChange = { excluded[item.id] = !it })
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.note ?: item.categoryName, fontSize = 13.sp, color = TextPrimary, maxLines = 1)
                    Text(
                        "${dateFormat.format(Date(item.date))} • ${if (item.type == MovementType.EXPENSE) "salió" else "entró"} ${CurrencyFormatter.format(item.amount)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
