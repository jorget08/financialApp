package com.financialapp.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialapp.domain.engine.NaturalLanguageExpenseParser
import com.financialapp.domain.engine.ParsedTransactionResult
import com.financialapp.domain.engine.TransactionType
import com.financialapp.ui.theme.*
import com.financialapp.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val parsedData: ParsedTransactionResult? = null,
    val isConfirmed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    BOT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickChatScreen(
    initialSharedText: String? = null,
    onSaveTransaction: (amount: Double, type: String, categoryName: String, note: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = MessageSender.BOT,
                text = "¡Hola! Soy tu asistente de registro rápido para Colombia (COP). Escríbeme o reenvíame mensajes como en WhatsApp:\n\n• \"Gasté 15k en almuerzo\"\n• \"Pagué 85 mil de internet\"\n• \"Entraron 2.5m de sueldo\"\n• \"50 lucas taxi\"",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            processUserMessage(initialSharedText, messages, onSaveTransaction)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(AccentPrimaryLight)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Registro por Chat (WhatsApp Style)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Motor NLP Local en Pesos (0ms)", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ej: 'Gasté 35k en cena' o '50 lucas taxi'...", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 3
                    )

                    FloatingActionButton(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isNotEmpty()) {
                                inputText = ""
                                processUserMessage(text, messages, onSaveTransaction)
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        containerColor = AccentPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onConfirm = {
                        val parsed = message.parsedData ?: return@ChatBubble
                        val typeStr = if (parsed.type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                        onSaveTransaction(parsed.amount, typeStr, parsed.suggestedCategoryName, parsed.concept)
                        val index = messages.indexOfFirst { it.id == message.id }
                        if (index != -1) {
                            messages[index] = message.copy(isConfirmed = true)
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

private fun processUserMessage(
    text: String,
    messages: MutableList<ChatMessage>,
    onSaveTransaction: (amount: Double, type: String, categoryName: String, note: String) -> Unit
) {
    messages.add(ChatMessage(sender = MessageSender.USER, text = text))

    val parsed = NaturalLanguageExpenseParser.parse(text)

    if (parsed != null) {
        val typeStr = if (parsed.type == TransactionType.INCOME) "INCOME" else "EXPENSE"
        val typeLabel = if (parsed.type == TransactionType.INCOME) "Ingreso" else "Gasto"

        onSaveTransaction(parsed.amount, typeStr, parsed.suggestedCategoryName, parsed.concept)

        messages.add(
            ChatMessage(
                sender = MessageSender.BOT,
                text = "✓ Registrado exitosamente:\n• Tipo: $typeLabel\n• Monto: ${CurrencyFormatter.format(parsed.amount)}\n• Categoría: ${parsed.suggestedCategoryName}\n• Detalle: ${parsed.concept}",
                parsedData = parsed,
                isConfirmed = true
            )
        )
    } else {
        messages.add(
            ChatMessage(
                sender = MessageSender.BOT,
                text = "No logré identificar el monto o concepto. Prueba expresiones como:\n• 'Gasté 25k en almuerzo'\n• '50 lucas taxi'\n• 'Entraron 2.5m de sueldo'"
            )
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onConfirm: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) AccentPrimary.copy(alpha = 0.9f) else DarkSurfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                if (message.parsedData != null && !message.isConfirmed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar y Guardar", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    fontSize = 9.sp,
                    color = TextTertiary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
