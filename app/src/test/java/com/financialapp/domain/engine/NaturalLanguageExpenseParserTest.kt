package com.financialapp.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NaturalLanguageExpenseParserTest {

    @Test
    fun testColombianExpenseParsing() {
        val res = NaturalLanguageExpenseParser.parse("Gasté 15k en café")
        assertNotNull(res)
        assertEquals(TransactionType.EXPENSE, res!!.type)
        assertEquals(15000.0, res.amount, 0.01)
        assertEquals("Entretenimiento y Ocio", res.suggestedCategoryName)

        val resService = NaturalLanguageExpenseParser.parse("Pagué 85 mil de internet")
        assertNotNull(resService)
        assertEquals(85000.0, resService!!.amount, 0.01)
        assertEquals("Casa y Servicios", resService.suggestedCategoryName)
    }

    @Test
    fun testColombianIncomeParsing() {
        val res = NaturalLanguageExpenseParser.parse("Entraron 2.5m de sueldo")
        assertNotNull(res)
        assertEquals(TransactionType.INCOME, res!!.type)
        assertEquals(2500000.0, res.amount, 0.01)
        assertEquals("Sueldo / Salario", res.suggestedCategoryName)
    }

    @Test
    fun testColombianSlangParsing() {
        val res = NaturalLanguageExpenseParser.parse("50 lucas taxi al aeropuerto")
        assertNotNull(res)
        assertEquals(TransactionType.EXPENSE, res!!.type)
        assertEquals(50000.0, res.amount, 0.01)
        assertEquals("Transporte", res.suggestedCategoryName)
    }
}
