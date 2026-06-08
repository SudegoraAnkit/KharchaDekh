package com.ankitsudegora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ankitsudegora.ui.screens.*
import com.ankitsudegora.ui.theme.KharchaDekhTheme
import com.ankitsudegora.viewmodel.*
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.ankitsudegora.util.BackupManager
import com.ankitsudegora.data.TransactionWithCategory

enum class AppTab {
    DASHBOARD, CALENDAR, ADD_MANUAL, CATEGORIES, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModel.Factory(application)
    }

    private val activeEnrichIdState = mutableStateOf<Long?>(null)

    private val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = BackupManager.backupDatabase(this, uri)
            if (success) {
                Toast.makeText(this, "Database backup created successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to create database backup.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val success = BackupManager.restoreDatabase(this, uri)
            if (success) {
                Toast.makeText(this, "Database restored successfully! Restarting app...", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to restore database backup.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial notification click intent
        handleIntentRouting(intent)

        setContent {
            KharchaDekhTheme {
                val onboardingState by viewModel.onboardingState.collectAsStateWithLifecycle()
                val userName by viewModel.userName.collectAsStateWithLifecycle()

                if (onboardingState == OnboardingState.Required) {
                    OnboardingScreen(
                        onConsentGranted = { name ->
                            viewModel.updateUserName(name)
                            viewModel.completeOnboarding(true) 
                            try {
                                val settingsIntent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(settingsIntent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(this, "Could not open notification settings. Please enable manually in settings.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onManualOnlyClicked = { 
                            viewModel.completeOnboarding(false) 
                        }
                    )
                } else {
                    MainAppScaffold()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntentRouting(intent)
    }

    private fun handleIntentRouting(intent: android.content.Intent?) {
        val enrichId = intent?.getLongExtra("EXTRA_TRANSACTION_ID", -1L) ?: -1L
        if (enrichId != -1L) {
            activeEnrichIdState.value = enrichId
        }
    }

    @Composable
    fun MainAppScaffold() {
        var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
        var showFilterScreen by remember { mutableStateOf(false) }
        val activeEnrichId by activeEnrichIdState

        val analytics by viewModel.analyticsState.collectAsStateWithLifecycle()
        val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
        val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
        val categories by viewModel.allCategories.collectAsStateWithLifecycle()
        val selectedFilter by viewModel.timeboxFilter.collectAsStateWithLifecycle()
        val rHour by viewModel.reminderHour.collectAsStateWithLifecycle()
        val rMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
        val userName by viewModel.userName.collectAsStateWithLifecycle()
        val monthlyIncome by viewModel.monthlyIncome.collectAsStateWithLifecycle()
        val savingsTargetPct by viewModel.savingsTargetPct.collectAsStateWithLifecycle()
        val spendingTargetPct by viewModel.spendingTargetPct.collectAsStateWithLifecycle()
        val autoBackupNight by viewModel.autoBackupNight.collectAsStateWithLifecycle()
        val billingCycleStartDay by viewModel.billingCycleStartDay.collectAsStateWithLifecycle()
        val monthlyCategorySpends by viewModel.monthlyCategorySpends.collectAsStateWithLifecycle()
        val forecastAllowance by viewModel.forecastAllowance.collectAsStateWithLifecycle()
        
        // Collect active recurring schedules Flow
        val recurringSchedules by viewModel.allSchedules.collectAsStateWithLifecycle(initialValue = emptyList())

        if (showFilterScreen) {
            AdvancedFilterScreen(
                categories = categories,
                allTransactions = allTransactions,
                onNavigateBack = { showFilterScreen = false },
                onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) },
                onEditTransaction = { id ->
                    activeEnrichIdState.value = id
                    showFilterScreen = false
                }
            )
        } else if (activeEnrichId != null) {
            EnrichmentScreen(
                transactionId = activeEnrichId!!,
                categories = categories,
                onGetTransaction = { id -> viewModel.getTransactionById(id) },
                onFinalizeTransaction = { id, catId, notes, amount, merchant, type, recurringFreq ->
                    viewModel.finalizeSmsTransaction(id, catId, notes, amount, merchant, type, recurringFreq)
                    activeEnrichIdState.value = null // Close enriching panel
                },
                onNavigateBack = {
                    activeEnrichIdState.value = null
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.DASHBOARD,
                            onClick = { currentTab = AppTab.DASHBOARD },
                            label = { Text("Feed") },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.DASHBOARD) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                                    contentDescription = "Dashboard"
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.CALENDAR,
                            onClick = { currentTab = AppTab.CALENDAR },
                            label = { Text("Calendar") },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.CALENDAR) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                    contentDescription = "Calendar"
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.ADD_MANUAL,
                            onClick = { currentTab = AppTab.ADD_MANUAL },
                            label = { Text("Log Cash") },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.ADD_MANUAL) Icons.Filled.AddCircle else Icons.Outlined.AddCircle,
                                    contentDescription = "Log Cash"
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.CATEGORIES,
                            onClick = { currentTab = AppTab.CATEGORIES },
                            label = { Text("Categories") },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.CATEGORIES) Icons.Filled.Category else Icons.Outlined.Category,
                                    contentDescription = "Categories"
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.SETTINGS,
                            onClick = { currentTab = AppTab.SETTINGS },
                            label = { Text("Settings") },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                userName = userName,
                                analytics = analytics,
                                pendingTransactions = pendingTransactions,
                                allTransactions = allTransactions,
                                categories = categories,
                                selectedFilter = selectedFilter,
                                onFilterSelected = { filter -> viewModel.setTimeboxFilter(filter) },
                                onEnrichTransaction = { id -> activeEnrichIdState.value = id },
                                onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) },
                                onNavigateToCategories = { currentTab = AppTab.CATEGORIES },
                                onExportCsv = { handleExportCsv(allTransactions) },
                                onExportPdf = { handleExportPdf(allTransactions) },
                                monthlyIncome = monthlyIncome,
                                savingsTargetPct = savingsTargetPct,
                                spendingTargetPct = spendingTargetPct,
                                monthlyCategorySpends = monthlyCategorySpends,
                                forecastAllowance = forecastAllowance,
                                onSearchClicked = { showFilterScreen = true }
                            )
                        }
                        AppTab.CALENDAR -> {
                            CalendarScreen(
                                allTransactions = allTransactions,
                                onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) },
                                onEditTransaction = { id -> activeEnrichIdState.value = id }
                            )
                        }
                        AppTab.ADD_MANUAL -> {
                            AddManualScreen(
                                categories = categories,
                                onSaveTransaction = { amount, type, merchant, catId, notes, method, date, recurringFreq ->
                                    viewModel.addManualTransaction(amount, type, merchant, catId, notes, method, date, recurringFreq)
                                    currentTab = AppTab.DASHBOARD // navigate back automatically
                                },
                                onNavigateBack = { currentTab = AppTab.DASHBOARD }
                            )
                        }
                        AppTab.CATEGORIES -> {
                            CategoriesScreen(
                                categories = categories,
                                onAddCategory = { name, icon -> viewModel.addCustomCategory(name, icon) },
                                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                                onUpdateCategoryBudget = { cat, budget -> viewModel.updateCategoryBudget(cat, budget) }
                            )
                        }
                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                userName = userName,
                                onUpdateUserName = { name -> viewModel.updateUserName(name) },
                                currentHour = rHour,
                                currentMinute = rMinute,
                                onUpdateTime = { h, m -> viewModel.updateReminderTime(h, m) },
                                onResetOnboarding = { viewModel.resetOnboarding() },
                                onSimulateSms = { body -> viewModel.simulateSmsTransaction(body) },
                                onBackupDatabase = { createDocLauncher.launch("kharchadekh_backup.zip") },
                                onRestoreDatabase = { openDocLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                                recurringSchedules = recurringSchedules,
                                categories = categories,
                                onToggleSchedule = { s -> viewModel.toggleRecurringSchedule(s) },
                                onDeleteSchedule = { s -> viewModel.deleteRecurringSchedule(s) },
                                monthlyIncome = monthlyIncome,
                                savingsTargetPct = savingsTargetPct,
                                spendingTargetPct = spendingTargetPct,
                                onUpdateBudgetGoals = { inc, sav, spnd -> viewModel.updateBudgetGoals(inc, sav, spnd) },
                                autoBackupNight = autoBackupNight,
                                onUpdateAutoBackupNight = { enabled -> viewModel.updateAutoBackupNight(enabled) },
                                billingCycleStartDay = billingCycleStartDay,
                                onUpdateBillingCycleStartDay = { day -> viewModel.updateBillingCycleStartDay(day) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun shareFile(uri: Uri, mimeType: String, title: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, title))
    }

    private fun handleExportCsv(transactions: List<TransactionWithCategory>) {
        val uri = com.ankitsudegora.util.Exporter.exportToCsv(this, transactions)
        if (uri != null) {
            shareFile(uri, "text/csv", "Share CSV Expense Report")
        } else {
            Toast.makeText(this, "Failed to export CSV report", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleExportPdf(transactions: List<TransactionWithCategory>) {
        val uri = com.ankitsudegora.util.Exporter.exportToPdf(this, transactions)
        if (uri != null) {
            shareFile(uri, "application/pdf", "Share PDF Expense Report")
        } else {
            Toast.makeText(this, "Failed to export PDF report", Toast.LENGTH_SHORT).show()
        }
    }
}
