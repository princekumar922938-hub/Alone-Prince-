package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CategoryEntity
import com.example.data.ExpenseEntity
import com.example.data.ExpenseRepository
import com.example.model.BudgetStatus
import com.example.model.CategorySummary
import com.example.model.DashboardStats
import com.example.model.DateRange
import com.example.model.DateRangeOption
import com.example.model.PaymentMethod
import com.example.model.ReportPeriodType
import com.example.model.SortOption
import com.example.util.DateUtils
import com.example.util.ExportUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = ExpenseRepository(database)

    // Events (e.g. snackbar messages)
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    // Base flows
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCategories: StateFlow<List<CategoryEntity>> = repository.activeCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -------------------------------------------------------------------------
    // Dashboard State
    // -------------------------------------------------------------------------
    private val _dashboardDateRangeOption = MutableStateFlow(DateRangeOption.THIS_MONTH)
    val dashboardDateRangeOption = _dashboardDateRangeOption.asStateFlow()

    private val _dashCustomStart = MutableStateFlow<Long?>(null)
    private val _dashCustomEnd = MutableStateFlow<Long?>(null)

    val dashboardRange: StateFlow<DateRange> = combine(
        _dashboardDateRangeOption,
        _dashCustomStart,
        _dashCustomEnd
    ) { opt, s, e ->
        DateUtils.getDateRange(opt, s, e)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DateUtils.getDateRange(DateRangeOption.THIS_MONTH)
    )

    // Stats computed dynamically from saved expenses
    val dashboardData: StateFlow<Pair<DashboardStats, List<CategorySummary>>> = combine(
        allExpenses,
        dashboardRange,
        allCategories
    ) { expenses, range, cats ->
        repository.computeStats(expenses, range, cats)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardStats() to emptyList()
    )

    // -------------------------------------------------------------------------
    // Budget State
    // -------------------------------------------------------------------------
    private val currentMonthKey = DateUtils.currentMonthKey()
    val monthlyBudget = repository.getBudgetForMonth(currentMonthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val budgetStatus: StateFlow<BudgetStatus> = combine(
        allExpenses,
        monthlyBudget
    ) { expenses, budget ->
        val monthRange = DateUtils.getDateRange(DateRangeOption.THIS_MONTH)
        val spentPaise = expenses
            .filter { it.expenseDate in monthRange.startEpochDay..monthRange.endEpochDay }
            .sumOf { it.amountPaise }
        val overallBudgetPaise = budget?.overallBudgetPaise ?: 0L
        val remaining = overallBudgetPaise - spentPaise
        val pct = if (overallBudgetPaise > 0) (spentPaise.toDouble() / overallBudgetPaise.toDouble() * 100.0).toFloat() else 0f
        BudgetStatus(
            monthKey = currentMonthKey,
            overallBudgetPaise = overallBudgetPaise,
            currentMonthSpentPaise = spentPaise,
            remainingPaise = remaining,
            percentageUsed = pct,
            isNearWarning = pct >= 80f && pct <= 100f,
            isExceeded = pct > 100f
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetStatus(currentMonthKey, 0L, 0L, 0L, 0f, false, false)
    )

    // -------------------------------------------------------------------------
    // Transactions Screen State & Filters
    // -------------------------------------------------------------------------
    val txSearchQuery = MutableStateFlow("")
    val txFilterCategoryId = MutableStateFlow<Long?>(null) // null = all
    val txFilterPaymentMethod = MutableStateFlow<String?>(null) // null = all
    val txSortOption = MutableStateFlow(SortOption.NEWEST_FIRST)
    val txDateRangeOption = MutableStateFlow(DateRangeOption.ALL_TIME)
    val txCustomStart = MutableStateFlow<Long?>(null)
    val txCustomEnd = MutableStateFlow<Long?>(null)

    val txDateRange: StateFlow<DateRange> = combine(
        txDateRangeOption,
        txCustomStart,
        txCustomEnd
    ) { opt, s, e ->
        DateUtils.getDateRange(opt, s, e)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DateUtils.getDateRange(DateRangeOption.ALL_TIME)
    )

    private data class TxFilters(
        val query: String,
        val categoryId: Long?,
        val paymentMethod: String?,
        val sort: SortOption,
        val range: DateRange
    )

    private val txFilters = combine(
        txSearchQuery,
        txFilterCategoryId,
        txFilterPaymentMethod,
        txSortOption,
        txDateRange
    ) { query, catId, payment, sort, range ->
        TxFilters(query, catId, payment, sort, range)
    }

    val filteredTransactions: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        txFilters
    ) { expenses, filters ->
        var list = expenses.filter { it.expenseDate in filters.range.startEpochDay..filters.range.endEpochDay }

        if (filters.categoryId != null) {
            list = list.filter { it.categoryId == filters.categoryId }
        }
        if (filters.paymentMethod != null) {
            list = list.filter { it.paymentMethod.equals(filters.paymentMethod, ignoreCase = true) }
        }
        if (filters.query.isNotBlank()) {
            val q = filters.query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q) ||
                        it.categoryName.lowercase().contains(q)
            }
        }

        when (filters.sort) {
            SortOption.NEWEST_FIRST -> list.sortedWith(compareByDescending<ExpenseEntity> { it.expenseDate }.thenByDescending { it.createdAt })
            SortOption.OLDEST_FIRST -> list.sortedWith(compareBy<ExpenseEntity> { it.expenseDate }.thenBy { it.createdAt })
            SortOption.HIGHEST_AMOUNT -> list.sortedByDescending { it.amountPaise }
            SortOption.LOWEST_AMOUNT -> list.sortedBy { it.amountPaise }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // -------------------------------------------------------------------------
    // Reports State
    // -------------------------------------------------------------------------
    val reportPeriodType = MutableStateFlow(ReportPeriodType.MONTHLY)
    val reportSelectedYear = MutableStateFlow(DateUtils.today().year)
    val reportSelectedMonth = MutableStateFlow(DateUtils.today().monthValue)
    val reportSelectedQuarter = MutableStateFlow((DateUtils.today().monthValue - 1) / 3 + 1)
    val reportSelectedDate = MutableStateFlow(DateUtils.todayEpochDay())
    val reportCustomStart = MutableStateFlow(DateUtils.today().minusDays(30).toEpochDay())
    val reportCustomEnd = MutableStateFlow(DateUtils.todayEpochDay())
    val reportSelectedCategoryId = MutableStateFlow<Long?>(null) // null = all

    private data class ReportDateParams(
        val year: Int,
        val month: Int,
        val quarter: Int,
        val day: Long,
        val customStart: Long,
        val customEnd: Long
    )

    private val reportCustomPair = combine(reportCustomStart, reportCustomEnd) { s, e -> s to e }

    private val reportDateParams = combine(
        reportSelectedYear,
        reportSelectedMonth,
        reportSelectedQuarter,
        reportSelectedDate,
        reportCustomPair
    ) { yr, mo, qtr, day, customPair ->
        ReportDateParams(yr, mo, qtr, day, customPair.first, customPair.second)
    }

    val reportRange: StateFlow<DateRange> = combine(
        reportPeriodType,
        reportDateParams
    ) { type, params ->
        when (type) {
            ReportPeriodType.DAILY -> DateRange(params.day, params.day, "Daily (${DateUtils.formatDate(params.day)})")
            ReportPeriodType.MONTHLY -> DateUtils.getMonthRange(params.year, params.month)
            ReportPeriodType.QUARTERLY -> DateUtils.getQuarterRange(params.year, params.quarter)
            ReportPeriodType.YEARLY -> DateUtils.getYearRange(params.year)
            ReportPeriodType.CUSTOM -> DateRange(
                minOf(params.customStart, params.customEnd),
                maxOf(params.customStart, params.customEnd),
                "${DateUtils.formatDate(minOf(params.customStart, params.customEnd))} - ${DateUtils.formatDate(maxOf(params.customStart, params.customEnd))}"
            )
            ReportPeriodType.CATEGORY_SPECIFIC -> DateUtils.getMonthRange(params.year, params.month)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DateUtils.getMonthRange(DateUtils.today().year, DateUtils.today().monthValue)
    )

    val reportExpenses: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        reportRange,
        reportSelectedCategoryId
    ) { expenses, range, catId ->
        var list = expenses.filter { it.expenseDate in range.startEpochDay..range.endEpochDay }
        if (catId != null) {
            list = list.filter { it.categoryId == catId }
        }
        list
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Editing Expense state
    val expenseBeingEdited = MutableStateFlow<ExpenseEntity?>(null)

    // Initial check for categories
    init {
        viewModelScope.launch {
            AppDatabase.populateDefaultCategories(database.categoryDao())
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    fun setDashboardDateRange(option: DateRangeOption) {
        _dashboardDateRangeOption.value = option
    }

    fun setDashboardCustomRange(start: Long, end: Long) {
        _dashCustomStart.value = start
        _dashCustomEnd.value = end
        _dashboardDateRangeOption.value = DateRangeOption.CUSTOM
    }

    fun setTxCustomRange(start: Long, end: Long) {
        txCustomStart.value = start
        txCustomEnd.value = end
        txDateRangeOption.value = DateRangeOption.CUSTOM
    }

    fun saveExpense(
        id: Long = 0,
        title: String,
        amountPaise: Long,
        categoryId: Long,
        categoryName: String,
        date: Long,
        time: String,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertExpense(
                    ExpenseEntity(
                        title = title.trim(),
                        amountPaise = amountPaise,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        expenseDate = date,
                        expenseTime = time,
                        paymentMethod = paymentMethod,
                        notes = notes.trim()
                    )
                )
                _userMessage.emit("Expense added successfully")
            } else {
                repository.updateExpense(
                    ExpenseEntity(
                        id = id,
                        title = title.trim(),
                        amountPaise = amountPaise,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        expenseDate = date,
                        expenseTime = time,
                        paymentMethod = paymentMethod,
                        notes = notes.trim()
                    )
                )
                expenseBeingEdited.value = null
                _userMessage.emit("Expense updated successfully")
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _userMessage.emit("Expense deleted")
        }
    }

    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            repository.addCategory(name, iconKey, colorHex)
            _userMessage.emit("Category '$name' created")
        }
    }

    fun renameCategory(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameCategory(id, newName)
            _userMessage.emit("Category renamed to '$newName'")
        }
    }

    fun setCategoryActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.setCategoryActive(id, isActive)
            _userMessage.emit(if (isActive) "Category activated" else "Category hidden")
        }
    }

    fun setMonthlyBudget(amountPaise: Long) {
        viewModelScope.launch {
            repository.setBudget(DateUtils.currentMonthKey(), amountPaise)
            _userMessage.emit("Monthly budget updated")
        }
    }

    fun generateSampleData() {
        viewModelScope.launch {
            repository.generateSampleData()
            _userMessage.emit("Realistic sample financial records generated")
        }
    }

    fun clearAllExpenses() {
        viewModelScope.launch {
            repository.clearAllExpenses()
            _userMessage.emit("All expense transactions cleared")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            AppDatabase.populateDefaultCategories(database.categoryDao())
            _userMessage.emit("Application reset to factory state")
        }
    }

    fun restoreFromJson(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val parsed = ExportUtils.parseJsonBackup(json)
            if (parsed != null) {
                val (exps, cats, bgts) = parsed
                repository.restoreBackup(exps, cats, bgts)
                _userMessage.emit("Backup restored: ${exps.size} transactions, ${cats.size} categories")
                onComplete(true)
            } else {
                _userMessage.emit("Invalid backup JSON file")
                onComplete(false)
            }
        }
    }
}
