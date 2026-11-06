package com.example.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CategoryEntity
import com.example.ui.ExpenseViewModel
import com.example.ui.components.BudgetStatusBanner
import com.example.ui.components.SimpleConfirmDialog
import com.example.util.CategoryIcons
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.ExportUtils
import kotlinx.coroutines.launch

import androidx.activity.compose.BackHandler

@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToDownloadApk: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val budgetStatus by viewModel.budgetStatus.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(CurrencyUtils.paiseToRupeeString(budgetStatus.overallBudgetPaise)) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryBeingRenamed by remember { mutableStateOf<CategoryEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }

    var showClearExpensesConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { ExportUtils.installApkFromUri(context, it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Settings & Preferences",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Budgets, categories, data backup & APK install options",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section: APK Install & Share (ऐप इंस्टॉल करें)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("apk_install_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDownloadApk() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Install App / APK Download",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "स्मार्ट खर्चा ऐप को फोन में इंस्टॉल या शेयर करें",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated Screen Navigation Button
                    Button(
                        onClick = onNavigateToDownloadApk,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_download_apk_screen_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download APK & Browser Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { ExportUtils.installCurrentApk(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("install_apk_direct_btn")
                        ) {
                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fast Install", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { ExportUtils.shareAppApk(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_apk_file_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share APK", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                apkPickerLauncher.launch("application/vnd.android.package-archive")
                            } catch (_: Exception) {
                                apkPickerLauncher.launch("*/*")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_apk_from_storage_btn")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install APK from Storage (फाइल से इंस्टॉल करें)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "APK इंस्टॉल करने के तरीके:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. 'Install APK' पर टैप करके सीधे पैकेज इंस्टॉलर खोलें।\n" +
                                        "2. 'Share APK' से अपने फोन में WhatsApp / Google Drive पर .apk फाइल भेजकर टैप करके इंस्टॉल करें।\n" +
                                        "3. AI Studio के ऊपर दाईं ओर Settings/Export मेनू से सीधे APK फाइल डाउनलोड कर सकते हैं।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section: Monthly Budget Management
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("budget_management_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                budgetInput = if (budgetStatus.overallBudgetPaise > 0) {
                                    CurrencyUtils.paiseToRupeeString(budgetStatus.overallBudgetPaise)
                                } else "40000"
                                showBudgetDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("set_budget_btn")
                        ) {
                            Text(if (budgetStatus.overallBudgetPaise > 0) "Adjust" else "Set Budget")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set an overall monthly spending limit. Budgets are tracking goals and never counted as expenses in totals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (budgetStatus.overallBudgetPaise > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        BudgetStatusBanner(
                            budgetStatus = budgetStatus,
                            onManageBudget = {
                                budgetInput = CurrencyUtils.paiseToRupeeString(budgetStatus.overallBudgetPaise)
                                showBudgetDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Section: Regional & Formatting Settings (Read-only / verified standards)
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
                        text = "Financial & Regional Standards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingRow(title = "Default Currency", value = "INR (₹) Indian Rupee")
                    SettingRow(title = "Currency Grouping", value = "₹1,25,000.00 (Lakhs & Crores)")
                    SettingRow(title = "Date Format", value = "DD/MM/YYYY (Indian Standard)")
                    SettingRow(title = "Default Timezone", value = "Asia/Kolkata (IST)")
                    SettingRow(title = "Calculation Engine", value = "High-precision Integer Paise (Zero Float Drift)")
                }
            }
        }

        // Section: Data Backup, Restore & CSV Export
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("backup_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Export & Cloud Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Export transactions to spreadsheet (CSV) or backup everything in portable JSON format.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csv = ExportUtils.generateTransactionsCsv(allExpenses)
                                ExportUtils.shareExportFile(
                                    context,
                                    "SmartKharcha_Full_Export.csv",
                                    "text/csv",
                                    csv
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("export_all_csv_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV")
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val (exps, cats, bgts) = viewModel.repository.getAllDataForBackup()
                                    val json = ExportUtils.createJsonBackup(exps, cats, bgts)
                                    ExportUtils.shareExportFile(
                                        context,
                                        "SmartKharcha_Backup_${DateUtils.todayEpochDay()}.json",
                                        "application/json",
                                        json
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("backup_json_btn")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON Backup")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            restoreJsonText = ""
                            restoreError = null
                            showRestoreDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("restore_backup_btn")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore from JSON Backup")
                    }
                }
            }
        }

        // Section: Sample Data & Reset
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("sample_data_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Demo Data & Maintenance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Load realistic expenses across categories to explore charts and reports, or clear records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.generateSampleData() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("load_sample_data_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Demo Data")
                        }

                        OutlinedButton(
                            onClick = { showClearExpensesConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("clear_expenses_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Expenses")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showResetAllConfirm = true },
                        modifier = Modifier.fillMaxWidth().testTag("reset_all_data_btn")
                    ) {
                        Text("Reset App to Factory Defaults", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Section: Category Management
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expense Categories (${allCategories.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to rename or toggle active visibility",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddCategoryDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_new_category_settings_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        // List of all categories
        items(allCategories, key = { it.id }) { cat ->
            val color = CategoryIcons.parseColor(cat.colorHex)
            val icon = CategoryIcons.getIcon(cat.iconKey)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (cat.isDefault) "Default System Category" else "Custom Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Rename button
                    IconButton(
                        onClick = {
                            categoryBeingRenamed = cat
                            renameInput = cat.name
                        },
                        modifier = Modifier.size(32.dp).testTag("rename_cat_${cat.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Active Toggle
                    IconButton(
                        onClick = { viewModel.setCategoryActive(cat.id, !cat.isActive) },
                        modifier = Modifier.size(32.dp).testTag("toggle_cat_${cat.id}")
                    ) {
                        Icon(
                            imageVector = if (cat.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (cat.isActive) "Active" else "Hidden",
                            tint = if (cat.isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog: Adjust Monthly Budget
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter overall spending budget in INR for ${DateUtils.formatMonthYear(DateUtils.todayEpochDay())}:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Budget Amount (INR)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("budget_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paise = CurrencyUtils.parseAmountToPaise(budgetInput)
                        if (paise != null) {
                            viewModel.setMonthlyBudget(paise)
                            showBudgetDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_budget_confirm_btn")
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add Custom Category
    if (showAddCategoryDialog) {
        var newName by remember { mutableStateOf("") }
        var selectedIconKey by remember { mutableStateOf("shopping") }
        val colorHex = "#2563EB"

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Create Expense Category", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Category Name *") },
                        placeholder = { Text("e.g. Subscriptions, Pet Care") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_new_cat_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCategory(newName.trim(), selectedIconKey, colorHex)
                            showAddCategoryDialog = false
                        }
                    },
                    modifier = Modifier.testTag("settings_save_cat_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Rename Category
    categoryBeingRenamed?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryBeingRenamed = null },
            title = { Text("Rename Category", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Renaming will safely update this category across all historical expense records without data loss:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_cat_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameCategory(cat.id, renameInput)
                            categoryBeingRenamed = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_rename_btn")
                ) {
                    Text("Save Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryBeingRenamed = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Restore from JSON
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore JSON Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste your valid Smart Kharcha JSON backup content below. Data will be validated before saving:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = {
                            restoreJsonText = it
                            restoreError = null
                        },
                        label = { Text("Backup JSON String") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().height(160.dp).testTag("restore_json_input")
                    )
                    restoreError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromJson(restoreJsonText) { success ->
                            if (success) {
                                showRestoreDialog = false
                            } else {
                                restoreError = "Invalid or corrupted backup format"
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_btn")
                ) {
                    Text("Validate & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm: Clear All Expenses
    if (showClearExpensesConfirm) {
        SimpleConfirmDialog(
            title = "Clear All Expenses?",
            message = "Are you sure you want to delete all recorded expenses? This cannot be undone.",
            confirmText = "Clear Everything",
            isDestructive = true,
            onConfirm = {
                viewModel.clearAllExpenses()
                showClearExpensesConfirm = false
            },
            onDismiss = { showClearExpensesConfirm = false }
        )
    }

    // Confirm: Reset App
    if (showResetAllConfirm) {
        SimpleConfirmDialog(
            title = "Reset Entire Application?",
            message = "This will delete all expense records, custom categories, and budgets, restoring initial default settings.",
            confirmText = "Reset All",
            isDestructive = true,
            onConfirm = {
                viewModel.clearAllData()
                showResetAllConfirm = false
            },
            onDismiss = { showResetAllConfirm = false }
        )
    }
}

@Composable
fun SettingRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
