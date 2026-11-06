package com.example.ui.add_expense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CategoryEntity
import com.example.data.ExpenseEntity
import com.example.model.PaymentMethod
import com.example.ui.ExpenseViewModel
import com.example.util.CategoryIcons
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import androidx.activity.compose.BackHandler
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    expenseToEdit: ExpenseEntity? = null,
    onExpenseSaved: () -> Unit,
    onNavigateBack: () -> Unit = onExpenseSaved,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountString by remember {
        mutableStateOf(
            if (expenseToEdit != null) CurrencyUtils.paiseToRupeeString(expenseToEdit.amountPaise) else ""
        )
    }
    var selectedCategoryId by remember { mutableStateOf<Long?>(expenseToEdit?.categoryId) }
    var selectedCategoryName by remember { mutableStateOf(expenseToEdit?.categoryName ?: "") }
    var expenseDateEpoch by remember { mutableStateOf(expenseToEdit?.expenseDate ?: DateUtils.todayEpochDay()) }
    var expenseTime by remember { mutableStateOf(expenseToEdit?.expenseTime ?: DateUtils.currentTimeString()) }
    var selectedPaymentMethod by remember { mutableStateOf(expenseToEdit?.paymentMethod ?: "UPI") }
    var notes by remember { mutableStateOf(expenseToEdit?.notes ?: "") }

    // Validation & Error states
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Dropdown / Dialog states
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var categorySearchFilter by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showQuickAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }

    // Set default category if creating new
    LaunchedEffect(activeCategories) {
        if (selectedCategoryId == null && activeCategories.isNotEmpty()) {
            val defaultCat = activeCategories.firstOrNull { it.name == "Groceries" } ?: activeCategories.first()
            selectedCategoryId = defaultCat.id
            selectedCategoryName = defaultCat.name
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = expenseDateEpoch * 86400000L
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("add_expense_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (expenseToEdit != null) "Edit Expense" else "Record New Expense",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (expenseToEdit != null) "Update existing transaction details" else "Enter daily expenditure accurately",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expenseToEdit != null) {
                IconButton(onClick = onExpenseSaved) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel edit")
                }
            }
        }

        // Amount Field (High prominence with INR symbol)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Amount in INR *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountString,
                    onValueChange = {
                        amountString = it
                        amountError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    leadingIcon = {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    placeholder = { Text("0.00", style = MaterialTheme.typography.headlineSmall) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Parsed Preview
                val parsedPaise = CurrencyUtils.parseAmountToPaise(amountString)
                if (parsedPaise != null) {
                    Text(
                        text = "Formatted: ${CurrencyUtils.formatInr(parsedPaise)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Expense Title
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                titleError = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("title_input"),
            label = { Text("Expense Title *") },
            placeholder = { Text("e.g. Milk & Eggs, Petrol refill, Doctor fee") },
            isError = titleError != null,
            supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Category Selector Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { showQuickAddCategoryDialog = true },
                        modifier = Modifier.testTag("quick_add_category_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Category")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showCategoryDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("category_selector_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val currentCat = activeCategories.firstOrNull { it.id == selectedCategoryId }
                                val iconKey = currentCat?.iconKey ?: "other"
                                val colorHex = currentCat?.colorHex ?: "#2563EB"
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(CategoryIcons.parseColor(colorHex).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIcons.getIcon(iconKey),
                                        contentDescription = null,
                                        tint = CategoryIcons.parseColor(colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedCategoryName.ifBlank { "Select Category" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    // Category Dropdown with Search
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = {
                            showCategoryDropdown = false
                            categorySearchFilter = ""
                        },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        OutlinedTextField(
                            value = categorySearchFilter,
                            onValueChange = { categorySearchFilter = it },
                            placeholder = { Text("Filter categories...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            singleLine = true
                        )

                        val filteredCats = activeCategories.filter {
                            it.name.contains(categorySearchFilter, ignoreCase = true)
                        }

                        if (filteredCats.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching category") },
                                onClick = {}
                            )
                        } else {
                            filteredCats.forEach { cat ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIcons.getIcon(cat.iconKey),
                                            contentDescription = null,
                                            tint = CategoryIcons.parseColor(cat.colorHex)
                                        )
                                    },
                                    text = { Text(cat.name, fontWeight = if (cat.id == selectedCategoryId) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        selectedCategoryName = cat.name
                                        categoryError = null
                                        showCategoryDropdown = false
                                        categorySearchFilter = ""
                                    }
                                )
                            }
                        }
                    }
                }

                categoryError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // Date and Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Picker Field
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .weight(1.2f)
                    .height(56.dp)
                    .testTag("date_picker_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateUtils.formatDate(expenseDateEpoch),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Time Field
            OutlinedTextField(
                value = expenseTime,
                onValueChange = { expenseTime = it },
                modifier = Modifier
                    .weight(0.8f)
                    .testTag("time_input"),
                label = { Text("Time") },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Payment Method Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.values().take(3).forEach { pm ->
                        val isSelected = selectedPaymentMethod == pm.displayName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = pm.displayName },
                            label = { Text(pm.displayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.values().drop(3).forEach { pm ->
                        val isSelected = selectedPaymentMethod == pm.displayName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = pm.displayName },
                            label = { Text(pm.displayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Notes / Description (Optional)
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notes_input"),
            label = { Text("Notes / Description (Optional)") },
            placeholder = { Text("Add any extra details, invoice number, or recipient...") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button (with double-click protection)
        Button(
            onClick = {
                if (isSubmitting) return@Button

                // Validate Title
                if (title.isBlank()) {
                    titleError = "Expense title is required"
                    return@Button
                }

                // Validate Amount
                val amountPaise = CurrencyUtils.parseAmountToPaise(amountString)
                if (amountPaise == null || amountPaise <= 0L) {
                    amountError = "Please enter a valid positive amount in INR"
                    return@Button
                }

                // Validate Category
                val catId = selectedCategoryId
                if (catId == null) {
                    categoryError = "Please select an expense category"
                    return@Button
                }

                isSubmitting = true
                viewModel.saveExpense(
                    id = expenseToEdit?.id ?: 0L,
                    title = title,
                    amountPaise = amountPaise,
                    categoryId = catId,
                    categoryName = selectedCategoryName,
                    date = expenseDateEpoch,
                    time = expenseTime,
                    paymentMethod = selectedPaymentMethod,
                    notes = notes
                )
                onExpenseSaved()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_expense_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isSubmitting
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (expenseToEdit != null) "Update Expense" else "Save Expense Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        expenseDateEpoch = millis / 86400000L
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Quick Add Category Dialog (User does not need to leave the form)
    if (showQuickAddCategoryDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showQuickAddCategoryDialog = false },
            title = { Text("Create New Category", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Add a custom expense category to use immediately:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Gym & Fitness, Pet Care") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_cat_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.addCategory(newCatName.trim(), "other", "#2563EB")
                            selectedCategoryName = newCatName.trim()
                            showQuickAddCategoryDialog = false
                            newCatName = ""
                        }
                    },
                    modifier = Modifier.testTag("save_quick_cat_btn")
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
