package com.financialapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.dao.TransactionWithCategory
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentTransactions: List<TransactionWithCategory>,
    categories: List<CategoryEntity>,
    totalIncome: Double,
    totalExpenses: Double,
    onQuickExpenseClick: () -> Unit,
    onQuickIncomeClick: () -> Unit,
    onUpdateTransaction: (id: String, amount: Double, categoryId: String, note: String) -> Unit,
    onDeleteTransaction: (id: String) -> Unit
) {
    val netBalance = totalIncome - totalExpenses
    var transactionToEdit by remember { mutableStateOf<TransactionWithCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Finanzas Pro (COP)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Xiaomi 14T Pro • HyperOS Edition", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de Balance del Mes en Pesos Colombianos
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Balance Neto del Mes",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(netBalance),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalance >= 0) AccentPrimaryLight else AccentRed
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Ingresos
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AccentPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentPrimaryLight, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Ingresos", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "+${CurrencyFormatter.format(totalIncome)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            // Gastos
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AccentRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Gastos", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "-${CurrencyFormatter.format(totalExpenses)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Botones de Registro Ultrarrápido (Fricción Cero)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onQuickExpenseClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚡ - Gasto (1s)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Button(
                        onClick = onQuickIncomeClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Ingreso (1s)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // Título de Actividad Reciente
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Movimientos Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Toca para editar",
                        fontSize = 11.sp,
                        color = AccentBlue
                    )
                }
            }

            // Lista de Transacciones
            if (recentTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Aún no tienes movimientos este mes.", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Toca '⚡ - Gasto' o '+ Ingreso' para registrar al instante.", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(recentTransactions) { item ->
                    TransactionRowItem(
                        item = item,
                        onClick = { transactionToEdit = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Modal de Edición de Transacción (Requerimiento 7)
    transactionToEdit?.let { item ->
        EditTransactionDialog(
            transaction = item,
            availableCategories = categories.filter { it.type == item.type },
            onDismiss = { transactionToEdit = null },
            onSave = { updatedAmount, updatedCatId, updatedNote ->
                onUpdateTransaction(item.id, updatedAmount, updatedCatId, updatedNote)
                transactionToEdit = null
            },
            onDelete = {
                onDeleteTransaction(item.id)
                transactionToEdit = null
            }
        )
    }
}

@Composable
fun TransactionRowItem(
    item: TransactionWithCategory,
    onClick: () -> Unit
) {
    val isExpense = item.type == "EXPENSE"
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(android.graphics.Color.parseColor(item.categoryColor)).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.categoryName.take(1),
                    color = Color(android.graphics.Color.parseColor(item.categoryColor)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.note?.ifBlank { item.categoryName } ?: item.categoryName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.categoryName,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (item.source == "WIDGET") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Widget",
                                color = AccentBlue,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else if (item.source == "CHAT_NLP") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Chat",
                                color = AccentPurple,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatWithSign(item.amount, isExpense),
                    color = if (isExpense) AccentRed else AccentPrimaryLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = dateFormatter.format(Date(item.date)),
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Diálogo interactivo para editar descripción, monto o categoría de un gasto ya registrado (Requerimiento 7)
 */
@Composable
fun EditTransactionDialog(
    transaction: TransactionWithCategory,
    availableCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, categoryId: String, note: String) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(transaction.amount.toLong().toString()) }
    var noteText by remember { mutableStateOf(transaction.note ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transaction.type == "EXPENSE") "Editar Gasto" else "Editar Ingreso",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Monto en COP
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto en COP ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Observación o Descripción
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Observación / Detalle") },
                    placeholder = { Text("Ej: Compra en D1, Almuerzo con amigos...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Categoría
                Text("Cambiar Categoría:", fontSize = 12.sp, color = TextSecondary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableCategories) { cat ->
                        val isSelected = cat.id == selectedCategoryId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AccentPrimary else DarkSurfaceVariant,
                            modifier = Modifier.clickable { selectedCategoryId = cat.id }
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: transaction.amount
                    onSave(amount, selectedCategoryId, noteText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            Row {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AccentRed)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        },
        containerColor = DarkSurface
    )
}
