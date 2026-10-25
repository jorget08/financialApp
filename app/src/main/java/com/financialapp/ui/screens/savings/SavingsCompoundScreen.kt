package com.financialapp.ui.screens.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.domain.engine.CompoundInterestEngine
import com.financialapp.domain.model.MonthlyCompoundRecord
import com.financialapp.domain.model.SavingsSummary
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsCompoundScreen(
    primaryAccount: SavingsAccountEntity?,
    onSaveAccountConfig: (name: String, bank: String, tea: Double, initialBal: Double, monthlyDeposit: Double) -> Unit,
    onRecordNewDeposit: (amount: Double) -> Unit
) {
    var selectedHorizonMonths by remember { mutableIntStateOf(12) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ahorro & Interés Compuesto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (primaryAccount != null) {
                            Text("${primaryAccount.bankName} • ${primaryAccount.teaRate}% E.A. (COP)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            Text("Sin cuenta configurada", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                },
                actions = {
                    if (primaryAccount != null) {
                        IconButton(onClick = { showConfigDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurar", tint = AccentBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->

        // Si el usuario aún no ha configurado su cuenta de ahorro: Estado Vacío para que él ponga sus datos
        if (primaryAccount == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Configura tu Cuenta de Ahorros",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ingresa los datos reales de tu banco en Colombia (ej: Nu Colombia 13% E.A., Lulo Bank, Pibank o tu entidad), tu saldo inicial y cuánto planeas ahorrar mes a mes para calcular tus proyecciones exactas.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showConfigDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configurar mis Números Reales", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Cálculos matemáticos basados ESTRICTAMENTE en los números ingresados por el usuario
            val schedule: SavingsSummary = remember(primaryAccount, selectedHorizonMonths) {
                CompoundInterestEngine.generateSchedule(
                    initialCapital = primaryAccount.initialBalance,
                    teaPercentage = primaryAccount.teaRate,
                    plannedMonthlyDeposit = primaryAccount.plannedMonthlyDeposit,
                    totalMonths = selectedHorizonMonths
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tarjeta Resumen con Cifras en Pesos Colombianos
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurface,
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Saldo Proyectado (${selectedHorizonMonths} meses)", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        CurrencyFormatter.format(schedule.finalBalance),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple
                                    )
                                }
                                Button(
                                    onClick = { showDepositDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aportar", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = DarkBorder, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Total Aportado
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Capital Depositado", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        CurrencyFormatter.format(schedule.totalDeposited),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                // Total Intereses Ganados
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Interés Ganado (Puro)", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "+${CurrencyFormatter.format(schedule.totalInterestEarned)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPrimaryLight
                                    )
                                }

                                // TEM
                                Column(modifier = Modifier.weight(0.8f)) {
                                    Text("TEM (Efectiva Mensual)", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "${String.format(Locale.US, "%.3f", schedule.temPercentage)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }

                // Selector de Horizonte Temporal
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val horizons = listOf(6 to "6 Meses", 12 to "1 Año", 24 to "2 Años", 60 to "5 Años")
                        for ((months, label) in horizons) {
                            val isSelected = selectedHorizonMonths == months
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AccentPurple else DarkSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                onClick = { selectedHorizonMonths = months }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Explicación didáctica del interés no lineal
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentPurple.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Efecto bola de nieve: tus intereses pasados generan más intereses cada mes. Cada fila muestra cómo el dinero trabaja automáticamente sin esfuerzo adicional.",
                                fontSize = 11.sp,
                                color = Color(0xFFD2A8FF),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Encabezado de la Tabla de Control Mensual
                item {
                    Text(
                        text = "Tabla de Control y Proyección Mes a Mes (COP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Cabecera de Columnas
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Período", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text("Aporte", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("Interés (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                            Text("Saldo Acumulado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                        }
                    }
                }

                // Filas de la Tabla
                items(schedule.records) { record ->
                    SavingsTableRowCop(record)
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Diálogo de configuración de cuenta
    if (showConfigDialog) {
        SavingsConfigDialogCop(
            currentAccount = primaryAccount,
            onDismiss = { showConfigDialog = false },
            onConfirm = { name, bank, tea, initialBal, monthlyDeposit ->
                onSaveAccountConfig(name, bank, tea, initialBal, monthlyDeposit)
                showConfigDialog = false
            }
        )
    }

    // Diálogo para registrar aporte
    if (showDepositDialog && primaryAccount != null) {
        RecordDepositDialogCop(
            defaultAmount = primaryAccount.plannedMonthlyDeposit,
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount ->
                onRecordNewDeposit(amount)
                showDepositDialog = false
            }
        )
    }
}

@Composable
fun SavingsTableRowCop(record: MonthlyCompoundRecord) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale("es", "CO")) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mes
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mes ${record.monthIndex}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = record.date.format(monthFormatter),
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }

            // Depósito
            Text(
                text = "+${CurrencyFormatter.format(record.deposit)}",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )

            // Interés del mes (crece no linealmente)
            Text(
                text = "+${CurrencyFormatter.format(record.interestEarned)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentPrimaryLight,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.1f)
            )

            // Saldo Acumulado
            Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(record.endBalance),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun SavingsConfigDialogCop(
    currentAccount: SavingsAccountEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, bank: String, tea: Double, initialBal: Double, monthlyDeposit: Double) -> Unit
) {
    var bankName by remember { mutableStateOf(currentAccount?.bankName ?: "Nu Colombia (Cajitas)") }
    var accountName by remember { mutableStateOf(currentAccount?.name ?: "Mi Ahorro Alto Rendimiento") }
    var teaText by remember { mutableStateOf(currentAccount?.teaRate?.toString() ?: "13.0") }
    var initialBalText by remember { mutableStateOf(currentAccount?.initialBalance?.toLong()?.toString() ?: "0") }
    var monthlyDepositText by remember { mutableStateOf(currentAccount?.plannedMonthlyDeposit?.toLong()?.toString() ?: "200000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar mi Cuenta de Ahorro", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Nombre del Banco (ej: Nu, Lulo, Pibank)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teaText,
                    onValueChange = { teaText = it },
                    label = { Text("Tasa Efectiva Anual (% E.A.)") },
                    placeholder = { Text("Ej: 13.0 para 13% E.A.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialBalText,
                    onValueChange = { initialBalText = it },
                    label = { Text("Saldo Actual / Capital Inicial (COP $)") },
                    placeholder = { Text("Ej: 0 o tu saldo actual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthlyDepositText,
                    onValueChange = { monthlyDepositText = it },
                    label = { Text("Aporte Mensual Meta (COP $)") },
                    placeholder = { Text("Ej: 200000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tea = teaText.toDoubleOrNull() ?: 13.0
                    val init = initialBalText.toDoubleOrNull() ?: 0.0
                    val monthly = monthlyDepositText.toDoubleOrNull() ?: 0.0
                    onConfirm(accountName, bankName, tea, init, monthly)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Guardar mis Números")
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

@Composable
fun RecordDepositDialogCop(
    defaultAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(defaultAmount.toLong().toString()) }

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) onConfirm(amount)
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
