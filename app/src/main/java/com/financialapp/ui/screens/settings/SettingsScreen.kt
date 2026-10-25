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
import com.financialapp.data.remote.ai.AISettingsManager
import com.financialapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: AISettingsManager
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(settingsManager.getApiKey()) }
    var selectedProvider by remember { mutableStateOf(settingsManager.getProvider()) }
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
                            text = "Tu clave de API se almacena únicamente en el chip seguro de tu Xiaomi 14T Pro. Moneda configurada: Pesos Colombianos (COP).",
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
                            val providers = listOf(
                                AISettingsManager.PROVIDER_GEMINI,
                                AISettingsManager.PROVIDER_OPENAI
                            )
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
                                        selectedModel = if (prov == AISettingsManager.PROVIDER_GEMINI) "gemini-2.5-flash" else "gpt-4o"
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
                        Text("Modelo de IA Especializado", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        val availableModels = if (selectedProvider == AISettingsManager.PROVIDER_GEMINI) {
                            listOf(
                                "gemini-2.5-flash" to "Gemini 2.5 Flash (Recomendado: Rápido + Búsqueda)",
                                "gemini-2.5-pro" to "Gemini 2.5 Pro (Máximo Razonamiento)",
                                "gemini-2.0-flash" to "Gemini 2.0 Flash"
                            )
                        } else {
                            listOf(
                                "gpt-4o" to "GPT-4o (Omni)",
                                "gpt-4o-mini" to "GPT-4o Mini"
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((modelId, modelLabel) in availableModels) {
                                val isSelected = selectedModel == modelId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AccentPurple.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentPurple)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    onClick = { selectedModel = modelId }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo de API Key
                        Text("Tu API Key Privada", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: AIzaSy... (Gemini) o sk-... (OpenAI)", color = TextTertiary, fontSize = 13.sp) },
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
                                settingsManager.saveApiKey(apiKey)
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

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
