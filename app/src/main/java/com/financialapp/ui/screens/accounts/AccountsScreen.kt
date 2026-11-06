package com.financialapp.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.AccountType
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.domain.engine.AccountLedger
import com.financialapp.ui.components.*
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onSaveAccount: (AccountEntity) -> Unit,
    onRemoveAccount: (AccountEntity) -> Unit,
    onReconcile: (AccountEntity, calculated: Double, real: Double) -> Unit,
    onTransfer: (from: String, to: String, amount: Double, date: Long, note: String?) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<AccountEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var reconciling by remember { mutableStateOf<AccountEntity?>(null) }
    var transferring by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("es", "CO")) }

    val balances = remember(accounts, transactions) { accounts.associate { it.id to AccountLedger.balance(it, transactions) } }
    val cash = accounts.filter { it.type != AccountType.CREDIT_CARD }.sumOf { balances[it.id] ?: 0.0 }
    val debt = accounts.filter { it.type == AccountType.CREDIT_CARD }.sumOf { balances[it.id] ?: 0.0 }
    val unassigned = remember(transactions) {
        transactions.count { it.accountId == null && it.type in listOf(MovementType.EXPENSE, MovementType.INCOME, MovementType.LOAN_OUT, MovementType.LOAN_IN) }
    }

    ToolScaffold("Cuentas y tarjetas", onBack, onAdd = { creating = true }, addLabel = "Cuenta") {
        if (accounts.isEmpty()) {
            item {
                EmptyCard(
                    "Agrega tus cuentas",
                    "Crea Nequi, Falabella, efectivo... con su saldo de hoy. Así la app te muestra si lo registrado cuadra con lo que tienes de verdad."
                )
            }
        } else {
            item {
                FinanceCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disponible", fontSize = 11.sp, color = TextSecondary)
                            Text(CurrencyFormatter.format(cash), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deuda en tarjetas", fontSize = 11.sp, color = TextSecondary)
                            Text(CurrencyFormatter.format(kotlin.math.abs(debt)), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                    if (unassigned > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$unassigned movimientos no tienen cuenta y no afectan estos saldos.",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
            if (accounts.size >= 2) {
                item {
                    OutlinedButton(onClick = { transferring = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Transferir o pagar tarjeta", color = TextPrimary)
                    }
                }
            }
        }
        items(accounts, key = { it.id }) { account ->
            val balance = balances[account.id] ?: 0.0
            val isCard = account.type == AccountType.CREDIT_CARD
            FinanceCard(onClick = { editing = account }) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            when (account.type) {
                                AccountType.CREDIT_CARD -> "Tarjeta de crédito"
                                AccountType.CASH -> "Efectivo"
                                else -> "Cuenta"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(CurrencyFormatter.format(kotlin.math.abs(balance)), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            when {
                                isCard -> if (balance < 0) "por pagar" else "a favor"
                                balance < 0 -> "saldo negativo"
                                else -> "saldo"
                            },
                            color = if (balance < 0 && !isCard) AccentRed else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        account.lastReconciledAt?.let { "Cuadrada el ${dateFormat.format(Date(it))}" } ?: "Aún sin cuadrar",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { reconciling = account }) { Text("Cuadrar", fontSize = 12.sp, color = AccentBlue) }
                }
            }
        }
    }

    if (creating || editing != null) {
        AccountDialog(
            account = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { onSaveAccount(it); creating = false; editing = null },
            onRemove = editing?.let { account -> { onRemoveAccount(account); editing = null } }
        )
    }
    reconciling?.let { account ->
        ReconcileDialog(
            account = account,
            calculated = balances[account.id] ?: 0.0,
            onDismiss = { reconciling = null },
            onConfirm = { real ->
                onReconcile(account, balances[account.id] ?: 0.0, real)
                reconciling = null
            }
        )
    }
    if (transferring) {
        TransferDialog(
            accounts = accounts,
            onDismiss = { transferring = false },
            onConfirm = { from, to, amount, date, note ->
                onTransfer(from, to, amount, date, note)
                transferring = false
            }
        )
    }
}

@Composable
private fun AccountDialog(
    account: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onRemove: (() -> Unit)?
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: AccountType.BANK) }
    // La deuda de una tarjeta se escribe en positivo y se guarda negativa
    var balanceText by remember { mutableStateOf(account?.let { kotlin.math.abs(it.openingBalance).toLong().toString() } ?: "") }
    var keyword by remember { mutableStateOf(account?.captureKeyword ?: "") }
    val isCard = type == AccountType.CREDIT_CARD

    FormDialog(
        title = if (account == null) "Nueva cuenta" else "Editar cuenta",
        canSave = name.isNotBlank(),
        onDismiss = onDismiss,
        onDelete = onRemove,
        onSave = {
            val amount = balanceText.toDoubleOrNull() ?: 0.0
            val opening = if (isCard) -amount else amount
            onSave(
                (account ?: AccountEntity(name = name.trim(), type = type, openingBalance = opening)).copy(
                    name = name.trim(),
                    type = type,
                    openingBalance = opening,
                    captureKeyword = keyword.trim().ifBlank { null }
                )
            )
        }
    ) {
        FinanceTextField(name, { name = it }, "Nombre", "Ej: Nequi, Falabella, Efectivo")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChoiceChip("Cuenta", type == AccountType.BANK) { type = AccountType.BANK }
            ChoiceChip("Efectivo", type == AccountType.CASH) { type = AccountType.CASH }
            ChoiceChip("Tarjeta", isCard) { type = AccountType.CREDIT_CARD }
        }
        OutlinedTextField(
            value = balanceText,
            onValueChange = { text -> balanceText = text.filter { it.isDigit() } },
            label = { Text(if (isCard) "Deuda actual en COP" else "Saldo actual en COP") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            colors = financeFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Los movimientos anteriores a hoy no cambian este saldo: ya están incluidos en él.",
            fontSize = 11.sp,
            color = TextTertiary
        )
        FinanceTextField(keyword, { keyword = it }, "Palabra en notificaciones (opcional)", "Ej: nequi")
    }
}

@Composable
private fun ReconcileDialog(
    account: AccountEntity,
    calculated: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val isCard = account.type == AccountType.CREDIT_CARD
    var realText by remember { mutableStateOf("") }
    val entered = realText.toDoubleOrNull()
    val real = entered?.let { if (isCard) -it else it }
    val difference = real?.let { it - calculated }

    FormDialog(
        title = "Cuadrar ${account.name}",
        canSave = real != null,
        onDismiss = onDismiss,
        saveLabel = if (difference == 0.0) "Confirmar" else "Ajustar saldo",
        onSave = { onConfirm(real!!) }
    ) {
        Text(
            "La app calcula ${CurrencyFormatter.format(kotlin.math.abs(calculated))}. Abre ${account.name} y escribe lo que aparece allá.",
            fontSize = 13.sp,
            color = TextSecondary
        )
        OutlinedTextField(
            value = realText,
            onValueChange = { text -> realText = text.filter { it.isDigit() } },
            label = { Text(if (isCard) "Deuda real en COP" else "Saldo real en COP") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            colors = financeFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (difference != null) {
            if (difference == 0.0) {
                Text("Todo cuadra.", color = AccentPrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(
                    "Diferencia de ${CurrencyFormatter.format(kotlin.math.abs(difference))}: " +
                        if (difference < 0) "falta registrar un gasto, o sobra un ingreso." else "falta registrar un ingreso, o sobra un gasto.",
                    color = AccentOrange,
                    fontSize = 13.sp
                )
                Text(
                    "Mejor busca y registra lo que falta. Ajustar agrega un movimiento que corrige el saldo, sin contar como gasto ni ingreso.",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TransferDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Long, String?) -> Unit
) {
    val sources = accounts.filter { it.type != AccountType.CREDIT_CARD }.ifEmpty { accounts }
    var fromId by remember { mutableStateOf(sources.first().id) }
    var toId by remember { mutableStateOf(accounts.firstOrNull { it.id != fromId }?.id) }
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val amount = parseMoney(amountText)
    val destination = toId?.takeIf { it != fromId }
    val paysCard = accounts.firstOrNull { it.id == destination }?.type == AccountType.CREDIT_CARD

    FormDialog(
        title = "Transferir o pagar tarjeta",
        canSave = amount != null && destination != null,
        onDismiss = onDismiss,
        saveLabel = if (paysCard) "Pagar tarjeta" else "Transferir",
        onSave = {
            onConfirm(fromId, destination!!, amount!!, date, if (paysCard) "Pago de tarjeta" else "Transferencia")
        }
    ) {
        AccountPicker(accounts, fromId, { id -> id?.let { fromId = it } }, "Sale de", allowNone = false)
        AccountPicker(accounts.filter { it.id != fromId }, destination, { toId = it }, "Llega a", allowNone = false)
        MoneyField(amountText, { amountText = it })
        Text("Fecha", fontSize = 12.sp, color = TextSecondary)
        DateSelector(date) { date = it }
        if (paysCard) {
            Text(
                "El pago baja tu deuda y no cuenta como gasto: las compras ya contaron cuando las hiciste.",
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
    }
}
