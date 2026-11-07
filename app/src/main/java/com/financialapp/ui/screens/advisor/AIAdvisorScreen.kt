package com.financialapp.ui.screens.advisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.financialapp.data.local.entities.AiSuggestionEntity
import com.financialapp.data.remote.ai.AIAssistantService
import com.financialapp.data.remote.ai.AISettingsManager
import com.financialapp.data.remote.ai.AdvisorPrompts
import com.financialapp.data.remote.ai.AiMessage
import com.financialapp.data.remote.ai.AiRequest
import com.financialapp.data.remote.ai.SuggestionTopic
import com.financialapp.ui.components.financeFieldColors
import com.financialapp.ui.theme.*
import kotlinx.coroutines.launch

/** La conversación vive mientras la app esté abierta; las sugerencias sí quedan guardadas. */
object AdvisorSession {
    val chat = mutableStateListOf<AiMessage>()
}

private enum class AdvisorTab(val label: String) { SUGGESTIONS("Sugerencias"), CHAT("Conversar") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAdvisorScreen(
    aiService: AIAssistantService,
    settingsManager: AISettingsManager,
    suggestions: List<AiSuggestionEntity>,
    loadDossier: suspend () -> String,
    onSaveSuggestion: suspend (AiSuggestionEntity) -> Unit,
    onDeleteSuggestion: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var tab by remember { mutableStateOf(AdvisorTab.SUGGESTIONS) }
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
                            Text(aiService.providerModelLabel, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tab.ordinal, containerColor = DarkBackground, contentColor = AccentPurple) {
                AdvisorTab.entries.forEach { option ->
                    Tab(
                        selected = tab == option,
                        onClick = { tab = option },
                        text = { Text(option.label, color = if (tab == option) TextPrimary else TextSecondary) }
                    )
                }
            }
            if (!hasApiKey) {
                MissingKeyCard(settingsManager.getProvider(), onNavigateToSettings)
            }
            when (tab) {
                AdvisorTab.SUGGESTIONS -> SuggestionsTab(aiService, hasApiKey, suggestions, loadDossier, onSaveSuggestion, onDeleteSuggestion)
                AdvisorTab.CHAT -> ChatTab(aiService, hasApiKey, loadDossier)
            }
        }
    }
}

@Composable
private fun MissingKeyCard(provider: String, onNavigateToSettings: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AccentOrange.copy(alpha = 0.15f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Falta la API Key de $provider", fontWeight = FontWeight.Bold, color = AccentOrange, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Agrega tu clave de Gemini, OpenAI o Claude en Ajustes para usar el asesor.", color = TextPrimary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNavigateToSettings,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configurar API Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatTab(aiService: AIAssistantService, hasApiKey: Boolean, loadDossier: suspend () -> String) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val messages = AdvisorSession.chat

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Pregúntame lo que quieras: \"¿será que me compro esto?\", \"¿cómo voy este mes?\", \"¿qué es un CDT?\". Conozco tus cifras, pero no los nombres ni las observaciones de tus movimientos.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            items(messages) { message ->
                val fromUser = message.fromUser
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (fromUser) AccentPrimary.copy(alpha = 0.9f) else DarkSurfaceVariant,
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Text(message.text, color = TextPrimary, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            if (isLoading) {
                item {
                    Text("Pensando...", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                }
            }
            error?.let {
                item { Text(it, color = AccentRed, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
            }
        }
        Surface(color = DarkSurface, modifier = Modifier.fillMaxWidth().imePadding()) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Escribe tu pregunta...", color = TextTertiary, fontSize = 13.sp) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = financeFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty() || isLoading) return@FilledIconButton
                        input = ""
                        error = null
                        messages.add(AiMessage(true, text))
                        isLoading = true
                        scope.launch {
                            val system = AdvisorPrompts.chatSystem + "\n\nESTADO ACTUAL DE SUS FINANZAS:\n" + loadDossier()
                            val result = aiService.complete(AiRequest(system, messages.toList(), webSearch = false, deep = false))
                            result.onSuccess { messages.add(AiMessage(false, it.text)) }
                                .onFailure { err ->
                                    // Se quita la pregunta para no enviar dos turnos de usuario seguidos al reintentar
                                    messages.removeAt(messages.lastIndex)
                                    input = text
                                    error = err.message ?: "No se pudo obtener respuesta."
                                }
                            isLoading = false
                        }
                    },
                    enabled = hasApiKey && !isLoading && input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentPurple, contentColor = Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}
