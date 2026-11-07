package com.financialapp.ui.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.financialapp.domain.engine.CategorySpending
import com.financialapp.domain.engine.ExpenseAnalyzer
import com.financialapp.domain.engine.MovementTotals
import com.financialapp.ui.components.EditTransactionDialog
import com.financialapp.ui.screens.home.TransactionRowItem
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import kotlin.math.roundToInt

private enum class ExpensePeriod(val label: String) {
    CURRENT("Este mes"),
    PREVIOUS("Mes anterior"),
    ALL("Todo")
}

/**
 * Cuánto se ha gastado por categoría y búsqueda en todo el historial
 * (ej. escribir "Camilo" para ver cada préstamo y cuánto falta por devolver).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    transactions: List<TransactionWithCategory>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    currentPeriodRange: Pair<Long, Long>,
    previousPeriodRange: Pair<Long, Long>,
    onUpdateTransaction: (id: String, edit: TransactionEdit) -> Unit,
    onDeleteTransaction: (id: String) -> Unit,
    onBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(ExpensePeriod.CURRENT) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionWithCategory?>(null) }

    val range = when (period) {
        ExpensePeriod.CURRENT -> currentPeriodRange
        ExpensePeriod.PREVIOUS -> previousPeriodRange
        ExpensePeriod.ALL -> null
    }
    val matches = remember(transactions, query, range) { ExpenseAnalyzer.filter(transactions, query, range) }
    val spending = remember(matches) { ExpenseAnalyzer.spendingByCategory(matches) }
    // Si la categoría elegida ya no tiene resultados, se ignora para no dejar la lista vacía sin salida
    val activeCategoryId = selectedCategoryId?.takeIf { id -> spending.any { it.categoryId == id } }
    val visible = remember(matches, activeCategoryId) {
        if (activeCategoryId == null) matches else matches.filter { it.categoryId == activeCategoryId }
    }
    val totals = remember(visible) { ExpenseAnalyzer.totals(visible) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Gastos y búsqueda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Fuera de la lista para que el campo no pierda el foco al desplazarse
            OutlinedTextField(
                value = query,
                onValueChange = { text ->
                    // Una búsqueda nueva mira todo el historial para que no se escape ningún movimiento antiguo
                    if (query.isBlank() && text.isNotBlank()) period = ExpensePeriod.ALL
                    query = text
                },
                placeholder = { Text("Buscar: Camilo, mercado, Netflix…", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                ExpensePeriod.entries.forEach { option ->
                    FilterChip(
                        selected = period == option,
                        onClick = { period = option },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = TextSecondary,
                            selectedContainerColor = AccentPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
            ) {
                item {
                    TotalsCard(totals = totals, isSearching = query.isNotBlank())
                }

                if (spending.isNotEmpty()) {
                    item {
                        SectionTitle(text = "Por categoría", hint = "Toca para filtrar")
                    }
                    items(spending, key = { "category_${it.categoryId}" }) { row ->
                        CategorySpendingRow(
                            row = row,
                            isSelected = row.categoryId == activeCategoryId,
                            onClick = {
                                selectedCategoryId = if (row.categoryId == activeCategoryId) null else row.categoryId
                            }
                        )
                    }
                }

                item {
                    SectionTitle(text = "Movimientos (${visible.size})", hint = "Toca para editar")
                }

                if (visible.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (query.isBlank()) "No hay movimientos en este periodo." else "Nada coincide con \"${query.trim()}\" en este periodo.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(visible, key = { it.id }) { item ->
                        TransactionRowItem(item = item, onClick = { transactionToEdit = item })
                    }
                }
            }
        }
    }

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
private fun TotalsCard(totals: MovementTotals, isSearching: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TotalColumn(
                    label = "Gastos (${totals.expenseCount})",
                    value = CurrencyFormatter.format(totals.expenses),
                    modifier = Modifier.weight(1f)
                )
                TotalColumn(
                    label = "Ingresos (${totals.incomeCount})",
                    value = CurrencyFormatter.format(totals.income),
                    modifier = Modifier.weight(1f)
                )
            }
            if (isSearching) {
                Spacer(modifier = Modifier.height(12.dp))
                TotalColumn(
                    label = "Salió − entró (lo que falta si es un préstamo)",
                    value = CurrencyFormatter.format(totals.outstanding)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Solo aparecen los movimientos con esta palabra en la observación o la categoría.",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TotalColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun SectionTitle(text: String, hint: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(hint, fontSize = 11.sp, color = AccentBlue)
    }
}

@Composable
private fun CategorySpendingRow(row: CategorySpending, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = if (isSelected) BorderStroke(1.dp, AccentBlue) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.categoryName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    Text(
                        text = "${row.count} ${if (row.count == 1) "movimiento" else "movimientos"}" + if (isSelected) " • Filtrando" else "",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(CurrencyFormatter.format(row.total), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatShare(row.share), color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(row.share.toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentBlue)
                )
            }
        }
    }
}

private fun formatShare(share: Double): String {
    val percent = (share * 100).roundToInt()
    return if (percent == 0 && share > 0.0) "<1%" else "$percent%"
}
