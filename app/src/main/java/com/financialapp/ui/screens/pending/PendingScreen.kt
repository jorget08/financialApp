package com.financialapp.ui.screens.pending

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.TransactionEdit
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.PendingMovementEntity
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.ui.components.*
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DUPLICATE_WINDOW_MS = 24 * 60 * 60 * 1000L

/** Ya hay un movimiento igual cerca de esa fecha: probablemente se registró a mano. */
fun findPossibleDuplicate(pending: PendingMovementEntity, transactions: List<TransactionEntity>): TransactionEntity? {
    val amount = pending.amount ?: return null
    return transactions.firstOrNull {
        it.amount == amount && kotlin.math.abs(it.date - pending.postedAt) <= DUPLICATE_WINDOW_MS
    }
}

@Composable
fun PendingScreen(
    pending: List<PendingMovementEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onConfirm: (PendingMovementEntity, TransactionEdit) -> Unit,
    onDiscard: (String) -> Unit,
    onBack: () -> Unit
) {
    var reviewing by remember { mutableStateOf<PendingMovementEntity?>(null) }
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")) }

    ToolScaffold("Por confirmar", onBack) {
        if (pending.isEmpty()) {
            item {
                EmptyCard(
                    "Nada por confirmar",
                    "Cuando llegue una notificación de Nequi o Falabella, el movimiento aparecerá aquí. Actívalo en Ajustes > Captura automática."
                )
            }
        }
        items(pending, key = { it.id }) { item ->
            val duplicate = remember(item, transactions) { findPossibleDuplicate(item, transactions) }
            FinanceCard(onClick = { reviewing = item }) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.merchant ?: item.sourceLabel, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                        Text("${item.sourceLabel} • ${timeFormat.format(Date(item.postedAt))}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(
                        text = item.amount?.let { CurrencyFormatter.format(it) } ?: "Sin monto",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.rawText, color = TextTertiary, fontSize = 11.sp, lineHeight = 15.sp)
                if (duplicate != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Posible duplicado: ya registraste ${CurrencyFormatter.format(duplicate.amount)} (${duplicate.note ?: "sin detalle"}).",
                        color = AccentOrange,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { reviewing = item },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) { Text("Revisar y confirmar", fontSize = 12.sp) }
                    TextButton(onClick = { onDiscard(item.id) }) {
                        Text("Descartar", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    reviewing?.let { item ->
        ConfirmPendingDialog(
            pending = item,
            categories = categories,
            accounts = accounts,
            onDismiss = { reviewing = null },
            onConfirm = { edit ->
                onConfirm(item, edit)
                reviewing = null
            }
        )
    }
}

@Composable
private fun ConfirmPendingDialog(
    pending: PendingMovementEntity,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEdit) -> Unit
) {
    var amountText by remember { mutableStateOf(pending.amount?.toLong()?.toString() ?: "") }
    var type by remember { mutableStateOf(pending.type ?: MovementType.EXPENSE) }
    var note by remember { mutableStateOf(pending.merchant ?: "") }
    var date by remember { mutableLongStateOf(pending.postedAt) }
    var accountId by remember {
        mutableStateOf(
            accounts.firstOrNull { account ->
                !account.captureKeyword.isNullOrBlank() &&
                    pending.sourceLabel.contains(account.captureKeyword, ignoreCase = true)
            }?.id
        )
    }
    val available = categories.filter { it.type == type }
    var categoryId by remember(type) {
        mutableStateOf(
            available.firstOrNull { it.name == pending.suggestedCategoryName }?.id ?: available.firstOrNull()?.id
        )
    }
    val amount = parseMoney(amountText)

    FormDialog(
        title = "Confirmar movimiento",
        canSave = amount != null && categoryId != null,
        onDismiss = onDismiss,
        saveLabel = "Confirmar",
        onSave = {
            onConfirm(
                TransactionEdit(
                    amount = amount!!,
                    type = type,
                    categoryId = categoryId!!,
                    note = note,
                    date = date,
                    accountId = accountId,
                    personName = null
                )
            )
        }
    ) {
        Text(pending.rawText, color = TextTertiary, fontSize = 11.sp, lineHeight = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChoiceChip("Gasto", type == MovementType.EXPENSE) { type = MovementType.EXPENSE }
            ChoiceChip("Ingreso", type == MovementType.INCOME) { type = MovementType.INCOME }
        }
        MoneyField(amountText, { amountText = it })
        FinanceTextField(note, { note = it }, "Observación / Detalle")
        Text("Fecha", fontSize = 12.sp, color = TextSecondary)
        DateSelector(date) { date = it }
        AccountPicker(accounts, accountId, { accountId = it })
        CategoryPicker(available, categoryId) { categoryId = it }
    }
}
