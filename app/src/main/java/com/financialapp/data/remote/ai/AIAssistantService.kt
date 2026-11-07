package com.financialapp.data.remote.ai

import com.financialapp.domain.engine.ParsedTransactionResult
import com.financialapp.domain.engine.TransactionType
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class AiMessage(val fromUser: Boolean, val text: String)

data class AiRequest(
    val system: String,
    val messages: List<AiMessage>,
    val webSearch: Boolean = false,
    val deep: Boolean = true
)

/**
 * Asesor con el proveedor elegido en Ajustes. Antes siempre se llamaba a Gemini, aunque
 * el usuario hubiera elegido OpenAI: ahora cada proveedor usa su propia clave y API.
 */
class AIAssistantService(
    private val settingsManager: AISettingsManager
) {

    private val http by lazy {
        HttpClient(Android) {
            engine {
                connectTimeout = 30_000
                socketTimeout = 300_000
            }
        }
    }

    val providerModelLabel: String get() = "${settingsManager.getProvider()} • ${settingsManager.getModelName()}"

    suspend fun complete(request: AiRequest): Result<AiAnswer> = withContext(Dispatchers.IO) {
        val provider = settingsManager.getProvider()
        val apiKey = settingsManager.getApiKey(provider)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(AiException("No has configurado la API Key de $provider en Ajustes."))
        }
        val model = settingsManager.getModelName()
        try {
            Result.success(
                when (provider) {
                    AISettingsManager.PROVIDER_CLAUDE -> ClaudeClient.complete(apiKey, model, request)
                    AISettingsManager.PROVIDER_OPENAI -> callOpenAi(apiKey, model, request)
                    else -> callGemini(apiKey, model, request)
                }
            )
        } catch (e: AiException) {
            Result.failure(e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AiException("No se pudo conectar con $provider. Revisa tu internet e intenta de nuevo."))
        }
    }

    private suspend fun callGemini(apiKey: String, model: String, request: AiRequest): AiAnswer {
        val body = buildJsonObject {
            putJsonObject("system_instruction") {
                putJsonArray("parts") { addJsonObject { put("text", request.system) } }
            }
            putJsonArray("contents") {
                request.messages.forEach { message ->
                    addJsonObject {
                        put("role", if (message.fromUser) "user" else "model")
                        putJsonArray("parts") { addJsonObject { put("text", message.text) } }
                    }
                }
            }
            if (request.webSearch) {
                putJsonArray("tools") { addJsonObject { putJsonObject("google_search") {} } }
            }
        }
        val response = http.post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw httpError("Gemini", response.status.value, text)
        return AiResponseParsers.parseGemini(text)
    }

    private suspend fun callOpenAi(apiKey: String, model: String, request: AiRequest): AiAnswer {
        val body = buildJsonObject {
            put("model", model)
            put("instructions", request.system)
            putJsonArray("input") {
                request.messages.forEach { message ->
                    addJsonObject {
                        put("role", if (message.fromUser) "user" else "assistant")
                        put("content", message.text)
                    }
                }
            }
            if (request.webSearch) {
                putJsonArray("tools") { addJsonObject { put("type", "web_search") } }
            }
        }
        val response = http.post("https://api.openai.com/v1/responses") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw httpError("OpenAI", response.status.value, text)
        return AiResponseParsers.parseOpenAi(text)
    }

    private fun httpError(provider: String, status: Int, body: String): AiException {
        val detail = AiResponseParsers.parseErrorMessage(body)
        return AiException(
            when (status) {
                400, 401, 403 -> "$provider rechazó la solicitud. Revisa la API Key y el modelo en Ajustes." + (detail?.let { "\n$it" } ?: "")
                404 -> "$provider no encontró ese modelo. Elige otro en Ajustes."
                429 -> "$provider alcanzó el límite de uso de tu cuenta. Intenta en un momento."
                else -> "$provider devolvió un error ($status)."
            }
        )
    }

    /**
     * Fallback por IA para interpretar textos complejos cuando la expresión regular local falla.
     */
    suspend fun parseComplexTransaction(text: String): Result<ParsedTransactionResult> {
        val prompt = """
            Analiza esta frase sobre gastos o ingresos en Colombia (COP) y devuelve ÚNICAMENTE un JSON:
            {"type": "EXPENSE" o "INCOME", "amount": 50000.0, "concept": "pago de recibo", "category": "Casa y Servicios" (o Hogar, Salud y Bienestar, Entretenimiento y Ocio, Transporte, Otros Gastos, Sueldo / Salario, Honorarios, Inversiones)}
            Frase: "$text"
        """.trimIndent()
        return complete(AiRequest("Extraes datos de frases en español.", listOf(AiMessage(true, prompt)), deep = false))
            .mapCatching { answer ->
                val json = Json.parseToJsonElement(answer.text.substringAfter("{").substringBeforeLast("}").let { "{$it}" }).jsonObject
                val type = json["type"]?.jsonPrimitive?.content.orEmpty()
                ParsedTransactionResult(
                    type = if (type.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                    amount = json["amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    concept = json["concept"]?.jsonPrimitive?.content ?: text,
                    suggestedCategoryName = json["category"]?.jsonPrimitive?.content ?: "Otros Gastos",
                    isConfident = true
                )
            }
    }
}
