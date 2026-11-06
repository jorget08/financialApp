package com.financialapp.domain.engine

import com.financialapp.data.local.entities.MovementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Textos de ejemplo escritos a mano: hay que reemplazarlos con notificaciones reales cuando existan. */
class NotificationParserTest {

    @Test
    fun testOutgoingPaymentReadsAmountAndMerchant() {
        val parsed = NotificationParser.parse("Nequi", "Pagaste \$45.900 en Tienda D1 con tu tarjeta")

        assertNotNull(parsed)
        assertEquals(45_900.0, parsed!!.amount!!, 0.01)
        assertEquals(MovementType.EXPENSE, parsed.type)
        assertEquals("Tienda D1", parsed.merchant)
        assertEquals("Hogar", parsed.suggestedCategoryName)
    }

    @Test
    fun testMoneySentToAPersonIsAnExpenseWithTheirName() {
        val parsed = NotificationParser.parse("Nequi", "Enviaste \$50.000 a Camilo Perez")

        assertEquals(MovementType.EXPENSE, parsed!!.type)
        assertEquals("Camilo Perez", parsed.merchant)
    }

    @Test
    fun testMoneyReceivedIsIncome() {
        val parsed = NotificationParser.parse("Nequi", "Camilo te envió \$25.000")

        assertEquals(MovementType.INCOME, parsed!!.type)
        assertEquals(25_000.0, parsed.amount!!, 0.01)
    }

    @Test
    fun testCardPurchaseWithoutReadableAmountStillAlerts() {
        val parsed = NotificationParser.parse("CMR Falabella", "Realizaste una compra con tu tarjeta")

        assertNotNull(parsed)
        assertNull(parsed!!.amount)
        assertEquals(MovementType.EXPENSE, parsed.type)
    }

    @Test
    fun testAmountWithUnknownVerbIsKeptForReviewWithoutAType() {
        val parsed = NotificationParser.parse("Falabella", "Movimiento por \$12.000")

        assertEquals(12_000.0, parsed!!.amount!!, 0.01)
        assertNull(parsed.type)
    }

    @Test
    fun testSecurityCodesAreNeverCaptured() {
        assertNull(NotificationParser.parse("Nequi", "Tu código de verificación es 482913"))
        assertNull(NotificationParser.parse("Falabella", "Tu clave dinámica para la compra de \$80.000 es 1234"))
        assertNull(NotificationParser.parse("Nequi", "Tu OTP es 998877"))
    }

    @Test
    fun testFailedTransactionsAreNotMovements() {
        assertNull(NotificationParser.parse("Nequi", "Tu pago de \$20.000 fue rechazado"))
        assertNull(NotificationParser.parse("CMR", "La compra por \$20.000 no pudimos procesarla"))
    }

    @Test
    fun testPromotionsAreIgnored() {
        assertNull(NotificationParser.parse("Falabella", "¡Gana hasta \$50.000 en tu próxima visita!"))
        assertNull(NotificationParser.parse("Nequi", "Aprovecha este descuento especial"))
        assertNull(NotificationParser.parse("Nequi", "Hola, tenemos novedades para ti"))
    }

    @Test
    fun testColombianNumberFormats() {
        assertEquals(15_000.0, NotificationParser.parseColombianNumber("15.000")!!, 0.01)
        assertEquals(1_500_000.0, NotificationParser.parseColombianNumber("1.500.000")!!, 0.01)
        assertEquals(15_000.5, NotificationParser.parseColombianNumber("15.000,50")!!, 0.01)
        assertEquals(1_500_000.0, NotificationParser.parseColombianNumber("1,500,000.00")!!, 0.01)
        assertEquals(15_000.0, NotificationParser.parseColombianNumber("15000")!!, 0.01)
        assertEquals(15_000.0, NotificationParser.parseColombianNumber("15.000.")!!, 0.01)
    }

    @Test
    fun testAmountWrittenWithPesosWord() {
        assertEquals(30_000.0, NotificationParser.parseAmount("Retiro de 30.000 pesos")!!, 0.01)
    }
}
