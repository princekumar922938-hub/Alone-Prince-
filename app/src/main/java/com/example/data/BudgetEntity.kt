package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "month_key")
    val monthKey: String, // e.g. "2026-10"
    @ColumnInfo(name = "overall_budget_paise")
    val overallBudgetPaise: Long = 0L, // In paise
    @ColumnInfo(name = "category_budgets_json")
    val categoryBudgetsJson: String = "{}" // Category ID to Budget Paise map serialized
)
