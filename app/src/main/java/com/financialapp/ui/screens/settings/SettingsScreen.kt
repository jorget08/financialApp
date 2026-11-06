package com.financialapp.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.capture.AppPreferences
import com.financialapp.data.backup.BackupManager
import com.financialapp.data.remote.ai.AISettingsManager
import com.financialapp.ui.components.financeFieldColors
import com.financialapp.ui.theme.*
import com.financialapp.util.FinancialMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: AISettingsManager,
    preferences: AppPreferences,
    backupManager: BackupManager,
    monthStartDay: Int,
    onMonthStartDayChange: (Int) -> Unit,
    onOpenCategories: () -> Unit
) {
    val context = LocalContext.current
    var selectedProvider by remember { mutableStateOf(settingsManager.getProvider()) }
    var apiKey by remember { mutableStateOf(settingsManager.getApiKey(selectedProvider)) }
    var selectedModel by remember { mutableStateOf(settingsManager.getModelName()) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes & Claves de IA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Seguridad Cifrada (Hardware Keystore)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tu clave se guarda cifrada en este teléfono. Al usar el asesor, el proveedor elegido recibe un resumen de tus cifras, nunca nombres de personas ni observaciones de movimientos.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Proveedor de IA
                        Text("Proveedor de Inteligencia Artificial", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val providers = AISettingsManager.PROVIDERS
                            for (prov in providers) {
                                val isSelected = selectedProvider == prov
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AccentPrimary else DarkSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    onClick = {
                                        selectedProvider = prov
                                        apiKey = settingsManager.getApiKey(prov)
                                        selectedModel = AISettingsManager.defaultModel(prov)
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(prov, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selección de Modelo
                        Text("Modelo", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        val availableModels = AISettingsManager.modelsFor(selectedProvider)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((modelId, modelLabel) in availableModels) {
                                val isSelected = selectedModel == modelId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AccentPurple.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentPurple)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 42.dp),
                                    onClick = { selectedModel = modelId }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedModel = modelId },
                                            colors = RadioButtonDefaults.colors(selectedColor = AccentPurple)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = modelLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it },
                            label = { Text("O escribe otro modelo") },
                            singleLine = true,
                            colors = financeFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo de API Key
                        Text("Tu API Key de $selectedProvider", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(AISettingsManager.keyHint(selectedProvider), color = TextTertiary, fontSize = 13.sp) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                settingsManager.saveApiKey(selectedProvider, apiKey)
                                settingsManager.saveProvider(selectedProvider)
                                settingsManager.saveModelName(selectedModel)
                                Toast.makeText(context, "Configuración guardada en Keystore seguro", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Configuración Segura", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { CaptureCard(preferences) }

            item { BackupCard(backupManager) }

            // Mes financiero: día en que empieza el mes (día de pago)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Mi mes empieza el día",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ponlo en el día que te pagan. Lo que registres desde ese día cuenta para el mes siguiente.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FilledTonalButton(
                                onClick = { onMonthStartDayChange(monthStartDay - 1) },
                                enabled = monthStartDay > 1
                            ) { Text("−", fontSize = 18.sp) }
                            Text(
                                text = monthStartDay.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            FilledTonalButton(
                                onClick = { onMonthStartDayChange(monthStartDay + 1) },
                                enabled = monthStartDay < FinancialMonth.MAX_START_DAY
                            ) { Text("+", fontSize = 18.sp) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mes actual: ${FinancialMonth.currentPeriodLabel(monthStartDay)}",
                            fontSize = 12.sp,
                            color = AccentPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                OutlinedButton(onClick = onOpenCategories, modifier = Modifier.fillMaxWidth()) {
                    Text("Administrar categorías", color = TextPrimary)
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
