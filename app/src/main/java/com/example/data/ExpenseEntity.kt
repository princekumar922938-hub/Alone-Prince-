package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long, // Stored as integer paise to avoid floating-point errors (e.g. ₹100.50 = 10050)
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "expense_date")
    val expenseDate: Long, // Epoch day (days since 1970-01-01) in Asia/Kolkata timezone
    @ColumnInfo(name = "expense_time")
    val expenseTime: String = "", // e.g. "14:30" or "02:30 PM"
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "Cash", // Cash, UPI, Debit Card, Credit Card, Bank Transfer, Other
    @ColumnInfo(name = "notes")
    val notes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
