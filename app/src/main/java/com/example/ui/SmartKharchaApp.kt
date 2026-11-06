package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExpenseEntity
import com.example.ui.add_expense.AddExpenseScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.transactions.TransactionsScreen

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Download
import com.example.ui.download_apk.DownloadApkScreen

enum class AppScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TRANSACTIONS("Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    ADD_EXPENSE("Add Expense", Icons.Filled.Add, Icons.Filled.Add),
    REPORTS("Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    DOWNLOAD_APK("Download APK", Icons.Filled.Download, Icons.Outlined.Download)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartKharchaApp(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Custom Date Range Picker Dialog state
    var showCustomDateRangePicker by remember { mutableStateOf(false) }
    var targetRangeForCustom by remember { mutableStateOf<String>("dashboard") } // "dashboard" or "transactions"

    val dateRangePickerState = rememberDateRangePickerState()

    // Listen to ViewModel user messages (Snackbars)
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            // Tablet / Desktop layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("nav_rail"),
                    header = {
                        FloatingActionButton(
                            onClick = {
                                expenseToEdit = null
                                currentScreen = AppScreen.ADD_EXPENSE
                            },
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .testTag("fab_add_expense_rail")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Expense")
                        }
                    }
                ) {
                    AppScreen.values().filter { it != AppScreen.ADD_EXPENSE && it != AppScreen.DOWNLOAD_APK }.forEach { screen ->
                        val isSelected = currentScreen == screen || (screen == AppScreen.SETTINGS && currentScreen == AppScreen.DOWNLOAD_APK)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                expenseToEdit = null
                                currentScreen = screen
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_rail_${screen.name.lowercase()}")
                        )
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    ScreenContent(
                        currentScreen = currentScreen,
                        viewModel = viewModel,
                        expenseToEdit = expenseToEdit,
                        onNavigateToScreen = { screen ->
                            expenseToEdit = null
                            currentScreen = screen
                        },
                        onEditExpense = { exp ->
                            expenseToEdit = exp
                            currentScreen = AppScreen.ADD_EXPENSE
                        },
                        onRequestCustomRange = { target ->
                            targetRangeForCustom = target
                            showCustomDateRangePicker = true
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        } else {
            // Mobile layout with M3 Bottom Navigation Bar
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .testTag("bottom_nav_bar")
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AppScreen.values().filter { it != AppScreen.DOWNLOAD_APK }.forEach { screen ->
                            val isSelected = currentScreen == screen || (screen == AppScreen.SETTINGS && currentScreen == AppScreen.DOWNLOAD_APK)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (screen == AppScreen.ADD_EXPENSE) {
                                        expenseToEdit = null
                                    }
                                    currentScreen = screen
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("bottom_nav_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                ScreenContent(
                    currentScreen = currentScreen,
                    viewModel = viewModel,
                    expenseToEdit = expenseToEdit,
                    onNavigateToScreen = { screen ->
                        expenseToEdit = null
                        currentScreen = screen
                    },
                    onEditExpense = { exp ->
                        expenseToEdit = exp
                        currentScreen = AppScreen.ADD_EXPENSE
                    },
                    onRequestCustomRange = { target ->
                        targetRangeForCustom = target
                        showCustomDateRangePicker = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Material 3 DateRangePickerDialog
    if (showCustomDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showCustomDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis ?: startMillis
                    if (startMillis != null && endMillis != null) {
                        val startEpoch = startMillis / 86400000L
                        val endEpoch = endMillis / 86400000L
                        if (targetRangeForCustom == "dashboard") {
                            viewModel.setDashboardCustomRange(startEpoch, endEpoch)
                        } else {
                            viewModel.setTxCustomRange(startEpoch, endEpoch)
                        }
                    }
                    showCustomDateRangePicker = false
                }) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Select Custom Expense Date Range", modifier = Modifier.padding(16.dp)) }
            )
        }
    }
}

@Composable
fun ScreenContent(
    currentScreen: AppScreen,
    viewModel: ExpenseViewModel,
    expenseToEdit: ExpenseEntity?,
    onNavigateToScreen: (AppScreen) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onRequestCustomRange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = currentScreen,
        label = "screen_transition",
        modifier = modifier
    ) { screen ->
        when (screen) {
            AppScreen.DASHBOARD -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { categoryId ->
                        viewModel.txFilterCategoryId.value = categoryId
                        onNavigateToScreen(AppScreen.TRANSACTIONS)
                    },
                    onNavigateToAddExpense = { onNavigateToScreen(AppScreen.ADD_EXPENSE) },
                    onNavigateToSettings = { onNavigateToScreen(AppScreen.SETTINGS) },
                    onNavigateToDownloadApk = { onNavigateToScreen(AppScreen.DOWNLOAD_APK) },
                    onEditExpense = onEditExpense,
                    onCustomRangePickerRequest = { onRequestCustomRange("dashboard") }
                )
            }

            AppScreen.TRANSACTIONS -> {
                TransactionsScreen(
                    viewModel = viewModel,
                    onEditExpense = onEditExpense,
                    onCustomRangePickerRequest = { onRequestCustomRange("transactions") },
                    onNavigateBack = { onNavigateToScreen(AppScreen.DASHBOARD) }
                )
            }

            AppScreen.ADD_EXPENSE -> {
                AddExpenseScreen(
                    viewModel = viewModel,
                    expenseToEdit = expenseToEdit,
                    onExpenseSaved = {
                        onNavigateToScreen(AppScreen.TRANSACTIONS)
                    },
                    onNavigateBack = { onNavigateToScreen(AppScreen.DASHBOARD) }
                )
            }

            AppScreen.REPORTS -> {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { onNavigateToScreen(AppScreen.DASHBOARD) }
                )
            }

            AppScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToDownloadApk = { onNavigateToScreen(AppScreen.DOWNLOAD_APK) },
                    onNavigateBack = { onNavigateToScreen(AppScreen.DASHBOARD) }
                )
            }

            AppScreen.DOWNLOAD_APK -> {
                DownloadApkScreen(
                    viewModel = viewModel,
                    onNavigateBack = { onNavigateToScreen(AppScreen.SETTINGS) }
                )
            }
        }
    }
}
