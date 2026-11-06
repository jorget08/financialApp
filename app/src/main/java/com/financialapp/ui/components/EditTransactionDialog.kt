package com.financialapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.financialapp.data.TransactionEdit
import com.financialapp.data.local.dao.TransactionWithCategory
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.SystemCategory
import com.financialapp.ui.theme.*

/**
 * Editar monto, categoría, observación, fecha y cuenta de un movimiento ya registrado.
 * Un gasto o ingreso también puede marcarse como préstamo con una persona.
 */
@Composable
fun EditTransactionDialog(
    transaction: TransactionWithCategory,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEdit) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(kotlin.math.abs(transaction.amount).toLong().toString()) }
    var noteText by remember { mutableStateOf(transaction.note ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var date by remember { mutableLongStateOf(transaction.date) }
    var accountId by remember { mutableStateOf(transaction.accountId) }
    var person by remember { mutableStateOf(transaction.personName ?: "") }

    val wasLoan = transaction.type == MovementType.LOAN_OUT || transaction.type == MovementType.LOAN_IN
    val moneyOut = transaction.type == MovementType.EXPENSE || transaction.type == MovementType.LOAN_OUT
    val canBeLoan = wasLoan || transaction.type == MovementType.EXPENSE || transaction.type == MovementType.INCOME
    var isLoan by remember { mutableStateOf(wasLoan) }

    val baseType = if (moneyOut) MovementType.EXPENSE else MovementType.INCOME
    val pickable = categories.filter { it.type == baseType }
    val showCategories = canBeLoan && !isLoan

    val title = when (transaction.type) {
        MovementType.EXPENSE -> "Editar Gasto"
        MovementType.INCOME -> "Editar Ingreso"
        MovementType.LOAN_OUT, MovementType.LOAN_IN -> "Editar Préstamo"
        MovementType.TRANSFER -> "Editar Transferencia"
        MovementType.SAVINGS -> "Editar Ahorro"
        else -> "Editar Ajuste"
    }

    val amount = parseMoney(amountText)
    val needsPerson = isLoan && person.isBlank()
    // Sin categoría válida (un préstamo que vuelve a ser gasto) se usa la de "Otros"
    val fallbackCategory = if (moneyOut) SystemCategory.OTHER_EXPENSE else SystemCategory.OTHER_INCOME

    FormDialog(
        title = title,
        canSave = amount != null && !needsPerson,
        onDismiss = onDismiss,
        onDelete = onDelete,
        saveLabel = "Guardar Cambios",
        onSave = {
            val newType = when {
                !canBeLoan -> transaction.type
                isLoan -> if (moneyOut) MovementType.LOAN_OUT else MovementType.LOAN_IN
                else -> baseType
            }
            val categoryId = when {
                !canBeLoan -> transaction.categoryId
                isLoan -> SystemCategory.LOAN
                pickable.any { it.id == selectedCategoryId } -> selectedCategoryId
                else -> fallbackCategory
            }
            // Un ajuste guarda el signo en el monto; el formulario solo edita su valor
            val signed = if (transaction.amount < 0) -amount!! else amount!!
            onSave(
                TransactionEdit(
                    amount = signed,
                    type = newType,
                    categoryId = categoryId,
                    note = noteText,
                    date = date,
                    accountId = accountId,
                    personName = if (isLoan) person.trim() else null
                )
            )
        }
    ) {
        MoneyField(amountText, { amountText = it })
        FinanceTextField(noteText, { noteText = it }, "Observación / Detalle", "Ej: Compra en D1, Almuerzo...")
        Text("Fecha", fontSize = 12.sp, color = TextSecondary)
        DateSelector(date) { date = it }
        AccountPicker(accounts, accountId, { accountId = it })

        if (canBeLoan) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Es un préstamo, no un gasto", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = isLoan, onCheckedChange = { isLoan = it })
            }
        }
        if (isLoan) {
            FinanceTextField(person, { person = it }, "Persona", "Ej: Camilo")
        }
        if (showCategories) {
            CategoryPicker(pickable, selectedCategoryId) { selectedCategoryId = it }
        }
    }
}
