package com.financialapp.ui.screens.advisor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.data.local.entities.AiSuggestionEntity
import com.financialapp.data.remote.ai.AIAssistantService
import com.financialapp.data.remote.ai.AdvisorPrompts
import com.financialapp.data.remote.ai.AiMessage
import com.financialapp.data.remote.ai.AiRequest
import com.financialapp.data.remote.ai.SuggestionCard
import com.financialapp.data.remote.ai.SuggestionCards
import com.financialapp.data.remote.ai.SuggestionTopic
import com.financialapp.ui.components.FinanceCard
import com.financialapp.ui.components.financeFieldColors
import com.financialapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SuggestionsTab(
    aiService: AIAssistantService,
    hasApiKey: Boolean,
    suggestions: List<AiSuggestionEntity>,
    loadDossier: suspend () -> String,
    onSave: suspend (AiSuggestionEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var activeTopic by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var thinkingAbout by remember { mutableStateOf("") }
    val isLoading = activeTopic != null

    fun ask(label: String, request: String, useWeb: Boolean) {
        activeTopic = label
        error = null
        scope.launch {
            val prompt = AdvisorPrompts.withDossier(loadDossier(), request)
            aiService.complete(
                AiRequest(AdvisorPrompts.suggestionSystem, listOf(AiMessage(true, prompt)), webSearch = useWeb, deep = true)
            ).onSuccess { answer ->
                onSave(
                    AiSuggestionEntity(
                        topic = label,
                        question = request,
                        content = answer.text,
                        sources = answer.sources.joinToString("\n") { "${it.title.replace('|', ' ')}|${it.url}" },
                        providerModel = aiService.providerModelLabel
                    )
                )
            }.onFailure { error = it.message ?: "No se pudo obtener la sugerencia." }
            activeTopic = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item {
            Text("¿Sobre qué quieres ideas?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Mira tus movimientos, ahorros y metas. Agrega tus metas (terreno, moto...) para que pueda priorizarlas.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        val topics = SuggestionTopic.entries.filter { it != SuggestionTopic.CAN_I_AFFORD }
        items(topics.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { topic ->
                    OutlinedButton(
                        onClick = { ask(topic.label, topic.request, topic.useWeb) },
                        enabled = hasApiKey && !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(topic.label, fontSize = 12.sp, color = TextPrimary, maxLines = 2)
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        item {
            FinanceCard {
                Text("¿Será que compro esto?", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = thinkingAbout,
                    onValueChange = { thinkingAbout = it },
                    placeholder = { Text("Ej: moto de 9 millones a crédito", color = TextTertiary, fontSize = 13.sp) },
                    colors = financeFieldColors(),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val item = thinkingAbout.trim()
                        thinkingAbout = ""
                        ask(SuggestionTopic.CAN_I_AFFORD.label, AdvisorPrompts.canIAfford(item), false)
                    },
                    enabled = hasApiKey && !isLoading && thinkingAbout.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pedir opinión") }
            }
        }
        item {
            Text(
                "Son ideas, no asesoría certificada. Ninguna IA puede saber si una acción va a subir: toda inversión puede perder valor y la decisión es tuya.",
                color = TextTertiary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        if (isLoading) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentPurple)
                    Text("Analizando: $activeTopic. Puede tardar un par de minutos.", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        error?.let {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = AccentRed.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = AccentRed, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
        items(suggestions, key = { it.id }) { suggestion ->
            SuggestionResult(suggestion, onDelete = { onDelete(suggestion.id) })
        }
    }
}

@Composable
private fun SuggestionResult(suggestion: AiSuggestionEntity, onDelete: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")) }
    val cards = remember(suggestion.content) { SuggestionCards.parse(suggestion.content) }
    val sources = remember(suggestion.sources) {
        suggestion.sources.lines().mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }

    FinanceCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.topic, color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${dateFormat.format(Date(suggestion.createdAt))} • ${suggestion.providerModel}", color = TextTertiary, fontSize = 10.sp)
            }
            TextButton(onClick = onDelete) { Text("Borrar", color = TextSecondary, fontSize = 11.sp) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (cards.isEmpty()) {
            Text(suggestion.content, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
        } else {
            cards.forEachIndexed { index, card ->
                if (index > 0) HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                CardBody(card)
            }
        }
        if (sources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Fuentes", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            sources.forEach { (title, url) ->
                Text(
                    text = title,
                    color = AccentBlue,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            // Las URL vienen de internet: solo se abren enlaces web normales
                            if (url.startsWith("https://") || url.startsWith("http://")) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun CardBody(card: SuggestionCard) {
    if (card.titulo.isNotBlank()) {
        Text(card.titulo, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
    }
    if (card.accion.isNotBlank()) {
        Text(card.accion, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
    }
    Labeled("Por qué", card.porque)
    Labeled("Riesgo", card.riesgo)
    Labeled("Cuándo", card.horizonte)
}

@Composable
private fun Labeled(label: String, value: String) {
    if (value.isBlank()) return
    Spacer(modifier = Modifier.height(4.dp))
    Text("$label: $value", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
}
