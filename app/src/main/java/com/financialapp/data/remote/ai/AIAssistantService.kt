package com.financialapp.data.remote.ai

import com.financialapp.data.local.dao.CategoryExpenseSummary
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.domain.engine.ParsedTransactionResult
import com.financialapp.domain.engine.TransactionType
import com.financialapp.util.CurrencyFormatter
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Servicio Integrador de Inteligencia Artificial para el Asesor Financiero.
 * Configurado con modelos de última generación (Gemini 2.5 Flash / Pro).
 * Especializado en finanzas personales para Colombia (COP), cuentas de alto rendimiento
 * (ej. Nu Cajitas 13% E.A., Lulo Bank, Pibank, CDTs) y optimización del costo de vida.
 */
class AIAssistantService(
    private val settingsManager: AISettingsManager
) {

    private val systemInstruction = """
        Eres un Asesor Financiero Senior de élite y planificador patrimonial especializado en finanzas personales en Colombia.
        Tu moneda de trabajo es el Peso Colombiano (COP).
        
        TUS PRINCIPIOS Y REGLAS FUNDAMENTALES:
        1. Contexto Colombiano Actual:
           - Conoces las cuentas de ahorro de alto rendimiento reguladas por la Superfinanciera en Colombia (ej: Nu Colombia con Cajitas al 13% E.A., Lulo Bank al 13% E.A., Pibank al 12% E.A., RappiPay, CDTs de entidades vigiladas).
           - Conoces el impuesto del 4x1000 (GMF) y recuerdas la importancia de tener una cuenta exenta.
        2. Sinceridad absoluta:
           - Si el usuario tiene fugas de capital en "Entretenimiento y Ocio", salidas o compras impulsivas, indícaselo con cifras exactas en pesos colombianos (COP).
           - Si en "Casa y Servicios" o "Hogar" hay gastos elevados, sugiere alternativas prácticas de optimización (ej: comprar básicos en D1/Ara, revisar planes de telecomunicaciones).
        3. Tasa de Ahorro:
           - Evalúa el ratio (Ingresos - Gastos) / Ingresos. Si es menor al 20%, enciende las alarmas de ahorro.
        4. Cuenta de Ahorros e Interés Compuesto:
           - Si su cuenta de ahorros ofrece una tasa E.A. menor al 10% - 12% E.A., compárala de inmediato con las alternativas de alto rendimiento disponibles en el mercado colombiano para que su dinero no pierda contra la inflación.
        5. Formato de Salida:
           - Siempre entrega 3 consejos concretos, prácticos e inmediatos para recortar gastos esta semana en Colombia.
           - Entrega 1 meta estratégica para su cuenta de ahorros a 12 meses con interés compuesto.
        6. Tono: Profesional, directo, empático pero estricto en la disciplina financiera.
    """.trimIndent()

    /**
     * Realiza una auditoría exhaustiva del mes y entrega un diagnóstico con recomendaciones.
     */
    suspend fun auditFinances(
        totalIncome: Double,
        totalExpenses: Double,
        expensesByCategory: List<CategoryExpenseSummary>,
        savingsAccount: SavingsAccountEntity?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No se ha configurado la API Key de IA en Ajustes."))
        }

        try {
            val netSavings = totalIncome - totalExpenses
            val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) * 100 else 0.0

            val categoriesDossier = expensesByCategory.joinToString("\n") {
                "- ${it.categoryName}: ${CurrencyFormatter.format(it.totalAmount)} (${it.transactionCount} registros)"
            }

            val savingsDossier = savingsAccount?.let {
                "Banco: ${it.bankName} | Saldo Actual: ${CurrencyFormatter.format(it.initialBalance)} | Tasa E.A.: ${it.teaRate}% E.A. | Aporte mensual planeado: ${CurrencyFormatter.format(it.plannedMonthlyDeposit)}"
            } ?: "Actualmente el usuario no ha configurado una cuenta de ahorro formal."

            val userPrompt = """
                AQUÍ TIENES MI DOSSIER FINANCIERO DEL MES (EN PESOS COLOMBIANOS - COP):
                
                - Ingresos Totales: ${CurrencyFormatter.format(totalIncome)}
                - Gastos Totales: ${CurrencyFormatter.format(totalExpenses)}
                - Balance Neto Disponible: ${CurrencyFormatter.format(netSavings)}
                - Tasa de Ahorro Real: ${String.format("%.1f", savingsRate)}%
                
                DESGLOSE DE GASTOS POR CATEGORÍA:
                $categoriesDossier
                
                ESTADO DE CUENTA DE AHORROS:
                $savingsDossier
                
                Por favor, como mi Asesor Financiero en Colombia:
                1. ¿Dónde están mis mayores excesos o fugas de dinero este mes?
                2. ¿Qué opinas de mi tasa de ahorro y del rendimiento que me da mi banco? ¿Qué cuentas o instrumentos en Colombia me recomendarías para rentabilizar mejor mis ahorros?
                3. Dame 3 acciones prácticas para reducir costos esta misma semana y 1 meta para mis ahorros a 12 meses.
            """.trimIndent()

            val generativeModel = GenerativeModel(
                modelName = settingsManager.getModelName(),
                apiKey = apiKey,
                systemInstruction = com.google.ai.client.generativeai.type.content {
                    text(systemInstruction)
                }
            )

            val response = generativeModel.generateContent(userPrompt)
            val responseText = response.text ?: "No se recibió respuesta del modelo."
            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fallback por IA para interpretar textos complejos cuando la expresión regular local falla.
     */
    suspend fun parseComplexTransaction(text: String): Result<ParsedTransactionResult> = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API Key requerida"))
        }

        try {
            val prompt = """
                Analiza la siguiente frase sobre gastos o ingresos en Colombia (Pesos Colombianos - COP) y extrae la información en JSON:
                Frase: "$text"
                
                Devuelve ÚNICAMENTE un JSON con esta estructura exacta sin texto adicional:
                {
                   "type": "EXPENSE" o "INCOME",
                   "amount": 50000.0,
                   "concept": "pago de recibo",
                   "category": "Casa y Servicios" (o Hogar, Salud y Bienestar, Entretenimiento y Ocio, Transporte, Otros Gastos, Sueldo / Salario, Honorarios, Inversiones)
                }
            """.trimIndent()

            val generativeModel = GenerativeModel(
                modelName = settingsManager.getModelName(),
                apiKey = apiKey
            )

            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
            val json = JSONObject(jsonText)

            val typeStr = json.optString("type", "EXPENSE")
            val amount = json.optDouble("amount", 0.0)
            val concept = json.optString("concept", text)
            val category = json.optString("category", "Otros Gastos")

            Result.success(
                ParsedTransactionResult(
                    type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                    amount = amount,
                    concept = concept,
                    suggestedCategoryName = category,
                    isConfident = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
