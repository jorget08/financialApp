package com.financialapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: String, // "EXPENSE", "INCOME" o "SYSTEM" (interna, nunca se muestra para elegir)
    val displayOrder: Int = 0
)

object SystemCategory {
    const val TRANSFER = "cat_sys_transfer"
    const val ADJUSTMENT = "cat_sys_adjustment"
    const val LOAN = "cat_sys_loan"
    const val SAVINGS = "cat_sys_savings"
    const val OTHER_EXPENSE = "cat_otros_gastos"
    const val OTHER_INCOME = "cat_otros_ingresos"
}
