package com.example.data

import com.example.model.BudgetStatus
import com.example.model.CategorySummary
import com.example.model.DailySummary
import com.example.model.DashboardStats
import com.example.model.DateRange
import com.example.model.MonthlySummary
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ExpenseRepository(private val database: AppDatabase) {

    private val expenseDao = database.expenseDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val activeCategories: Flow<List<CategoryEntity>> = categoryDao.getActiveCategories()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getExpensesInRange(startDate: Long, endDate: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesInRange(startDate, endDate)
    }

    fun getExpensesByCategory(categoryId: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByCategory(categoryId)
    }

    suspend fun getExpenseById(id: Long): ExpenseEntity? {
        return withContext(Dispatchers.IO) {
            expenseDao.getExpenseById(id)
        }
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        return withContext(Dispatchers.IO) {
            expenseDao.insertExpense(expense)
        }
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        withContext(Dispatchers.IO) {
            expenseDao.updateExpense(expense.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteExpense(id: Long) {
        withContext(Dispatchers.IO) {
            expenseDao.deleteExpenseById(id)
        }
    }

    suspend fun addCategory(name: String, iconKey: String, colorHex: String): Long {
        return withContext(Dispatchers.IO) {
            val entity = CategoryEntity(
                name = name.trim(),
                iconKey = iconKey,
                colorHex = colorHex,
                isDefault = false,
                isActive = true
            )
            categoryDao.insertCategory(entity)
        }
    }

    suspend fun renameCategory(id: Long, newName: String) {
        withContext(Dispatchers.IO) {
            val current = categoryDao.getCategoryById(id) ?: return@withContext
            categoryDao.updateCategory(current.copy(name = newName.trim()))
            expenseDao.updateCategoryNameInExpenses(id, newName.trim())
        }
    }

    suspend fun setCategoryActive(id: Long, isActive: Boolean) {
        withContext(Dispatchers.IO) {
            categoryDao.setCategoryActive(id, isActive)
        }
    }

    fun getBudgetForMonth(monthKey: String): Flow<BudgetEntity?> {
        return budgetDao.getBudgetForMonth(monthKey)
    }

    suspend fun setBudget(monthKey: String, overallPaise: Long, categoryBudgetsJson: String = "{}") {
        withContext(Dispatchers.IO) {
            budgetDao.setBudget(
                BudgetEntity(
                    monthKey = monthKey,
                    overallBudgetPaise = overallPaise,
                    categoryBudgetsJson = categoryBudgetsJson
                )
            )
        }
    }

    suspend fun getAllDataForBackup(): Triple<List<ExpenseEntity>, List<CategoryEntity>, List<BudgetEntity>> {
        return withContext(Dispatchers.IO) {
            val expenses = expenseDao.getAllExpensesSnapshot()
            val categories = categoryDao.getAllCategoriesSnapshot()
            val budgets = budgetDao.getAllBudgetsSnapshot()
            Triple(expenses, categories, budgets)
        }
    }

    suspend fun restoreBackup(
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>
    ) {
        withContext(Dispatchers.IO) {
            if (categories.isNotEmpty()) {
                categoryDao.insertCategories(categories)
            }
            if (budgets.isNotEmpty()) {
                budgetDao.insertBudgets(budgets)
            }
            if (expenses.isNotEmpty()) {
                expenseDao.insertExpenses(expenses)
            }
        }
    }

    suspend fun clearAllExpenses() {
        withContext(Dispatchers.IO) {
            expenseDao.deleteAllExpenses()
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            expenseDao.deleteAllExpenses()
            categoryDao.deleteCustomCategories()
            budgetDao.deleteAllBudgets()
        }
    }

    suspend fun generateSampleData() {
        withContext(Dispatchers.IO) {
            val cats = categoryDao.getAllCategoriesSnapshot().associateBy { it.name }
            val today = DateUtils.today()
            val samples = mutableListOf<ExpenseEntity>()

            fun getCatId(name: String): Pair<Long, String> {
                val cat = cats[name]
                return if (cat != null) cat.id to cat.name else 25L to "Other Expenses"
            }

            // Today expenses
            val (c1Id, c1Name) = getCatId("Groceries")
            samples.add(ExpenseEntity(title = "Morning Organic Milk & Bread", amountPaise = 18500, categoryId = c1Id, categoryName = c1Name, expenseDate = today.toEpochDay(), expenseTime = "08:15 AM", paymentMethod = "UPI", notes = "Fresh dairy and brown bread"))
            val (c2Id, c2Name) = getCatId("Petrol")
            samples.add(ExpenseEntity(title = "Two-Wheeler Tank Refill", amountPaise = 52000, categoryId = c2Id, categoryName = c2Name, expenseDate = today.toEpochDay(), expenseTime = "11:30 AM", paymentMethod = "UPI", notes = "HPCL Petrol Pump"))
            val (c3Id, c3Name) = getCatId("Food & Restaurants")
            samples.add(ExpenseEntity(title = "Lunch at South Indian Cafe", amountPaise = 34000, categoryId = c3Id, categoryName = c3Name, expenseDate = today.toEpochDay(), expenseTime = "01:45 PM", paymentMethod = "Cash", notes = "Masala Dosa & Filter Coffee"))

            // Yesterday expenses
            val yDay = today.minusDays(1)
            val (c4Id, c4Name) = getCatId("Fruits & Vegetables")
            samples.add(ExpenseEntity(title = "Weekly Sabzi Mandi Shopping", amountPaise = 46000, categoryId = c4Id, categoryName = c4Name, expenseDate = yDay.toEpochDay(), expenseTime = "06:20 PM", paymentMethod = "UPI", notes = "Vegetables & fresh fruits"))
            val (c5Id, c5Name) = getCatId("Mobile Recharge & Phone Bill")
            samples.add(ExpenseEntity(title = "Jio Monthly 5G Plan", amountPaise = 34900, categoryId = c5Id, categoryName = c5Name, expenseDate = yDay.toEpochDay(), expenseTime = "09:10 AM", paymentMethod = "Debit Card", notes = "2GB/day unlimited pack"))

            // Last 7 days expenses
            for (i in 2..6) {
                val day = today.minusDays(i.toLong())
                when (i) {
                    2 -> {
                        val (id, name) = getCatId("Electricity Bill")
                        samples.add(ExpenseEntity(title = "State Discom Monthly Power Bill", amountPaise = 185000, categoryId = id, categoryName = name, expenseDate = day.toEpochDay(), expenseTime = "04:00 PM", paymentMethod = "UPI", notes = "Online bill payment"))
                    }
                    3 -> {
                        val (id, name) = getCatId("Shopping")
                        samples.add(ExpenseEntity(title = "Casual Cotton Shirt", amountPaise = 129900, categoryId = id, categoryName = name, expenseDate = day.toEpochDay(), expenseTime = "07:30 PM", paymentMethod = "Credit Card", notes = "Festival sale purchase"))
                    }
                    4 -> {
                        val (id, name) = getCatId("Medical & Healthcare")
                        samples.add(ExpenseEntity(title = "Doctor Consultation & Medicines", amountPaise = 75000, categoryId = id, categoryName = name, expenseDate = day.toEpochDay(), expenseTime = "12:15 PM", paymentMethod = "Cash", notes = "Apollo Pharmacy"))
                    }
                    5 -> {
                        val (id, name) = getCatId("Internet & Wi-Fi")
                        samples.add(ExpenseEntity(title = "Airtel Xstream Fiber Bill", amountPaise = 94300, categoryId = id, categoryName = name, expenseDate = day.toEpochDay(), expenseTime = "10:00 AM", paymentMethod = "Bank Transfer", notes = "100 Mbps broadband"))
                    }
                    6 -> {
                        val (id, name) = getCatId("LPG Gas")
                        samples.add(ExpenseEntity(title = "Indane Gas Cylinder Refill", amountPaise = 91000, categoryId = id, categoryName = name, expenseDate = day.toEpochDay(), expenseTime = "02:10 PM", paymentMethod = "UPI", notes = "Doorstep cylinder delivery"))
                    }
                }
            }

            // Earlier this month
            val day10 = today.withDayOfMonth(minOf(10, today.dayOfMonth))
            val (c6Id, c6Name) = getCatId("Rent & Housing")
            samples.add(ExpenseEntity(title = "Apartment House Rent", amountPaise = 1500000, categoryId = c6Id, categoryName = c6Name, expenseDate = day10.toEpochDay(), expenseTime = "09:00 AM", paymentMethod = "Bank Transfer", notes = "Transfer to landlord"))

            val (c7Id, c7Name) = getCatId("EMI & Loan Payments")
            samples.add(ExpenseEntity(title = "Car Auto Loan EMI", amountPaise = 850000, categoryId = c7Id, categoryName = c7Name, expenseDate = day10.toEpochDay(), expenseTime = "10:30 AM", paymentMethod = "Bank Transfer", notes = "HDFC Auto debit"))

            // Previous month expenses
            val prevMonth = today.minusMonths(1)
            for (p in listOf(5, 12, 18, 25)) {
                val pDate = prevMonth.withDayOfMonth(p)
                when (p) {
                    5 -> {
                        val (id, name) = getCatId("Rent & Housing")
                        samples.add(ExpenseEntity(title = "Apartment House Rent (Prev)", amountPaise = 1500000, categoryId = id, categoryName = name, expenseDate = pDate.toEpochDay(), expenseTime = "09:00 AM", paymentMethod = "Bank Transfer", notes = "Previous month rent"))
                    }
                    12 -> {
                        val (id, name) = getCatId("Groceries")
                        samples.add(ExpenseEntity(title = "Supermarket Monthly Ration", amountPaise = 640000, categoryId = id, categoryName = name, expenseDate = pDate.toEpochDay(), expenseTime = "05:45 PM", paymentMethod = "Credit Card", notes = "Bulk pantry staples"))
                    }
                    18 -> {
                        val (id, name) = getCatId("Travel")
                        samples.add(ExpenseEntity(title = "Train Tickets & Cab", amountPaise = 280000, categoryId = id, categoryName = name, expenseDate = pDate.toEpochDay(), expenseTime = "03:15 PM", paymentMethod = "UPI", notes = "IRCTC and Ola cab"))
                    }
                    25 -> {
                        val (id, name) = getCatId("Entertainment")
                        samples.add(ExpenseEntity(title = "Weekend Movie & Snacks", amountPaise = 110000, categoryId = id, categoryName = name, expenseDate = pDate.toEpochDay(), expenseTime = "08:00 PM", paymentMethod = "Credit Card", notes = "Cinema IMAX multiplex"))
                    }
                }
            }

            expenseDao.insertExpenses(samples)

            // Also set a default demo monthly budget: ₹40,000 (4,000,000 paise)
            val currentMonth = DateUtils.currentMonthKey()
            budgetDao.setBudget(
                BudgetEntity(
                    monthKey = currentMonth,
                    overallBudgetPaise = 4000000L
                )
            )
        }
    }

    /**
     * Calculates complete financial metrics strictly according to the mathematical rules in the prompt:
     * - Total Expenditure = Sum of qualifying expense amounts
     * - Category Total = Sum of qualifying expense amounts belonging to that category
     * - Daily Total = Sum of all expenses recorded on that date
     * - Monthly Total = Sum of all expenses within the calendar month
     * - Quarterly Total = Sum of all expenses within the calendar quarter
     * - Yearly Total = Sum of all expenses within the calendar year
     * - Category Percentage = Category Total / Total Expenditure * 100
     * - Average Daily Expenditure = Total Expenditure / Number of calendar days in selected period
     * - Active Day Average = Total Expenditure / Number of active spending days
     */
    fun computeStats(
        allExpenses: List<ExpenseEntity>,
        selectedRange: DateRange,
        allCategories: List<CategoryEntity>
    ): Pair<DashboardStats, List<CategorySummary>> {
        val today = DateUtils.today()
        val todayEpoch = today.toEpochDay()

        // Week: current Monday to Sunday
        val mondayEpoch = today.minusDays((today.dayOfWeek.value - 1).toLong()).toEpochDay()
        val sundayEpoch = mondayEpoch + 6

        // Current Month
        val monthStartEpoch = today.withDayOfMonth(1).toEpochDay()
        val monthEndEpoch = today.plusMonths(1).withDayOfMonth(1).minusDays(1).toEpochDay()

        // Previous Month
        val prevMonth = today.minusMonths(1)
        val prevMonthStart = prevMonth.withDayOfMonth(1).toEpochDay()
        val prevMonthEnd = prevMonth.plusMonths(1).withDayOfMonth(1).minusDays(1).toEpochDay()

        // Current Quarter
        val quarter = (today.monthValue - 1) / 3 + 1
        val quarterStartMonth = (quarter - 1) * 3 + 1
        val quarterStartEpoch = LocalDate.of(today.year, quarterStartMonth, 1).toEpochDay()
        val quarterEndEpoch = LocalDate.of(today.year, quarterStartMonth + 2, 1)
            .plusMonths(1).withDayOfMonth(1).minusDays(1).toEpochDay()

        // Current Year
        val yearStartEpoch = LocalDate.of(today.year, 1, 1).toEpochDay()
        val yearEndEpoch = LocalDate.of(today.year, 12, 31).toEpochDay()

        var todayPaise = 0L
        var thisWeekPaise = 0L
        var thisMonthPaise = 0L
        var prevMonthPaise = 0L
        var thisQuarterPaise = 0L
        var thisYearPaise = 0L
        var allTimePaise = 0L

        // For filtered range
        val rangeExpenses = mutableListOf<ExpenseEntity>()

        for (e in allExpenses) {
            val d = e.expenseDate
            val a = e.amountPaise
            allTimePaise += a

            if (d == todayEpoch) todayPaise += a
            if (d in mondayEpoch..sundayEpoch) thisWeekPaise += a
            if (d in monthStartEpoch..monthEndEpoch) thisMonthPaise += a
            if (d in prevMonthStart..prevMonthEnd) prevMonthPaise += a
            if (d in quarterStartEpoch..quarterEndEpoch) thisQuarterPaise += a
            if (d in yearStartEpoch..yearEndEpoch) thisYearPaise += a

            if (d in selectedRange.startEpochDay..selectedRange.endEpochDay) {
                rangeExpenses.add(e)
            }
        }

        val rangeTotalPaise = rangeExpenses.sumOf { it.amountPaise }
        val rangeCount = rangeExpenses.size

        // Calendar days in period
        val calendarDays = maxOf(1L, (selectedRange.endEpochDay - selectedRange.startEpochDay + 1))
        val avgDailyPaise = if (calendarDays > 0) rangeTotalPaise / calendarDays else 0L

        // Active days
        val activeDaysMap = rangeExpenses.groupBy { it.expenseDate }
        val activeDaysCount = maxOf(1, activeDaysMap.keys.size)
        val activeDayAvgPaise = if (rangeExpenses.isNotEmpty()) rangeTotalPaise / activeDaysCount else 0L

        // Highest spending day
        val highestDayEntry = activeDaysMap.maxByOrNull { entry -> entry.value.sumOf { it.amountPaise } }
        val highestSpendingDay = highestDayEntry?.let {
            val dateStr = DateUtils.formatDate(it.key)
            val sumStr = com.example.util.CurrencyUtils.formatInr(it.value.sumOf { e -> e.amountPaise })
            "$dateStr ($sumStr)"
        }

        // Category breakdown
        val catMap = allCategories.associateBy { it.id }
        val groupedByCat = rangeExpenses.groupBy { it.categoryId }

        val categorySummaries = groupedByCat.map { (catId, exps) ->
            val catTotal = exps.sumOf { it.amountPaise }
            val catObj = catMap[catId]
            val catName = catObj?.name ?: exps.firstOrNull()?.categoryName ?: "Other"
            val iconKey = catObj?.iconKey ?: "other"
            val colorHex = catObj?.colorHex ?: "#3B82F6"
            val pct = if (rangeTotalPaise > 0) (catTotal.toDouble() / rangeTotalPaise.toDouble() * 100.0).toFloat() else 0f
            val avg = if (exps.isNotEmpty()) catTotal / exps.size else 0L

            CategorySummary(
                categoryId = catId,
                categoryName = catName,
                iconKey = iconKey,
                colorHex = colorHex,
                totalPaise = catTotal,
                transactionCount = exps.size,
                percentage = pct,
                averagePaise = avg
            )
        }.sortedByDescending { it.totalPaise }

        val highestCategory = categorySummaries.firstOrNull()?.let {
            "${it.categoryName} (${com.example.util.CurrencyUtils.formatInr(it.totalPaise)})"
        }

        // Month over month comparison percent
        val momChange: Float? = if (prevMonthPaise > 0) {
            ((thisMonthPaise - prevMonthPaise).toFloat() / prevMonthPaise.toFloat()) * 100f
        } else null

        val stats = DashboardStats(
            todayPaise = todayPaise,
            thisWeekPaise = thisWeekPaise,
            thisMonthPaise = thisMonthPaise,
            thisQuarterPaise = thisQuarterPaise,
            thisYearPaise = thisYearPaise,
            allTimePaise = allTimePaise,
            totalTransactionsCount = allExpenses.size,
            currentRangeTotalPaise = rangeTotalPaise,
            currentRangeCount = rangeCount,
            averageDailyPaise = avgDailyPaise,
            activeDayAveragePaise = activeDayAvgPaise,
            highestSpendingDay = highestSpendingDay,
            highestSpendingCategory = highestCategory,
            prevMonthPaise = prevMonthPaise,
            monthOverMonthChangePercent = momChange
        )

        return stats to categorySummaries
    }
}
