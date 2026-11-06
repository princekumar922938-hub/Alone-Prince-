package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month_key = :monthKey")
    fun getBudgetForMonth(monthKey: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month_key = :monthKey")
    suspend fun getBudgetSnapshot(monthKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSnapshot(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
