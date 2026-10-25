package com.financialapp.ui.screens.quickentry

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.ui.theme.FinancialAppTheme
import com.financialapp.util.CurrencyFormatter
import com.financialapp.widget.QuickExpenseGlanceWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Actividad Flotante Ultrarrápida (<50ms de arranque).
 * Permite registrar tanto GASTOS como INGRESOS con mínima fricción en Pesos Colombianos (COP).
 * Soporta monto rápido con botón '000', categoría y observación opcional.
 */
class QuickExpenseDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialType = intent.getStringExtra(EXTRA_ENTRY_TYPE) ?: "EXPENSE"

        setContent {
            FinancialAppTheme {
                QuickExpenseDialogContent(
                    initialType = initialType,
                    onDismiss = { finish() },
                    onSave = { amount, type, categoryId, note ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getInstance(applicationContext)
                            val transaction = TransactionEntity(
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                note = note.ifBlank { if (type == "EXPENSE") "Gasto rápido" else "Ingreso" },
                                source = "WIDGET",
                                date = System.currentTimeMillis()
                            )
                            db.transactionDao().insertTransaction(transaction)

                            // Actualizar widget de escritorio
                            QuickExpenseGlanceWidget().updateAll(applicationContext)

                            withContext(Dispatchers.Main) {
                                val label = if (type == "EXPENSE") "Gasto" else "Ingreso"
                                Toast.makeText(applicationContext, "$label guardado: ${CurrencyFormatter.format(amount)}", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_ENTRY_TYPE = "extra_entry_type" // "EXPENSE" o "INCOME"
    }
}

@Composable
fun QuickExpenseDialogContent(
    initialType: String = "EXPENSE",
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    var entryType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var allCategories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        db.categoryDao().getAllCategoriesFlow()
            .flowOn(Dispatchers.IO)
            .collect { categories ->
                allCategories = categories
            }
    }

    // Filtrar categorías según tipo seleccionado (Gasto o Ingreso)
    val filteredCategories = remember(entryType, allCategories) {
        allCategories.filter { it.type == entryType }
    }

    LaunchedEffect(filteredCategories) {
        if (filteredCategories.isNotEmpty() && (selectedCategoryId == null || filteredCategories.none { it.id == selectedCategoryId })) {
            selectedCategoryId = filteredCategories.first().id
        }
    }

    val currentAmount = amountText.toDoubleOrNull() ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF161B22),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Barra Superior con Botón Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Registro Rápido (COP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector Tipo: [ 🔴 Gasto | 🟢 Ingreso ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isExpense = entryType == "EXPENSE"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isExpense) Color(0xFFF85149) else Color(0xFF21262D),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { entryType = "EXPENSE" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🔴 Gasto",
                                fontWeight = FontWeight.Bold,
                                color = if (isExpense) Color.White else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isExpense) Color(0xFF238636) else Color(0xFF21262D),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { entryType = "INCOME" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🟢 Ingreso",
                                fontWeight = FontWeight.Bold,
                                color = if (!isExpense) Color.White else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pantalla del Monto en COP
                Text(
                    text = if (amountText.isEmpty()) "$ 0" else CurrencyFormatter.format(currentAmount),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entryType == "EXPENSE") Color(0xFFF85149) else Color(0xFF2EA043),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selector de Categorías (Chips grandes)
                Text(
                    text = "Categoría:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8B949E)
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(88.dp)
                ) {
                    items(filteredCategories) { category ->
                        val isSelected = category.id == selectedCategoryId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) (if (entryType == "EXPENSE") Color(0xFFF85149) else Color(0xFF238636)) else Color(0xFF21262D),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryId = category.id }
                        ) {
                            Text(
                                text = category.name,
                                color = if (isSelected) Color.White else Color(0xFFC9D1D9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Campo de Observación / Comentario Opcional
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Observación o detalle (opcional)", fontSize = 11.sp, color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF58A6FF),
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Teclado Numérico con botón '000' especial para Pesos Colombianos
                NumericKeypadCop(
                    onDigitPress = { digit ->
                        if (amountText.length < 10) {
                            amountText += digit
                        }
                    },
                    onDeletePress = {
                        if (amountText.isNotEmpty()) {
                            amountText = amountText.dropLast(1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Guardar
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val catId = selectedCategoryId
                        if (amount > 0.0 && catId != null) {
                            onSave(amount, entryType, catId, noteText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entryType == "EXPENSE") Color(0xFFF85149) else Color(0xFF238636)
                    ),
                    enabled = currentAmount > 0.0 && selectedCategoryId != null
                ) {
                    Text(
                        text = if (entryType == "EXPENSE") "✓ Guardar Gasto (1s)" else "✓ Guardar Ingreso (1s)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NumericKeypadCop(
    onDigitPress: (String) -> Unit,
    onDeletePress: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "⌫") // Botón '000' para miles en COP
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (key in row) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (key == "000") Color(0xFF238636).copy(alpha = 0.3f) else Color(0xFF21262D),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                if (key == "⌫") onDeletePress() else onDigitPress(key)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                color = if (key == "000") Color(0xFF2EA043) else Color.White,
                                fontSize = if (key == "000") 14.sp else 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
