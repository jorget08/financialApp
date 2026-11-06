package com.financialapp.ui.screens.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.ui.components.AccountPicker
import com.financialapp.ui.theme.*

@Composable
fun RecordDepositDialogCop(
    defaultAmount: Double,
    onDismiss: () -> Unit,
    accounts: List<AccountEntity>,
    onConfirm: (Double, String?) -> Unit
) {
    var amountText by remember { mutableStateOf(defaultAmount.toLong().toString()) }
    var fromAccountId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Depósito a Cuenta", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Monto en pesos que acabas de transferir a tu cuenta de ahorros:", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto depositado (COP $)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                AccountPicker(accounts, fromAccountId, { fromAccountId = it }, "Sale de")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) onConfirm(amount, fromAccountId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Registrar Aporte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
