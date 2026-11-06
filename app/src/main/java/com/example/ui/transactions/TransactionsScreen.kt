package com.example.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExpenseEntity
import com.example.model.DateRangeOption
import com.example.model.PaymentMethod
import com.example.model.SortOption
import com.example.ui.ExpenseViewModel
import com.example.ui.components.DateRangeChipsRow
import com.example.ui.components.ExpenseRowItem
import androidx.activity.compose.BackHandler
import com.example.ui.components.SimpleConfirmDialog
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.ExportUtils

@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onEditExpense: (ExpenseEntity) -> Unit,
    onCustomRangePickerRequest: () -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.txSearchQuery.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.txFilterCategoryId.collectAsStateWithLifecycle()
    val selectedPayment by viewModel.txFilterPaymentMethod.collectAsStateWithLifecycle()
    val sortOption by viewModel.txSortOption.collectAsStateWithLifecycle()
    val dateRangeOption by viewModel.txDateRangeOption.collectAsStateWithLifecycle()
    val activeRange by viewModel.txDateRange.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryFilterMenu by remember { mutableStateOf(false) }
    var showPaymentFilterMenu by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showFilterPanel by remember { mutableStateOf(false) }

    val catMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val grandTotalPaise = remember(transactions) { transactions.sumOf { it.amountPaise } }

    // Group records by date (each date displays its daily subtotal)
    val groupedByDate = remember(transactions) {
        transactions.groupBy { it.expenseDate }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        // Top App Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${transactions.size} records • Grand Total: ${CurrencyUtils.formatInr(grandTotalPaise)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Export to CSV Button
            IconButton(
                onClick = {
                    val csv = ExportUtils.generateTransactionsCsv(transactions)
                    ExportUtils.shareExportFile(
                        context,
                        "SmartKharcha_Transactions_${DateUtils.formatDate(DateUtils.todayEpochDay()).replace("/", "-")}.csv",
                        "text/csv",
                        csv
                    )
                },
                modifier = Modifier.testTag("export_csv_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export Filtered CSV",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.txSearchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("tx_search_input"),
            placeholder = { Text("Search by title, notes, or category...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.txSearchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Date Range Selector
        DateRangeChipsRow(
            selectedOption = dateRangeOption,
            onSelectOption = { viewModel.txDateRangeOption.value = it },
            onCustomRangeClick = onCustomRangePickerRequest
        )

        // Filter & Sort Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Dropdown
            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("sort_btn")
                ) {
                    Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = sortOption.displayName, style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                viewModel.txSortOption.value = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Category Filter Dropdown
            Box {
                OutlinedButton(
                    onClick = { showCategoryFilterMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("filter_category_btn")
                ) {
                    Text(
                        text = if (selectedCatId != null) (catMap[selectedCatId]?.name ?: "Category") else "All Categories",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = showCategoryFilterMenu,
                    onDismissRequest = { showCategoryFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Categories") },
                        onClick = {
                            viewModel.txFilterCategoryId.value = null
                            showCategoryFilterMenu = false
                        }
                    )
                    allCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                viewModel.txFilterCategoryId.value = cat.id
                                showCategoryFilterMenu = false
                            }
                        )
                    }
                }
            }

            // Payment Filter Dropdown
            Box {
                OutlinedButton(
                    onClick = { showPaymentFilterMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("filter_payment_btn")
                ) {
                    Text(
                        text = selectedPayment ?: "All Payment",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                DropdownMenu(
                    expanded = showPaymentFilterMenu,
                    onDismissRequest = { showPaymentFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Payment Methods") },
                        onClick = {
                            viewModel.txFilterPaymentMethod.value = null
                            showPaymentFilterMenu = false
                        }
                    )
                    PaymentMethod.values().forEach { pm ->
                        DropdownMenuItem(
                            text = { Text(pm.displayName) },
                            onClick = {
                                viewModel.txFilterPaymentMethod.value = pm.displayName
                                showPaymentFilterMenu = false
                            }
                        )
                    }
                }
            }

            // Clear Filters button if any active
            if (selectedCatId != null || selectedPayment != null || searchQuery.isNotBlank() || dateRangeOption != DateRangeOption.ALL_TIME) {
                IconButton(
                    onClick = {
                        viewModel.txSearchQuery.value = ""
                        viewModel.txFilterCategoryId.value = null
                        viewModel.txFilterPaymentMethod.value = null
                        viewModel.txDateRangeOption.value = DateRangeOption.ALL_TIME
                    },
                    modifier = Modifier.testTag("clear_all_filters_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Filters",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Active Filter indicator badge
        if (selectedCatId != null || selectedPayment != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedCatId?.let { id ->
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.txFilterCategoryId.value = null },
                        label = { Text("Category: ${catMap[id]?.name ?: id}") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
                selectedPayment?.let { pm ->
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.txFilterPaymentMethod.value = null },
                        label = { Text("Payment: $pm") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Transaction List Grouped by Date
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching expenses found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search keywords or date/category filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedByDate.forEach { (epochDay, dayExpenses) ->
                    val dayTotalPaise = dayExpenses.sumOf { it.amountPaise }
                    val dateFormatted = DateUtils.formatDate(epochDay)

                    // Date Header with Daily Subtotal
                    item(key = "header_$epochDay") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormatted,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Daily Total: ${CurrencyUtils.formatInr(dayTotalPaise)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Transactions for that date
                    items(dayExpenses, key = { it.id }) { expense ->
                        val catObj = catMap[expense.categoryId]
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ExpenseRowItem(
                                expense = expense,
                                categoryIconKey = catObj?.iconKey ?: "other",
                                categoryColorHex = catObj?.colorHex ?: "#2563EB",
                                onEdit = { onEditExpense(expense) },
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    expenseToDelete?.let { exp ->
        SimpleConfirmDialog(
            title = "Delete Expense?",
            message = "Are you sure you want to delete '${exp.title}' for ${CurrencyUtils.formatInr(exp.amountPaise)}? This will recalculate all affected financial totals.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteExpense(exp.id)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}
