package com.financialapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: String, // "EXPENSE" o "INCOME"
    val displayOrder: Int = 0
)
