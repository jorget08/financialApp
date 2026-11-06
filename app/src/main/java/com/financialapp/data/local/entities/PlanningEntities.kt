package com.financialapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

object AccountType {
    const val CASH = "CASH"
    const val BANK = "BANK"
    const val CREDIT_CARD = "CREDIT_CARD"
}

/**
 * Lugar donde está la plata (Nequi, efectivo) o tarjeta de crédito.
 * En una tarjeta el saldo es negativo: es lo que se debe.
 */
@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val openingBalance: Double,
    val openingDate: Long = System.currentTimeMillis(),
    val captureKeyword: String? = null, // Ej: "nequi": asocia las notificaciones capturadas a esta cuenta
    val archived: Boolean = false,
    val lastReconciledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val categoryId: String,
    val monthlyLimit: Double
)

@Serializable
@Entity(
    tableName = "recurring_bills",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class RecurringBillEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val dayOfMonth: Int,
    val categoryId: String,
    val accountId: String? = null,
    val lastPaidMonth: String? = null, // "2026-11": mes calendario del último pago
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDate: Long? = null,
    val priority: Int = 0, // 0 es la meta más importante
    val createdAt: Long = System.currentTimeMillis()
)

/** Movimiento detectado en una notificación que espera confirmación. Se borra al confirmarlo o descartarlo. */
@Entity(tableName = "pending_movements", indices = [Index(value = ["dedupeKey"])])
data class PendingMovementEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sourceLabel: String,
    val rawText: String,
    val amount: Double?,
    val type: String?,
    val merchant: String?,
    val suggestedCategoryName: String,
    val postedAt: Long,
    val dedupeKey: String
)

@Entity(tableName = "ai_suggestions")
data class AiSuggestionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val question: String,
    val content: String,
    val sources: String, // Una fuente por línea: "título|url"
    val providerModel: String,
    val createdAt: Long = System.currentTimeMillis()
)
