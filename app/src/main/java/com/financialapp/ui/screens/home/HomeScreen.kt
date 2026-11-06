package com.financialapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.TransactionEdit
import com.financialapp.data.local.dao.TransactionWithCategory
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.MovementType
import com.financialapp.ui.components.EditTransactionDialog
import com.financialapp.ui.navigation.ToolRoute
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
    accounts: List<AccountEntity>,
    totalIncome: Double,
    totalExpenses: Double,
    totalSaved: Double,
    pendingCount: Int,
    periodLabel: String,
    onQuickExpenseClick: () -> Unit,
    onQuickIncomeClick: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onUpdateTransaction: (id: String, edit: TransactionEdit) -> Unit,
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
                            text = "Balance Neto • $periodLabel",
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

                        // El ahorro no es un gasto, pero esa plata ya no está disponible
                        if (totalSaved > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Apartado a ahorro: ${CurrencyFormatter.format(totalSaved)} • Disponible: ${CurrencyFormatter.format(netBalance - totalSaved)}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Movimientos detectados en notificaciones
            if (pendingCount > 0) {
                item {
                    Button(
                        onClick = { onOpenRoute(ToolRoute.PENDING) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) {
                        Text(
                            if (pendingCount == 1) "1 movimiento por confirmar" else "$pendingCount movimientos por confirmar",
                            fontWeight = FontWeight.Bold
                        )
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

            // Acceso a Gastos por Categoría y Búsqueda
            item {
                OutlinedButton(
                    onClick = onOpenExpenses,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gastos por categoría y búsqueda", color = TextPrimary)
                }
            }

            // Herramientas
            item {
                val tools = listOf(
                    "Cuentas" to ToolRoute.ACCOUNTS,
                    "Préstamos" to ToolRoute.LOANS,
                    "Presupuestos" to ToolRoute.BUDGETS,
                    "Pagos fijos" to ToolRoute.BILLS,
                    "Metas" to ToolRoute.GOALS,
                    "Revisión" to ToolRoute.REVIEW
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tools.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { (label, route) ->
                                Surface(
                                    onClick = { onOpenRoute(route) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurface,
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(label, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                                    }
                                }
                            }
                        }
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
            categories = categories,
            accounts = accounts,
            onDismiss = { transactionToEdit = null },
            onSave = { edit ->
                onUpdateTransaction(item.id, edit)
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
    val isOutflow = MovementType.isOutflow(item.type, item.amount)
    val isMovedMoney = item.type != MovementType.EXPENSE && item.type != MovementType.INCOME
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
                    text = item.note?.ifBlank { null } ?: item.personName ?: item.categoryName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.personName?.let { "${item.categoryName} • $it" } ?: item.categoryName,
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
                    } else if (item.source == "NOTIFICATION") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Auto",
                                color = AccentOrange,
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
                    text = CurrencyFormatter.formatWithSign(kotlin.math.abs(item.amount), isOutflow),
                    color = when {
                        isMovedMoney -> AccentBlue
                        isOutflow -> AccentRed
                        else -> AccentPrimaryLight
                    },
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
