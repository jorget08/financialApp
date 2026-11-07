package com.financialapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Tipos de movimiento. Solo EXPENSE e INCOME cuentan como gasto o ingreso del mes:
 * un préstamo, una transferencia, un pago de tarjeta o un ahorro mueven plata
 * entre lugares, pero no son gasto.
 */
object MovementType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val TRANSFER = "TRANSFER"       // De accountId a toAccountId (incluye pago de tarjeta)
    const val ADJUSTMENT = "ADJUSTMENT"   // Cuadre de saldo; amount lleva signo
    const val LOAN_OUT = "LOAN_OUT"       // Salió plata hacia personName
    const val LOAN_IN = "LOAN_IN"         // Entró plata de personName
    const val SAVINGS = "SAVINGS"         // Plata apartada a la cuenta de ahorro

    fun isOutflow(type: String, amount: Double = 1.0): Boolean = when (type) {
        EXPENSE, LOAN_OUT, SAVINGS, TRANSFER -> true
        ADJUSTMENT -> amount < 0
        else -> false
    }
}

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["categoryId"]),
        Index(value = ["accountId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: String, // Ver MovementType
    val categoryId: String,
    val note: String? = null,
    val date: Long = System.currentTimeMillis(),
    val source: String = "MANUAL", // "MANUAL", "WIDGET", "CHAT_NLP", "AI", "NOTIFICATION"
    val createdAt: Long = System.currentTimeMillis(),
    val accountId: String? = null,
    val toAccountId: String? = null,
    val personName: String? = null
)
