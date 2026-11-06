package com.financialapp.domain.engine

/**
 * Resultado estructurado del análisis de texto libre adaptado a Colombia (COP).
 */
data class ParsedTransactionResult(
    val type: TransactionType,
    val amount: Double,
    val concept: String,
    val suggestedCategoryName: String,
    val isConfident: Boolean = true
)

enum class TransactionType {
    EXPENSE,
    INCOME
}

/**
 * Parser de Lenguaje Natural Local Ultrarrápido optimizado para Colombia (COP).
 * Capaz de procesar:
 * - "Gasté 15k en café" -> $15.000
 * - "Pagué 85 mil de internet" -> $85.000
 * - "50 lucas de taxi" -> $50.000
 * - "Entraron 2.5m de sueldo" -> $2.500.000
 * - "Gasté 120000 en el mercado del hogar" -> $120.000
 */
object NaturalLanguageExpenseParser {

    private val expenseRegex = Regex(
        """(?:gast[eé]|pagu[eé]|compr[eé]|salieron?|di)\s*(?:\$|cop)?\s*(\d+(?:[.,]\d+)?\s*(?:k|mil|lucas|m|millones)?)\s*(?:en|de|por)?\s*(.*)""",
        RegexOption.IGNORE_CASE
    )

    private val incomeRegex = Regex(
        """(?:entr(?:[oó]|aron)|cobr[eé]|ingres[oó]|recib[ií]|sueldo|transferencia|quincena)\s*(?:\$|cop)?\s*(\d+(?:[.,]\d+)?\s*(?:k|mil|lucas|m|millones)?)\s*(?:de|en|por)?\s*(.*)""",
        RegexOption.IGNORE_CASE
    )

    // Formato ultracorto: "15k café", "50 lucas taxi", "25000 almuerzo"
    private val shorthandRegex = Regex(
        """^(?:\$|cop)?\s*(\d+(?:[.,]\d+)?\s*(?:k|mil|lucas|m|millones)?)\s+(.+)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(input: String): ParsedTransactionResult? {
        val clean = input.trim()
        if (clean.isBlank()) return null

        // 1. Gasto explícito
        expenseRegex.find(clean)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val rawConcept = match.groupValues[2].trim().ifEmpty { "Gasto rápido" }
            val category = categorizeExpense(rawConcept)
            return ParsedTransactionResult(
                type = TransactionType.EXPENSE,
                amount = amount,
                concept = rawConcept,
                suggestedCategoryName = category,
                isConfident = true
            )
        }

        // 2. Ingreso explícito
        incomeRegex.find(clean)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val rawConcept = match.groupValues[2].trim().ifEmpty { "Ingreso recibido" }
            val category = categorizeIncome(rawConcept)
            return ParsedTransactionResult(
                type = TransactionType.INCOME,
                amount = amount,
                concept = rawConcept,
                suggestedCategoryName = category,
                isConfident = true
            )
        }

        // 3. Shorthand "15k almuerzo"
        shorthandRegex.find(clean)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val rawConcept = match.groupValues[2].trim()
            val category = categorizeExpense(rawConcept)
            return ParsedTransactionResult(
                type = TransactionType.EXPENSE,
                amount = amount,
                concept = rawConcept,
                suggestedCategoryName = category,
                isConfident = false
            )
        }

        return null
    }

    /**
     * Interpreta sufijos colombianos: "15k" -> 15000, "50 lucas" -> 50000, "2.5m" -> 2500000
     */
    private fun parseAmount(raw: String): Double? {
        val clean = raw.lowercase().trim()
        return when {
            clean.endsWith("k") -> {
                val num = clean.removeSuffix("k").trim().replace(',', '.')
                (num.toDoubleOrNull() ?: return null) * 1_000.0
            }
            clean.endsWith("mil") || clean.endsWith("lucas") -> {
                val num = clean.replace("mil", "").replace("lucas", "").trim().replace(',', '.')
                (num.toDoubleOrNull() ?: return null) * 1_000.0
            }
            clean.endsWith("m") -> {
                val num = clean.removeSuffix("m").trim().replace(',', '.')
                (num.toDoubleOrNull() ?: return null) * 1_000_000.0
            }
            clean.endsWith("millones") || clean.endsWith("millon") -> {
                val num = clean.replace("millones", "").replace("millon", "").trim().replace(',', '.')
                (num.toDoubleOrNull() ?: return null) * 1_000_000.0
            }
            else -> {
                val normalized = clean.replace(".", "").replace(',', '.')
                normalized.toDoubleOrNull()
            }
        }
    }

    fun categorizeExpense(concept: String): String {
        val lower = concept.lowercase()
        return when {
            lower.containsAny("luz", "agua", "internet", "gas", "telefono", "celular", "arriendo", "alquiler", "administracion", "claro", "tigo", "movistar", "epm", "enel", "acueducto", "factura") -> "Casa y Servicios"
            lower.containsAny("hogar", "mueble", "aseo", "limpieza", "reparacion", "d1", "ara", "exito", "jumbo", "mercado", "supermercado", "compras del mes", "cocina") -> "Hogar"
            lower.containsAny("salud", "bienestar", "farmacia", "drogueria", "medicina", "doctor", "cita", "eps", "gym", "gimnasio", "suplementos", "odontologo", "dentista", "psicologo") -> "Salud y Bienestar"
            lower.containsAny("entretenimiento", "ocio", "cine", "salida", "cerveza", "bar", "rumba", "discoteca", "fiesta", "netflix", "spotify", "juegos", "play", "steam", "restaurante", "café", "cafe", "comida", "hamburguesa", "pizza", "domicilio", "rappi") -> "Entretenimiento y Ocio"
            lower.containsAny("uber", "taxi", "didi", "cabify", "gasolina", "pasaje", "transmilenio", "sitp", "metro", "mio", "peaje", "parqueadero") -> "Transporte"
            else -> "Otros Gastos"
        }
    }

    fun categorizeIncome(concept: String): String {
        val lower = concept.lowercase()
        return when {
            lower.containsAny("sueldo", "salario", "quincena", "nomina", "pago") -> "Sueldo / Salario"
            lower.containsAny("honorarios", "freelance", "proyecto", "trabajo extra", "asesoria") -> "Honorarios"
            lower.containsAny("rendimiento", "interes", "dividendo", "inversion", "cdt") -> "Inversiones"
            else -> "Otros Ingresos"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
