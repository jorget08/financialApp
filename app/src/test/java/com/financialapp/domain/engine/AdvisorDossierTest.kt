package com.financialapp.domain.engine

import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.SavingsAccountEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvisorDossierTest {

    private val review = PeriodReview("Mes", income = 3_000_000.0, expenses = 1_000_000.0, saved = 0.0, byCategory = emptyMap())
    private val account = SavingsAccountEntity(name = "Ahorro", bankName = "Banco", teaRate = 9.0, initialBalance = 6_000_000.0)

    private fun dossier(savings: SavingsAccountEntity?, goals: List<GoalEntity>) = AdvisorDossier.build(
        DossierInput(
            reviews = listOf(review),
            accounts = emptyList(),
            savings = savings,
            goals = goals,
            bills = emptyList(),
            budgets = emptyList(),
            loans = emptyList()
        )
    )

    @Test
    fun `las metas no se presentan como una prioridad elegida`() {
        val text = dossier(
            account,
            listOf(
                GoalEntity(name = "Terreno", targetAmount = 10_000_000.0, priority = 0),
                GoalEntity(name = "Casa", targetAmount = 20_000_000.0, priority = 1)
            )
        )

        assertFalse(text.contains("en su orden de prioridad"))
        assertTrue(text.contains("sin prioridad definida"))
        assertTrue(text.indexOf("- Casa") < text.indexOf("- Terreno"))
    }

    @Test
    fun `lo asignado a metas no cuenta como fondo de emergencia`() {
        val text = dossier(account, listOf(GoalEntity(name = "Moto", targetAmount = 10_000_000.0, savedAmount = 2_000_000.0)))

        assertTrue(text.contains("cubre 4.0 meses"))
        assertTrue(text.contains("no es plata adicional"))
    }

    @Test
    fun `avisa si lo asignado supera el saldo`() {
        val text = dossier(account, listOf(GoalEntity(name = "Moto", targetAmount = 10_000_000.0, savedAmount = 7_000_000.0)))

        assertTrue(text.contains("supera el saldo"))
        assertTrue(text.contains("cubre 0.0 meses"))
    }

    @Test
    fun `sin metas asignadas todo el saldo cuenta y no menciona asignaciones`() {
        val text = dossier(account, listOf(GoalEntity(name = "Moto", targetAmount = 10_000_000.0)))

        assertTrue(text.contains("cubre 6.0 meses"))
        assertFalse(text.contains("asignados a metas"))
        assertFalse(text.contains("no es plata adicional"))
    }
}
