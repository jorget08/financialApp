package com.financialapp.data.remote.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.beta.messages.BetaMessage
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.BetaWebSearchTool20250305
import com.anthropic.models.beta.messages.BetaWebSearchTool20260209
import com.anthropic.models.beta.messages.MessageCreateParams
import java.time.Duration

/** Claude mediante el SDK oficial de Anthropic. */
object ClaudeClient {

    private const val MAX_TOKENS = 16_000L
    private const val MAX_CONTINUATIONS = 5
    private const val MAX_SEARCHES = 5L
    private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"

    fun complete(apiKey: String, model: String, request: AiRequest): AiAnswer {
        val client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            // Con búsqueda web una sola respuesta puede tardar varios minutos
            .timeout(Duration.ofMinutes(5))
            .build()
        try {
            var builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(request.system)
                .outputConfig(
                    BetaOutputConfig.builder()
                        .effort(if (request.deep) BetaOutputConfig.Effort.HIGH else BetaOutputConfig.Effort.MEDIUM)
                        .build()
                )

            // Si el modelo declina por seguridad, el servidor reintenta con otro. Haiku no lo admite.
            if (!model.contains("haiku")) {
                builder = builder
                    .addBeta(FALLBACK_BETA)
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            }
            if (request.webSearch) {
                builder = if (model.contains("haiku")) {
                    builder.addTool(BetaWebSearchTool20250305.builder().maxUses(MAX_SEARCHES).build())
                } else {
                    builder.addTool(BetaWebSearchTool20260209.builder().maxUses(MAX_SEARCHES).build())
                }
            }
            request.messages.forEach { message ->
                builder = if (message.fromUser) {
                    builder.addUserMessage(message.text)
                } else {
                    builder.addAssistantMessage(message.text)
                }
            }

            var response = client.beta().messages().create(builder.build())
            val texts = StringBuilder()
            val sources = mutableListOf<AiSource>()
            collect(response, texts, sources)

            // La búsqueda web puede pausar el turno: se reanuda enviando la respuesta tal cual
            var continuations = 0
            while (response.stopReason().orElse(null) == BetaStopReason.PAUSE_TURN && continuations < MAX_CONTINUATIONS) {
                builder = builder.addMessage(response)
                response = client.beta().messages().create(builder.build())
                collect(response, texts, sources)
                continuations++
            }

            when (response.stopReason().orElse(null)) {
                BetaStopReason.REFUSAL -> throw AiException("Claude no pudo responder a esta consulta. Prueba a reformularla.")
                BetaStopReason.MAX_TOKENS -> if (texts.isBlank()) throw AiException("La respuesta quedó incompleta. Intenta de nuevo.")
                else -> Unit
            }
            if (texts.isBlank()) throw AiException("Claude no generó texto.")
            return AiAnswer(texts.toString(), sources.distinctBy { it.url })
        } catch (e: AnthropicServiceException) {
            throw AiException(friendlyError(e.statusCode()))
        } finally {
            client.close()
        }
    }

    private fun collect(response: BetaMessage, texts: StringBuilder, sources: MutableList<AiSource>) {
        response.content().forEach { block ->
            block.text().ifPresent { textBlock ->
                texts.append(textBlock.text())
                textBlock.citations().ifPresent { citations ->
                    citations.forEach { citation ->
                        citation.webSearchResultLocation().ifPresent { location ->
                            sources.add(AiSource(location.title().orElse(location.url()), location.url()))
                        }
                    }
                }
            }
        }
    }

    private fun friendlyError(status: Int): String = when (status) {
        401 -> "Claude rechazó la API Key. Revísala en Ajustes."
        403 -> "Tu clave de Claude no tiene permiso para este modelo."
        404 -> "Ese modelo de Claude no existe. Elige otro en Ajustes."
        429 -> "Claude alcanzó el límite de uso de tu cuenta. Intenta en un momento."
        in 500..599 -> "Claude no está disponible en este momento. Intenta en un momento."
        else -> "Claude devolvió un error ($status)."
    }
}
