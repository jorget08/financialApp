package com.financialapp.ui.screens.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.dao.CategoryExpenseSummary
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.remote.ai.AIAssistantService
import com.financialapp.data.remote.ai.AISettingsManager
import com.financialapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAdvisorScreen(
    totalIncome: Double,
    totalExpenses: Double,
    expensesByCategory: List<CategoryExpenseSummary>,
    savingsAccount: SavingsAccountEntity?,
    aiService: AIAssistantService,
    settingsManager: AISettingsManager,
    onNavigateToSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var auditResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasApiKey = remember(settingsManager) { settingsManager.isConfigured() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Asesor Financiero IA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Auditoría Patrimonial & Reducción de Gastos", fontSize = 11.sp, color = TextSecondary)
                        }
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
            // Verificación de API Key
            if (!hasApiKey) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AccentOrange.copy(alpha = 0.15f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentOrange)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("API Key no configurada", fontWeight = FontWeight.Bold, color = AccentOrange, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Para que el asesor pueda analizar tus ingresos, gastos y rendimientos, ingresa tu propia clave de Google Gemini u OpenAI en Ajustes.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Configurar mi API Key ahora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Banner del Asesor
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "¿Listo para auditar tus finanzas?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "La IA revisará tus gastos por categoría, calculará tu tasa de ahorro real y contrastará la TEA de tu banco frente a alternativas de mayor rendimiento.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = aiService.auditFinances(
                                        totalIncome = totalIncome,
                                        totalExpenses = totalExpenses,
                                        expensesByCategory = expensesByCategory,
                                        savingsAccount = savingsAccount
                                    )
                                    result.onSuccess { text ->
                                        auditResult = text
                                        isLoading = false
                                    }.onFailure { err ->
                                        errorMessage = err.localizedMessage ?: "Error al conectar con la IA."
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && hasApiKey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Auditando con IA...", fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generar Auditoría Financiera", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Mensaje de Error si ocurre
            if (errorMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentRed.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = AccentRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Resultado de la Auditoría del Asesor
            if (auditResult != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = DarkSurface,
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentPurple.copy(alpha = 0.5f))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Diagnóstico del Asesor Senior",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AccentPurple
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DarkBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = auditResult ?: "",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
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
