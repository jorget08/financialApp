package com.financialapp.domain.engine

import com.financialapp.data.local.entities.MovementType
import java.text.Normalizer

data class ParsedNotification(
    val amount: Double?,
    val type: String?, // Null si no se pudo saber si entró o salió plata
    val merchant: String?,
    val suggestedCategoryName: String
)

/**
 * Interpreta notificaciones de apps financieras. Devuelve null cuando no es un movimiento.
 * El texto de cada entidad cambia con el tiempo, por eso no depende de una plantilla exacta:
 * busca monto y verbos, y ante la duda lo deja para que el usuario lo revise.
 */
object NotificationParser {

    // Nunca se guarda nada que pueda ser una credencial o un código
    private val sensitive = listOf(
        "codigo", "clave", "contrasena", "otp", "token", "pin ", "verificacion", "dinamica"
    )
    private val failed = listOf(
        "rechazad", "fallid", "declinad", "no pudimos", "no se pudo", "no fue exitos", "no fue posible", "cancelad"
    )
    private val promo = listOf(
        "gana ", "ganate", "descuento", "promo", "oferta", "aprovecha", "hasta ", "cashback", "participa", "te invitamos", "pre aprobado", "preaprobado"
    )
    private val incomeWords = listOf(
        "recibiste", "te envio", "te enviaron", "te llego", "te llegaron", "te consignaron", "te transfirio", "te transfirieron", "abono a tu", "depositaron", "ingreso a tu"
    )
    private val expenseWords = listOf(
        "enviaste", "pagaste", "pago ", "compra", "compraste", "retiraste", "retiro", "transferiste", "transferencia", "recargaste", "recarga", "debito", "cargo", "avance"
    )

    private val moneyWithSymbol = Regex("""\$\s?(\d[\d.,]*)""")
    private val moneyWithWord = Regex("""(\d[\d.,]*)\s?(?:cop|pesos)\b""", RegexOption.IGNORE_CASE)
    private val merchantRegex = Regex("""\b(?:en|a|de)\s+([\p{L}\d][\p{L}\d&'*./ -]{1,40})""")
    private val endOfMerchant = Regex("""\s+(?:por|el|con|desde|hoy|ayer|a las|tu saldo)\b.*""", RegexOption.IGNORE_CASE)

    fun parse(title: String?, text: String?): ParsedNotification? {
        val full = listOfNotNull(title, text).joinToString(". ").trim()
        if (full.isBlank()) return null
        val normalized = normalize(full)

        if (sensitive.any { normalized.contains(it) }) return null
        if (failed.any { normalized.contains(it) }) return null

        val isIncome = incomeWords.any { normalized.contains(it) }
        val isExpense = !isIncome && expenseWords.any { normalized.contains(it) }
        val amount = parseAmount(full)
        val looksLikeMovement = isIncome || isExpense

        if (promo.any { normalized.contains(it) } && (!looksLikeMovement || amount == null)) return null
        if (!looksLikeMovement && amount == null) return null

        val merchant = extractMerchant(text ?: full, amount != null)
        return ParsedNotification(
            amount = amount,
            type = when {
                isIncome -> MovementType.INCOME
                isExpense -> MovementType.EXPENSE
                else -> null
            },
            merchant = merchant,
            suggestedCategoryName = if (isIncome) {
                NaturalLanguageExpenseParser.categorizeIncome(merchant ?: "")
            } else {
                NaturalLanguageExpenseParser.categorizeExpense(merchant ?: "")
            }
        )
    }

    fun parseAmount(text: String): Double? {
        val raw = moneyWithSymbol.find(text)?.groupValues?.get(1)
            ?: moneyWithWord.find(text)?.groupValues?.get(1)
            ?: return null
        return parseColombianNumber(raw)?.takeIf { it > 0 }
    }

    /**
     * "15.000" = 15000, "1.500.000" = 1500000, "15.000,50" = 15000.5, "1,500,000.00" = 1500000.
     * Un separador final seguido de 1 o 2 dígitos son centavos; cualquier otro agrupa miles.
     */
    fun parseColombianNumber(raw: String): Double? {
        val clean = raw.trimEnd('.', ',')
        if (clean.isEmpty()) return null
        val lastSeparator = clean.indexOfLast { it == '.' || it == ',' }
        if (lastSeparator < 0) return clean.toDoubleOrNull()
        val decimals = clean.length - lastSeparator - 1
        return if (decimals in 1..2) {
            val whole = clean.substring(0, lastSeparator).filter { it.isDigit() }
            "${whole.ifEmpty { "0" }}.${clean.substring(lastSeparator + 1)}".toDoubleOrNull()
        } else {
            clean.filter { it.isDigit() }.toDoubleOrNull()
        }
    }

    private fun extractMerchant(text: String, hasAmount: Boolean): String? {
        val searchFrom = if (hasAmount) {
            (moneyWithSymbol.find(text) ?: moneyWithWord.find(text))?.range?.last?.plus(1) ?: 0
        } else {
            0
        }
        val match = merchantRegex.find(text, searchFrom) ?: return null
        return match.groupValues[1]
            .replace(endOfMerchant, "")
            .trim(' ', '.', '-', '*')
            .takeIf { it.length >= 2 }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
