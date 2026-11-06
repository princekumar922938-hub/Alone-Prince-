package com.example.model

enum class DateRangeOption(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_QUARTER("This Quarter"),
    LAST_QUARTER("Last Quarter"),
    THIS_YEAR("This Year"),
    LAST_YEAR("Last Year"),
    CUSTOM("Custom Date Range"),
    ALL_TIME("All Time")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("Other")
}

enum class SortOption(val displayName: String) {
    NEWEST_FIRST("Newest Date"),
    OLDEST_FIRST("Oldest Date"),
    HIGHEST_AMOUNT("Highest Amount"),
    LOWEST_AMOUNT("Lowest Amount")
}

enum class ReportPeriodType(val displayName: String) {
    DAILY("Daily Report"),
    MONTHLY("Monthly Report"),
    QUARTERLY("Quarterly Report"),
    YEARLY("Yearly Report"),
    CUSTOM("Custom Date Range"),
    CATEGORY_SPECIFIC("Category Report")
}

data class DateRange(
    val startEpochDay: Long,
    val endEpochDay: Long,
    val label: String
)

data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val colorHex: String,
    val totalPaise: Long,
    val transactionCount: Int,
    val percentage: Float, // 0..100
    val averagePaise: Long
)

data class DailySummary(
    val epochDay: Long,
    val formattedDate: String,
    val totalPaise: Long,
    val transactionCount: Int
)

data class MonthlySummary(
    val yearMonth: String, // e.g. "2026-10"
    val displayLabel: String, // e.g. "Oct 2026"
    val totalPaise: Long,
    val transactionCount: Int
)

data class DashboardStats(
    val todayPaise: Long = 0L,
    val thisWeekPaise: Long = 0L,
    val thisMonthPaise: Long = 0L,
    val thisQuarterPaise: Long = 0L,
    val thisYearPaise: Long = 0L,
    val allTimePaise: Long = 0L,
    val totalTransactionsCount: Int = 0,
    val currentRangeTotalPaise: Long = 0L,
    val currentRangeCount: Int = 0,
    val averageDailyPaise: Long = 0L,
    val activeDayAveragePaise: Long = 0L,
    val highestSpendingDay: String? = null,
    val highestSpendingCategory: String? = null,
    val prevMonthPaise: Long = 0L,
    val monthOverMonthChangePercent: Float? = null
)

data class BudgetStatus(
    val monthKey: String,
    val overallBudgetPaise: Long,
    val currentMonthSpentPaise: Long,
    val remainingPaise: Long,
    val percentageUsed: Float,
    val isNearWarning: Boolean, // >= 80%
    val isExceeded: Boolean // > 100%
)
