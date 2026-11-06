package com.financialapp.data.remote.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AiSource(val title: String, val url: String)

data class AiAnswer(val text: String, val sources: List<AiSource> = emptyList())

/** Convierte las respuestas REST de Gemini y OpenAI. Separado de la red para poder probarlo. */
object AiResponseParsers {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseGemini(body: String): AiAnswer {
        val root = json.parseToJsonElement(body).jsonObject
        val candidate = root.array("candidates")?.firstOrNull()?.jsonObject
            ?: throw AiException(geminiBlockReason(root) ?: "Gemini no devolvió ninguna respuesta.")
        val text = candidate.obj("content")?.array("parts")
            ?.mapNotNull { it.jsonObject.string("text") }
            ?.joinToString("")
            .orEmpty()
        if (text.isBlank()) {
            throw AiException("Gemini no generó texto (${candidate.string("finishReason") ?: "sin motivo"}).")
        }
        val sources = candidate.obj("groundingMetadata")?.array("groundingChunks")
            ?.mapNotNull { chunk ->
                chunk.jsonObject.obj("web")?.let { web ->
                    web.string("uri")?.let { AiSource(web.string("title") ?: it, it) }
                }
            }
            .orEmpty()
        return AiAnswer(text, sources.distinctBy { it.url })
    }

    private fun geminiBlockReason(root: JsonObject): String? =
        root.obj("promptFeedback")?.string("blockReason")?.let { "Gemini bloqueó la consulta: $it." }

    fun parseOpenAi(body: String): AiAnswer {
        val root = json.parseToJsonElement(body).jsonObject
        val contents = root.array("output").orEmpty()
            .map { it.jsonObject }
            .filter { it.string("type") == "message" }
            .flatMap { it.array("content").orEmpty() }
            .map { it.jsonObject }
        contents.firstNotNullOfOrNull { it.string("refusal") }?.let { throw AiException("OpenAI no respondió: $it") }
        val text = contents.mapNotNull { it.string("text") }.joinToString("")
        if (text.isBlank()) throw AiException("OpenAI no generó texto.")
        val sources = contents.flatMap { it.array("annotations").orEmpty() }
            .map { it.jsonObject }
            .filter { it.string("type") == "url_citation" }
            .mapNotNull { a -> a.string("url")?.let { AiSource(a.string("title") ?: it, it) } }
        return AiAnswer(text, sources.distinctBy { it.url })
    }

    /** Mensaje del proveedor para errores HTTP; ambos lo envían en error.message. */
    fun parseErrorMessage(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject.obj("error")?.string("message")
    }.getOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = (this[key] as? JsonObject)
    private fun JsonObject.array(key: String): JsonArray? = (this[key] as? JsonArray)
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonElement)?.let {
        runCatching { it.jsonPrimitive.contentOrNull }.getOrNull()
    }
}

class AiException(message: String) : Exception(message)

@Serializable
data class SuggestionCard(
    val titulo: String = "",
    val accion: String = "",
    val porque: String = "",
    val riesgo: String = "",
    val horizonte: String = ""
)

object SuggestionCards {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** Lista vacía si no vino el JSON esperado; entonces se muestra el texto completo. */
    fun parse(text: String): List<SuggestionCard> {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return runCatching {
            json.decodeFromString<List<SuggestionCard>>(text.substring(start, end + 1))
                .filter { it.titulo.isNotBlank() || it.accion.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
