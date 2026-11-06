package com.financialapp.ui.screens.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.domain.engine.BudgetCalculator
import com.financialapp.domain.engine.BudgetLevel
import com.financialapp.domain.engine.BudgetStatus
import com.financialapp.ui.components.*
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    periodRange: Pair<Long, Long>,
    periodLabel: String,
    onSave: (categoryId: String, limit: Double) -> Unit,
    onDelete: (categoryId: String) -> Unit,
    onBack: () -> Unit
) {
    val statuses = remember(budgets, categories, transactions, periodRange) {
        BudgetCalculator.statuses(budgets, categories, transactions, periodRange)
    }
    val expenseCategories = categories.filter { it.type == "EXPENSE" }
    val free = expenseCategories.filter { category -> budgets.none { it.categoryId == category.id } }
    var editing by remember { mutableStateOf<BudgetStatus?>(null) }
    var creating by remember { mutableStateOf(false) }

    ToolScaffold(
        "Presupuestos • $periodLabel",
        onBack,
        onAdd = if (free.isNotEmpty()) ({ creating = true }) else null,
        addLabel = "Presupuesto"
    ) {
        if (statuses.isEmpty()) {
            item {
                EmptyCard(
                    "Ponle un tope a tus gastos",
                    "Define cuánto quieres gastar al mes por categoría. Te avisamos al llegar al 80 % y al 100 %."
                )
            }
        } else {
            item {
                val spent = statuses.sumOf { it.spent }
                val limit = statuses.sumOf { it.limit }
                FinanceCard {
                    Text("Total con presupuesto", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        "${CurrencyFormatter.format(spent)} de ${CurrencyFormatter.format(limit)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
        items(statuses, key = { it.category.id }) { status ->
            val (color, label) = levelStyle(status)
            FinanceCard(onClick = { editing = status }) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(status.category.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("${(status.ratio * 100).roundToInt()}%", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                ProgressBar(status.ratio, color)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${CurrencyFormatter.format(status.spent)} de ${CurrencyFormatter.format(status.limit)} • $label",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }

    if (creating) {
        BudgetDialog(free, null, { creating = false }, { id, limit -> onSave(id, limit); creating = false }, null)
    }
    editing?.let { status ->
        BudgetDialog(
            categories = listOf(status.category),
            status = status,
            onDismiss = { editing = null },
            onSave = { id, limit -> onSave(id, limit); editing = null },
            onDelete = { onDelete(status.category.id); editing = null }
        )
    }
}

private fun levelStyle(status: BudgetStatus): Pair<Color, String> = when (status.level) {
    BudgetLevel.OVER -> AccentRed to "Te pasaste por ${CurrencyFormatter.format(-status.remaining)}"
    BudgetLevel.WARNING -> AccentOrange to "Quedan ${CurrencyFormatter.format(status.remaining)}: ve con cuidado"
    BudgetLevel.OK -> AccentBlue to "Quedan ${CurrencyFormatter.format(status.remaining)}"
}

@Composable
private fun BudgetDialog(
    categories: List<CategoryEntity>,
    status: BudgetStatus?,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    var categoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var limitText by remember { mutableStateOf(status?.limit?.toLong()?.toString() ?: "") }
    val limit = parseMoney(limitText)

    FormDialog(
        title = status?.category?.name ?: "Nuevo presupuesto",
        canSave = limit != null && categoryId != null,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onSave = { onSave(categoryId!!, limit!!) }
    ) {
        if (status == null) CategoryPicker(categories, categoryId) { categoryId = it }
        MoneyField(limitText, { limitText = it }, "Tope mensual en COP ($)")
    }
}

/** Gestión de categorías propias. Una categoría en uso no se puede borrar. */
@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onSave: (CategoryEntity) -> Unit,
    onDelete: suspend (CategoryEntity) -> Boolean,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ToolScaffold("Categorías", onBack, onAdd = { creating = true }, addLabel = "Categoría") {
        listOf("EXPENSE" to "Gastos", "INCOME" to "Ingresos").forEach { (type, label) ->
            item { SectionLabel(label) }
            items(categories.filter { it.type == type }, key = { it.id }) { category ->
                FinanceCard(onClick = { editing = category }) {
                    Text(category.name, color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }

    if (creating || editing != null) {
        CategoryDialog(
            category = editing,
            nextOrder = (categories.maxOfOrNull { it.displayOrder } ?: 0) + 1,
            onDismiss = { creating = false; editing = null },
            onSave = { onSave(it); creating = false; editing = null },
            onDelete = editing?.let { category ->
                {
                    scope.launch {
                        if (onDelete(category)) {
                            editing = null
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "No se puede borrar: tiene movimientos o pagos fijos.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    Unit
                }
            }
        )
    }
}

private val categoryPalette = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#3F51B5", "#2196F3", "#009688", "#4CAF50", "#FF9800", "#795548", "#607D8B"
)

@Composable
private fun CategoryDialog(
    category: CategoryEntity?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: "EXPENSE") }
    var color by remember { mutableStateOf(category?.colorHex ?: categoryPalette.first()) }

    FormDialog(
        title = if (category == null) "Nueva categoría" else "Editar categoría",
        canSave = name.isNotBlank(),
        onDismiss = onDismiss,
        onDelete = onDelete,
        onSave = {
            onSave(
                (category ?: CategoryEntity(name = name.trim(), iconName = "label", colorHex = color, type = type, displayOrder = nextOrder))
                    .copy(name = name.trim(), colorHex = color, type = type)
            )
        }
    ) {
        FinanceTextField(name, { name = it }, "Nombre", "Ej: Moto, Mascotas")
        if (category == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceChip("Gasto", type == "EXPENSE") { type = "EXPENSE" }
                ChoiceChip("Ingreso", type == "INCOME") { type = "INCOME" }
            }
        }
        Text("Color", fontSize = 12.sp, color = TextSecondary)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categoryPalette) { hex ->
                val selected = hex == color
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(android.graphics.Color.parseColor(hex)),
                    border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, TextPrimary) else null,
                    onClick = { color = hex },
                    modifier = Modifier.size(32.dp)
                ) {}
            }
        }
    }
}
