package com.financialapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "savings_accounts")
data class SavingsAccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bankName: String,
    val teaRate: Double, // Tasa Efectiva Anual (ej: 7.50 para 7.5%)
    val currency: String = "USD",
    val initialBalance: Double = 0.0,
    val plannedMonthlyDeposit: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "savings_deposits",
    foreignKeys = [
        ForeignKey(
            entity = SavingsAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["depositDate"])
    ]
)
data class SavingsDepositEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val amount: Double,
    val depositDate: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = false,
    val note: String? = null
)
