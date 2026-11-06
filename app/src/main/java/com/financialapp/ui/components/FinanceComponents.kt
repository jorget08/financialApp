package com.financialapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pantalla estándar de herramientas: barra con volver, lista y botón opcional de agregar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)? = null,
    addLabel: String = "Agregar",
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            if (onAdd != null) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    containerColor = AccentPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(addLabel) }
                )
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
            content = content
        )
    }
}

@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(clickModifier)
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun EmptyCard(title: String, detail: String) {
    FinanceCard {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(detail, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
}

/** Barra de avance de un solo tono; el significado siempre va además en texto. */
@Composable
fun ProgressBar(fraction: Double, color: Color = AccentBlue) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(DarkSurfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

/** Texto de un monto en pesos a número. Null si está vacío, no es número o no es positivo. */
fun parseMoney(text: String): Double? =
    text.filter { it.isDigit() }.toDoubleOrNull()?.takeIf { it > 0 }

@Composable
fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String = "Monto en COP ($)") {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = financeFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun FinanceTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = TextTertiary) } },
        singleLine = true,
        colors = financeFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun financeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = DarkBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) AccentPrimary else DarkSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) Color.White else TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

@Composable
fun CategoryPicker(categories: List<CategoryEntity>, selectedId: String?, onSelect: (String) -> Unit) {
    Text("Categoría", fontSize = 12.sp, color = TextSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        items(categories, key = { it.id }) { category ->
            ChoiceChip(category.name, category.id == selectedId) { onSelect(category.id) }
        }
    }
}

/** Sin cuentas creadas no se muestra nada: registrar debe seguir siendo igual de rápido. */
@Composable
fun AccountPicker(
    accounts: List<AccountEntity>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    label: String = "Cuenta",
    allowNone: Boolean = true
) {
    if (accounts.isEmpty()) return
    Text(label, fontSize = 12.sp, color = TextSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        if (allowNone) {
            item { ChoiceChip("Sin cuenta", selectedId == null) { onSelect(null) } }
        }
        items(accounts, key = { it.id }) { account ->
            ChoiceChip(account.name, account.id == selectedId) { onSelect(account.id) }
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "CO"))

fun epochToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/** Hoy conserva la hora actual; un día anterior queda a mediodía para que el orden sea estable. */
fun localDateToEpoch(date: LocalDate, today: LocalDate = LocalDate.now()): Long {
    val zone = ZoneId.systemDefault()
    return if (date == today) {
        System.currentTimeMillis()
    } else {
        date.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
    }
}

/** Atajos Hoy/Ayer y calendario para registrar un gasto que se olvidó. Nunca fechas futuras. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(dateMillis: Long, onChange: (Long) -> Unit) {
    var showCalendar by remember { mutableStateOf(false) }
    val selected = epochToLocalDate(dateMillis)
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        ChoiceChip("Hoy", selected == today) { onChange(localDateToEpoch(today)) }
        ChoiceChip("Ayer", selected == yesterday) { onChange(localDateToEpoch(yesterday)) }
        val isOther = selected != today && selected != yesterday
        ChoiceChip(if (isOther) selected.format(dateFormatter) else "Otra fecha", isOther) { showCalendar = true }
    }

    if (showCalendar) {
        // El selector trabaja en UTC: se convierte para que el día elegido no se corra por la zona horaria
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        onChange(localDateToEpoch(Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    showCalendar = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** Diálogo estándar de formularios: desplazable y con guardar deshabilitado hasta que sea válido. */
@Composable
fun FormDialog(
    title: String,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Guardar",
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                content = content
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) { Text(saveLabel) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Eliminar", color = AccentRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
            }
        },
        containerColor = DarkSurface
    )
}
