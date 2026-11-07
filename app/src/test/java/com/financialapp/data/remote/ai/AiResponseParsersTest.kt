package com.financialapp.data.remote.ai

import com.financialapp.data.backup.BackupCodec
import com.financialapp.data.backup.BackupFile
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.TransactionEntity
import com.financialapp.domain.engine.AdvisorDossier
import com.financialapp.domain.engine.DossierInput
import com.financialapp.domain.engine.LoanLedger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseParsersTest {

    @Test
    fun testGeminiTextAndSources() {
        val body = """{"candidates":[{"content":{"parts":[{"text":"Hola "},{"text":"mundo"}]},
            "groundingMetadata":{"groundingChunks":[{"web":{"uri":"https://a.co","title":"A"}},{"web":{"uri":"https://a.co","title":"A"}}]}}]}"""

        val answer = AiResponseParsers.parseGemini(body)

        assertEquals("Hola mundo", answer.text)
        assertEquals(listOf(AiSource("A", "https://a.co")), answer.sources)
    }

    @Test(expected = AiException::class)
    fun testGeminiBlockedPromptIsAnError() {
        AiResponseParsers.parseGemini("""{"promptFeedback":{"blockReason":"SAFETY"}}""")
    }

    @Test
    fun testOpenAiSkipsSearchStepsAndReadsCitations() {
        val body = """{"output":[{"type":"web_search_call","id":"ws"},
            {"type":"message","content":[{"type":"output_text","text":"Respuesta",
            "annotations":[{"type":"url_citation","url":"https://b.co","title":"B"}]}]}]}"""

        val answer = AiResponseParsers.parseOpenAi(body)

        assertEquals("Respuesta", answer.text)
        assertEquals("https://b.co", answer.sources.single().url)
    }

    @Test
    fun testErrorMessageIsOptional() {
        assertEquals("mal", AiResponseParsers.parseErrorMessage("""{"error":{"message":"mal"}}"""))
        assertEquals(null, AiResponseParsers.parseErrorMessage("no es json"))
    }

    @Test
    fun testSuggestionCardsToleratesTextAroundJson() {
        val cards = SuggestionCards.parse("""Claro: [{"titulo":"Moto","accion":"Ahorra","porque":"x"}] listo""")

        assertEquals("Moto", cards.single().titulo)
        assertEquals("", cards.single().riesgo)
    }

    @Test
    fun testSuggestionCardsFallBackWhenNotJson() {
        assertTrue(SuggestionCards.parse("Te recomiendo [esto] primero").isEmpty())
    }

    @Test
    fun testDossierNeverIncludesPersonNames() {
        val loans = LoanLedger.summarize(
            listOf(TransactionEntity(amount = 50_000.0, type = "LOAN_OUT", categoryId = "c", personName = "Camilo", note = "Secreto"))
        )

        val dossier = AdvisorDossier.build(
            DossierInput(emptyList(), emptyList(), null, emptyList(), emptyList(), emptyList(), loans)
        )

        assertFalse(dossier.contains("Camilo"))
        assertFalse(dossier.contains("Secreto"))
        assertTrue(dossier.contains("me deben $ 50.000"))
    }

    private fun validBackup() = BackupFile(
        categories = listOf(CategoryEntity(id = "c", name = "Comida", iconName = "", colorHex = "", type = "EXPENSE")),
        transactions = listOf(TransactionEntity(id = "t", amount = 10.0, type = "EXPENSE", categoryId = "c", note = "=SUM(A1)")),
        accounts = listOf(AccountEntity(id = "a", name = "Nequi", type = "BANK", openingBalance = 5.0))
    )

    @Test
    fun testBackupRoundTripKeepsEveryField() {
        val backup = validBackup()

        val restored = BackupCodec.decode(BackupCodec.encode(backup)).getOrThrow()

        assertEquals(backup, restored)
    }

    @Test
    fun testBackupRejectsBrokenFilesBeforeTouchingData() {
        assertTrue(BackupCodec.decode("esto no es json").isFailure)
        assertTrue(BackupCodec.decode("{}").isFailure)
        val orphan = validBackup().copy(
            transactions = listOf(TransactionEntity(amount = 1.0, type = "EXPENSE", categoryId = "no-existe"))
        )
        assertNotNull(BackupCodec.validate(orphan))
        assertNotNull(BackupCodec.validate(validBackup().copy(backupVersion = 99)))
    }

    @Test
    fun testCsvEscapesQuotesAndFormulas() {
        val csv = BackupCodec.toCsv(
            listOf(TransactionEntity(amount = 10.0, type = "EXPENSE", categoryId = "c", note = "=HACK \"x\"", date = 0L)),
            mapOf("c" to "Comida"),
            emptyMap()
        )

        assertTrue(csv.contains("\"'=HACK \"\"x\"\"\""))
    }
}
