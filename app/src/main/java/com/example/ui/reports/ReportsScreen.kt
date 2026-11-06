package com.example.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CategorySummary
import com.example.model.ReportPeriodType
import com.example.ui.ExpenseViewModel
import com.example.ui.components.BarChart
import com.example.ui.components.DonutChart
import com.example.ui.components.ExpenseRowItem
import com.example.ui.components.TrendAreaChart
import com.example.util.CategoryIcons
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.ExportUtils
import androidx.activity.compose.BackHandler
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }
    val context = LocalContext.current
    val currentPeriodType by viewModel.reportPeriodType.collectAsStateWithLifecycle()
    val reportExpenses by viewModel.reportExpenses.collectAsStateWithLifecycle()
    val activeRange by viewModel.reportRange.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.reportSelectedCategoryId.collectAsStateWithLifecycle()

    val selectedYear by viewModel.reportSelectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.reportSelectedMonth.collectAsStateWithLifecycle()
    val selectedQuarter by viewModel.reportSelectedQuarter.collectAsStateWithLifecycle()
    val selectedDate by viewModel.reportSelectedDate.collectAsStateWithLifecycle()

    var showYearMenu by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showQuarterMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDailyDatePicker by remember { mutableStateOf(false) }

    val catMap = remember(allCategories) { allCategories.associateBy { it.id } }

    // Aggregate statistics for the report period
    val totalExpenditurePaise = remember(reportExpenses) { reportExpenses.sumOf { it.amountPaise } }
    val totalTransactions = reportExpenses.size

    val calendarDays = remember(activeRange) {
        maxOf(1L, activeRange.endEpochDay - activeRange.startEpochDay + 1)
    }
    val avgDailyPaise = if (calendarDays > 0) totalExpenditurePaise / calendarDays else 0L

    val activeDaysMap = remember(reportExpenses) { reportExpenses.groupBy { it.expenseDate } }
    val activeDaysCount = maxOf(1, activeDaysMap.keys.size)
    val activeDayAvgPaise = if (reportExpenses.isNotEmpty()) totalExpenditurePaise / activeDaysCount else 0L

    val highestDayEntry = remember(activeDaysMap) {
        activeDaysMap.maxByOrNull { entry -> entry.value.sumOf { it.amountPaise } }
    }
    val highestDayString = highestDayEntry?.let {
        "${DateUtils.formatDate(it.key)} (${CurrencyUtils.formatInr(it.value.sumOf { e -> e.amountPaise })})"
    }

    // Category breakdown
    val categorySummaries = remember(reportExpenses, allCategories) {
        val grouped = reportExpenses.groupBy { it.categoryId }
        grouped.map { (catId, exps) ->
            val sum = exps.sumOf { it.amountPaise }
            val catObj = catMap[catId]
            val pct = if (totalExpenditurePaise > 0) (sum.toDouble() / totalExpenditurePaise.toDouble() * 100.0).toFloat() else 0f
            CategorySummary(
                categoryId = catId,
                categoryName = catObj?.name ?: exps.firstOrNull()?.categoryName ?: "Other",
                iconKey = catObj?.iconKey ?: "other",
                colorHex = catObj?.colorHex ?: "#2563EB",
                totalPaise = sum,
                transactionCount = exps.size,
                percentage = pct,
                averagePaise = if (exps.isNotEmpty()) sum / exps.size else 0L
            )
        }.sortedByDescending { it.totalPaise }
    }

    val highestCategoryString = categorySummaries.firstOrNull()?.let {
        "${it.categoryName} (${CurrencyUtils.formatInr(it.totalPaise)})"
    }

    // Daily spending points for trend/bar charts
    val chartPoints = remember(reportExpenses, activeRange) {
        if (reportExpenses.isEmpty()) emptyList()
        else {
            val sortedDays = activeDaysMap.toSortedMap()
            sortedDays.map { (epochDay, exps) ->
                val date = LocalDate.ofEpochDay(epochDay)
                val label = "${date.dayOfMonth}/${date.monthValue}"
                label to exps.sumOf { it.amountPaise }
            }
        }
    }

    val dailyDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate * 86400000L
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Export Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Financial Reports",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Detailed analytics, breakdowns & exports",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    // Export CSV
                    IconButton(
                        onClick = {
                            val csv = ExportUtils.generateCategorySummaryCsv(categorySummaries, totalExpenditurePaise)
                            ExportUtils.shareExportFile(
                                context,
                                "SmartKharcha_${currentPeriodType.name}_CategorySummary.csv",
                                "text/csv",
                                csv
                            )
                        },
                        modifier = Modifier.testTag("report_export_csv_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Share Detailed Text / PDF report
                    IconButton(
                        onClick = {
                            val textReport = ExportUtils.generateDetailedReportText(
                                activeRange.label,
                                totalExpenditurePaise,
                                totalTransactions,
                                avgDailyPaise,
                                activeDayAvgPaise,
                                highestDayString,
                                highestCategoryString,
                                categorySummaries
                            )
                            ExportUtils.shareExportFile(
                                context,
                                "SmartKharcha_Detailed_Report.txt",
                                "text/plain",
                                textReport
                            )
                        },
                        modifier = Modifier.testTag("report_share_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Report", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Report Type Tabs / Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriodType.values().forEach { periodType ->
                    val isSelected = currentPeriodType == periodType
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.reportPeriodType.value = periodType },
                        label = { Text(periodType.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("report_type_${periodType.name}")
                    )
                }
            }
        }

        // Period Parameter Selectors (e.g. Month, Quarter, Year, Category)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Period: ${activeRange.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (currentPeriodType) {
                            ReportPeriodType.DAILY -> {
                                OutlinedButton(
                                    onClick = { showDailyDatePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("select_daily_date_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Date: ${DateUtils.formatDate(selectedDate)}")
                                }
                            }

                            ReportPeriodType.MONTHLY -> {
                                // Month Dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { showMonthMenu = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Month: ${java.time.Month.of(selectedMonth).name.take(3)}")
                                    }
                                    DropdownMenu(
                                        expanded = showMonthMenu,
                                        onDismissRequest = { showMonthMenu = false }
                                    ) {
                                        for (m in 1..12) {
                                            DropdownMenuItem(
                                                text = { Text(java.time.Month.of(m).name) },
                                                onClick = {
                                                    viewModel.reportSelectedMonth.value = m
                                                    showMonthMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Year Dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { showYearMenu = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Year: $selectedYear")
                                    }
                                    DropdownMenu(
                                        expanded = showYearMenu,
                                        onDismissRequest = { showYearMenu = false }
                                    ) {
                                        val curY = DateUtils.today().year
                                        for (y in (curY - 3)..(curY + 1)) {
                                            DropdownMenuItem(
                                                text = { Text(y.toString()) },
                                                onClick = {
                                                    viewModel.reportSelectedYear.value = y
                                                    showYearMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            ReportPeriodType.QUARTERLY -> {
                                // Quarter Dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { showQuarterMenu = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Quarter: Q$selectedQuarter")
                                    }
                                    DropdownMenu(
                                        expanded = showQuarterMenu,
                                        onDismissRequest = { showQuarterMenu = false }
                                    ) {
                                        listOf(
                                            1 to "Q1 (Jan - Mar)",
                                            2 to "Q2 (Apr - Jun)",
                                            3 to "Q3 (Jul - Sep)",
                                            4 to "Q4 (Oct - Dec)"
                                        ).forEach { (q, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    viewModel.reportSelectedQuarter.value = q
                                                    showQuarterMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Year Dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { showYearMenu = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Year: $selectedYear")
                                    }
                                    DropdownMenu(
                                        expanded = showYearMenu,
                                        onDismissRequest = { showYearMenu = false }
                                    ) {
                                        val curY = DateUtils.today().year
                                        for (y in (curY - 3)..(curY + 1)) {
                                            DropdownMenuItem(
                                                text = { Text(y.toString()) },
                                                onClick = {
                                                    viewModel.reportSelectedYear.value = y
                                                    showYearMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            ReportPeriodType.YEARLY -> {
                                Box {
                                    OutlinedButton(
                                        onClick = { showYearMenu = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Year: $selectedYear")
                                    }
                                    DropdownMenu(
                                        expanded = showYearMenu,
                                        onDismissRequest = { showYearMenu = false }
                                    ) {
                                        val curY = DateUtils.today().year
                                        for (y in (curY - 3)..(curY + 1)) {
                                            DropdownMenuItem(
                                                text = { Text(y.toString()) },
                                                onClick = {
                                                    viewModel.reportSelectedYear.value = y
                                                    showYearMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            ReportPeriodType.CUSTOM, ReportPeriodType.CATEGORY_SPECIFIC -> {
                                // Category filter option
                                Box {
                                    OutlinedButton(
                                        onClick = { showCategoryMenu = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (selectedCatId != null) (catMap[selectedCatId]?.name ?: "Category") else "All Categories",
                                            maxLines = 1
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showCategoryMenu,
                                        onDismissRequest = { showCategoryMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Categories") },
                                            onClick = {
                                                viewModel.reportSelectedCategoryId.value = null
                                                showCategoryMenu = false
                                            }
                                        )
                                        allCategories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.name) },
                                                onClick = {
                                                    viewModel.reportSelectedCategoryId.value = cat.id
                                                    showCategoryMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary KPI Metrics Row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("report_summary_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Expenditure",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatInr(totalExpenditurePaise),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalTransactions.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Daily Average", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.formatInr(avgDailyPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Active Day Avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.formatInr(activeDayAvgPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (highestDayString != null || highestCategoryString != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                highestDayString?.let {
                                    Text(
                                        text = "Highest Day: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                highestCategoryString?.let {
                                    Text(
                                        text = "Top Category: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Donut Chart: Category Spending Distribution
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DonutChart(
                        categories = categorySummaries,
                        totalPaise = totalExpenditurePaise
                    )
                }
            }
        }

        // Daily / Monthly Trend Chart
        if (chartPoints.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Expenditure over time in ${activeRange.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (chartPoints.size >= 3) {
                            TrendAreaChart(
                                points = chartPoints,
                                lineColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            BarChart(
                                items = chartPoints,
                                barColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Category Totals Table / Breakdown List
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category Breakdown Table",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (categorySummaries.isEmpty()) {
                        Text(
                            text = "No category data recorded for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categorySummaries.forEach { cat ->
                            val color = CategoryIcons.parseColor(cat.colorHex)
                            val icon = CategoryIcons.getIcon(cat.iconKey)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${cat.transactionCount} txns • Avg: ${CurrencyUtils.formatInr(cat.averagePaise)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyUtils.formatInr(cat.totalPaise),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format("%.1f%%", cat.percentage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transactions in this report
        item {
            Text(
                text = "Transaction Details (${reportExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(reportExpenses, key = { it.id }) { expense ->
            val catObj = catMap[expense.categoryId]
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ExpenseRowItem(
                    expense = expense,
                    categoryIconKey = catObj?.iconKey ?: "other",
                    categoryColorHex = catObj?.colorHex ?: "#2563EB",
                    onEdit = { viewModel.expenseBeingEdited.value = expense },
                    onDelete = { viewModel.deleteExpense(expense.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDailyDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDailyDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dailyDatePickerState.selectedDateMillis?.let { millis ->
                        viewModel.reportSelectedDate.value = millis / 86400000L
                    }
                    showDailyDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = dailyDatePickerState)
        }
    }
}
