package com.financialapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
        Index(value = ["categoryId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: String, // "EXPENSE" o "INCOME"
    val categoryId: String,
    val note: String? = null,
    val date: Long = System.currentTimeMillis(),
    val source: String = "MANUAL", // "MANUAL", "WIDGET", "CHAT_NLP", "AI"
    val createdAt: Long = System.currentTimeMillis()
)
