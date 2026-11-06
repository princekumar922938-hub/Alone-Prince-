package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_kharcha.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(1, "Groceries", "groceries", "#10B981", isDefault = true),
            CategoryEntity(2, "Milk & Dairy", "milk", "#38BDF8", isDefault = true),
            CategoryEntity(3, "Fruits & Vegetables", "fruits", "#84CC16", isDefault = true),
            CategoryEntity(4, "Petrol", "petrol", "#F97316", isDefault = true),
            CategoryEntity(5, "Diesel", "diesel", "#EA580C", isDefault = true),
            CategoryEntity(6, "Electricity Bill", "electricity", "#EAB308", isDefault = true),
            CategoryEntity(7, "Water Bill", "water", "#06B6D4", isDefault = true),
            CategoryEntity(8, "Mobile Recharge & Phone Bill", "phone", "#6366F1", isDefault = true),
            CategoryEntity(9, "Internet & Wi-Fi", "wifi", "#8B5CF6", isDefault = true),
            CategoryEntity(10, "LPG Gas", "gas", "#F43F5E", isDefault = true),
            CategoryEntity(11, "Rent & Housing", "housing", "#3B82F6", isDefault = true),
            CategoryEntity(12, "Medical & Healthcare", "medical", "#EF4444", isDefault = true),
            CategoryEntity(13, "Education", "education", "#A855F7", isDefault = true),
            CategoryEntity(14, "Transportation", "transport", "#14B8A6", isDefault = true),
            CategoryEntity(15, "Shopping", "shopping", "#EC4899", isDefault = true),
            CategoryEntity(16, "Food & Restaurants", "restaurant", "#F59E0B", isDefault = true),
            CategoryEntity(17, "Entertainment", "entertainment", "#8B5CF6", isDefault = true),
            CategoryEntity(18, "Travel", "travel", "#0EA5E9", isDefault = true),
            CategoryEntity(19, "Personal Care", "personal_care", "#D946EF", isDefault = true),
            CategoryEntity(20, "Household Expenses", "household", "#64748B", isDefault = true),
            CategoryEntity(21, "Maintenance & Repairs", "repairs", "#78716C", isDefault = true),
            CategoryEntity(22, "EMI & Loan Payments", "loan", "#DC2626", isDefault = true),
            CategoryEntity(23, "Insurance", "insurance", "#059669", isDefault = true),
            CategoryEntity(24, "Bank Charges", "bank", "#475569", isDefault = true),
            CategoryEntity(25, "Other Expenses", "other", "#6B7280", isDefault = true)
        )

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }
        }

        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(DEFAULT_CATEGORIES)
            }
        }
    }
}
