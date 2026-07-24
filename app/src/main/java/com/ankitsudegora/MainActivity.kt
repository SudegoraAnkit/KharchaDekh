package com.ankitsudegora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ankitsudegora.ui.screens.*
import com.ankitsudegora.ui.theme.KharchaDekhTheme
import com.ankitsudegora.viewmodel.*
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.ankitsudegora.util.BackupManager
import com.ankitsudegora.util.RestoreResult
import com.ankitsudegora.data.TransactionWithCategory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

enum class AppTab {
    DASHBOARD, CALENDAR, ADD_MANUAL, CATEGORIES, SETTINGS, PLANNED_LISTS, CREDIT_CARD
}

enum class ExportScope {
    THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM
}

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModel.Factory(application)
    }

    private val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            lifecycleScope.launch {
                val success = BackupManager.backupDatabase(this@MainActivity, uri)
                if (success) {
                    Toast.makeText(this@MainActivity, "Database backup created successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to create database backup.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            lifecycleScope.launch {
                when (val result = BackupManager.restoreDatabase(this@MainActivity, uri)) {
                    is RestoreResult.Success -> {
                        Toast.makeText(this@MainActivity, "Database restored successfully! Restarting app...", Toast.LENGTH_LONG).show()
                    }
                    is RestoreResult.Failure -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to restore database: ${result.message} (${result.errorCode})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
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
                                Toast.makeText(this, "Could not open notification settings. Please enable manually in settings.", Toast.LENGTH_LONG).show()
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
            viewModel.setActiveEnrichId(enrichId)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppScaffold() {
        var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
        var showFilterScreen by remember { mutableStateOf(false) }
        
        // Navigation states integrated into Viewmodel StateFlow to survive configuration changes
        val activeEnrichId by viewModel.activeEnrichId.collectAsStateWithLifecycle()

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
        val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
        val plannedLists by viewModel.allPlannedLists.collectAsStateWithLifecycle()
        val isMultiCurrencyEnabled by viewModel.isMultiCurrencyEnabled.collectAsStateWithLifecycle()
        val primaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
        
        // Collect active recurring schedules Flow
        val recurringSchedules by viewModel.allSchedules.collectAsStateWithLifecycle(initialValue = emptyList())

        // Export State Variables
        var transactionsForExport by remember { mutableStateOf<List<TransactionWithCategory>>(emptyList()) }
        var exportFormatType by remember { mutableStateOf("CSV") }
        var displayExportDialog by remember { mutableStateOf(false) }
        var displayCustomDatePicker by remember { mutableStateOf(false) }

        val onExportCsvClick: (List<TransactionWithCategory>) -> Unit = { txns ->
            transactionsForExport = txns
            exportFormatType = "CSV"
            displayExportDialog = true
        }

        val onExportPdfClick: (List<TransactionWithCategory>) -> Unit = { txns ->
            transactionsForExport = txns
            exportFormatType = "PDF"
            displayExportDialog = true
        }

        if (showFilterScreen) {
            AdvancedFilterScreen(
                categories = categories,
                allTransactions = allTransactions,
                onNavigateBack = { showFilterScreen = false },
                onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) },
                onEditTransaction = { id ->
                    viewModel.setActiveEnrichId(id)
                    showFilterScreen = false
                }
            )
        } else if (activeEnrichId != null) {
            EnrichmentScreen(
                transactionId = activeEnrichId!!,
                categories = categories,
                creditCards = creditCards,
                allTransactions = allTransactions,
                allPlannedLists = plannedLists,
                onGetTransaction = { id -> viewModel.getTransactionById(id) },
                onFinalizeTransaction = { id, catId, notes, amount, merchant, type, recurringFreq, subCat, paidViaCcId, repaidCcId, repaidTxnIds, linkedListId, refundedTxnId ->
                    viewModel.finalizeSmsTransaction(
                        id = id,
                        categoryId = catId,
                        notes = notes,
                        amount = amount,
                        merchant = merchant,
                        type = type,
                        recurringFrequency = recurringFreq,
                        subCategory = subCat,
                        paidViaCcId = paidViaCcId,
                        repaidCcId = repaidCcId,
                        selectedRepaidTxnIds = repaidTxnIds,
                        linkedListId = linkedListId,
                        refundedTxnId = refundedTxnId
                    )
                    viewModel.setActiveEnrichId(null) // Close enriching panel
                },
                onNavigateBack = {
                    viewModel.setActiveEnrichId(null)
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
                            selected = currentTab == AppTab.PLANNED_LISTS,
                            onClick = { currentTab = AppTab.PLANNED_LISTS },
                            label = { Text("Lists") },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.PLANNED_LISTS) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Outlined.PlaylistAddCheck,
                                    contentDescription = "Planned Lists"
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.CREDIT_CARD,
                            onClick = { currentTab = AppTab.CREDIT_CARD },
                            label = { Text("Cards") },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.CREDIT_CARD) Icons.Filled.CreditCard else Icons.Outlined.CreditCard,
                                    contentDescription = "Credit Cards"
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
                                onEnrichTransaction = { id -> viewModel.setActiveEnrichId(id) },
                                onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) },
                                onNavigateToCategories = { currentTab = AppTab.CATEGORIES },
                                onExportCsv = { onExportCsvClick(allTransactions) },
                                onExportPdf = { onExportPdfClick(allTransactions) },
                                monthlyIncome = monthlyIncome,
                                savingsTargetPct = savingsTargetPct,
                                spendingTargetPct = spendingTargetPct,
                                monthlyCategorySpends = monthlyCategorySpends,
                                forecastAllowance = forecastAllowance,
                                onSearchClicked = { showFilterScreen = true },
                                onSettingsClicked = { currentTab = AppTab.SETTINGS },
                                billingCycleStartDay = billingCycleStartDay,
                                onExportCsvCalendar = { txns -> performExportCsv(txns) },
                                onExportPdfCalendar = { txns -> performExportPdf(txns) },
                                primaryCurrency = primaryCurrency,
                                onConvertAmount = { amount, fromCurrency -> viewModel.convertAmount(amount, fromCurrency, primaryCurrency) }
                            )
                        }
                        AppTab.CALENDAR -> {
                            // Redirect to DashboardFeed where Calendar is now inline
                            currentTab = AppTab.DASHBOARD
                        }
                        AppTab.ADD_MANUAL -> {
                            AddManualScreen(
                                categories = categories,
                                creditCards = creditCards,
                                allTransactions = allTransactions,
                                isMultiCurrencyEnabled = isMultiCurrencyEnabled,
                                primaryCurrency = primaryCurrency,
                                onSaveTransaction = { amount, type, merchant, catId, notes, method, date, recurringFreq, subCat, paidViaCcId, repaidCcId, repaidTxnIds, currency ->
                                    viewModel.addManualTransaction(amount, type, merchant, catId, notes, method, date, recurringFreq, subCat, paidViaCcId, repaidCcId, repaidTxnIds, currency)
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
                                onRestoreDatabase = { openDocLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
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
                                onUpdateBillingCycleStartDay = { day -> viewModel.updateBillingCycleStartDay(day) },
                                onNavigateToCategories = { currentTab = AppTab.CATEGORIES },
                                creditCards = creditCards,
                                onAddCreditCard = { name -> viewModel.addCreditCard(name) },
                                onDeleteCreditCard = { card -> viewModel.deleteCreditCard(card) },
                                isMultiCurrencyEnabled = isMultiCurrencyEnabled,
                                onToggleMultiCurrency = { enabled -> viewModel.updateMultiCurrencyEnabled(enabled) },
                                primaryCurrency = primaryCurrency,
                                onUpdatePrimaryCurrency = { currency -> viewModel.updatePrimaryCurrency(currency) }
                            )
                        }
                        AppTab.PLANNED_LISTS -> {
                            PlannedListsScreen(
                                plannedLists = plannedLists,
                                categories = categories,
                                pendingTransactions = pendingTransactions,
                                onAddList = { name, cap, catId -> viewModel.addPlannedList(name, cap, catId) },
                                onDeleteList = { list -> viewModel.deletePlannedList(list) },
                                onDuplicateList = { list, name -> viewModel.duplicatePlannedList(list, name) },
                                onAddItem = { listId, name, qty, price -> viewModel.addPlannedItem(listId, name, qty, price) },
                                onUpdateItem = { item -> viewModel.updatePlannedItem(item) },
                                onDeleteItem = { item -> viewModel.deletePlannedItem(item) },
                                onToggleItem = { item -> viewModel.togglePlannedItemChecked(item) },
                                onGetLastPrice = { name -> viewModel.getLastPriceForItem(name) ?: 0.0 },
                                onCheckout = { list, method, categoryId, carryForward, linkedPendingTxnId ->
                                    viewModel.markPlannedListAsPaid(list, method, categoryId, carryForward, linkedPendingTxnId)
                                },
                                onUpdateTransactionAmount = { txnId, amount ->
                                    viewModel.updateTransactionAmount(txnId, amount)
                                },
                                onGetTransactionsByLinkedListId = { listId ->
                                    viewModel.getTransactionsByLinkedListId(listId)
                                },
                                onUnlinkTransaction = { txnId ->
                                    viewModel.unlinkTransactionFromPlannedList(txnId)
                                },
                                onLinkTransaction = { txnId, listId ->
                                    viewModel.linkTransactionToPlannedList(txnId, listId)
                                }
                            )
                        }
                        AppTab.CREDIT_CARD -> {
                            val creditCardsVal by viewModel.allCreditCards.collectAsStateWithLifecycle()
                            val allTxnsVal by viewModel.allTransactions.collectAsStateWithLifecycle()
                            CreditCardScreen(
                                creditCards = creditCardsVal,
                                allTransactions = allTxnsVal,
                                onRepayCard = { card, amount, txnIds ->
                                    viewModel.repayCreditCardBill(card, amount, txnIds)
                                },
                                onExportStatement = { card, transactions ->
                                    lifecycleScope.launch {
                                        val uri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            com.ankitsudegora.util.Exporter.exportCcStatement(this@MainActivity, card.cardName, transactions)
                                        }
                                        if (uri != null) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            startActivity(android.content.Intent.createChooser(intent, "Share Credit Card Statement"))
                                        } else {
                                            Toast.makeText(this@MainActivity, "Failed to generate CSV statement.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Export Range Selection Dialog Overlay
        if (displayExportDialog) {
            ExportFilterDialog(
                onDismiss = { displayExportDialog = false },
                onOptionSelected = { scope ->
                    displayExportDialog = false
                    val now = System.currentTimeMillis()
                    when (scope) {
                        ExportScope.THIS_WEEK -> {
                            val startLimit = now - 7L * 24 * 60 * 60 * 1000
                            val filtered = transactionsForExport.filter {
                                it.transaction.timestamp in startLimit..now
                            }
                            if (exportFormatType == "CSV") performExportCsv(filtered) else performExportPdf(filtered)
                        }
                        ExportScope.THIS_MONTH -> {
                            val startOfCycle = viewModel.getStartOfBillingCycleTimestamp(billingCycleStartDay, now)
                            val endOfCycle = viewModel.getStartOfNextBillingCycleTimestamp(billingCycleStartDay, now)
                            val filtered = transactionsForExport.filter {
                                it.transaction.timestamp in startOfCycle until endOfCycle
                            }
                            if (exportFormatType == "CSV") performExportCsv(filtered) else performExportPdf(filtered)
                        }
                        ExportScope.THIS_YEAR -> {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = now
                            cal.set(Calendar.MONTH, Calendar.JANUARY)
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val startLimit = cal.timeInMillis
                            val filtered = transactionsForExport.filter {
                                it.transaction.timestamp in startLimit..now
                            }
                            if (exportFormatType == "CSV") performExportCsv(filtered) else performExportPdf(filtered)
                        }
                        ExportScope.CUSTOM -> {
                            displayCustomDatePicker = true
                        }
                    }
                }
            )
        }

        // Material 3 Custom DateRangePicker Dialog
        if (displayCustomDatePicker) {
            DateRangePickerDialog(
                onDismissRequest = { displayCustomDatePicker = false },
                onConfirm = { startDate, endDate ->
                    displayCustomDatePicker = false
                    if (startDate != null && endDate != null) {
                        val endOfDayCal = Calendar.getInstance()
                        endOfDayCal.timeInMillis = endDate
                        endOfDayCal.set(Calendar.HOUR_OF_DAY, 23)
                        endOfDayCal.set(Calendar.MINUTE, 59)
                        endOfDayCal.set(Calendar.SECOND, 59)
                        endOfDayCal.set(Calendar.MILLISECOND, 999)
                        val endLimit = endOfDayCal.timeInMillis
                        
                        val filtered = transactionsForExport.filter {
                            it.transaction.timestamp in startDate..endLimit
                        }
                        if (exportFormatType == "CSV") performExportCsv(filtered) else performExportPdf(filtered)
                    }
                }
            )
        }
    }

    @Composable
    fun ExportFilterDialog(
        onDismiss: () -> Unit,
        onOptionSelected: (ExportScope) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Select Export Range", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOptionSelected(ExportScope.THIS_WEEK) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("This Week (Preceding 7 days)")
                    }
                    Button(
                        onClick = { onOptionSelected(ExportScope.THIS_MONTH) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("This Month (Billing Cycle)")
                    }
                    Button(
                        onClick = { onOptionSelected(ExportScope.THIS_YEAR) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("This Year (Calendar Year)")
                    }
                    Button(
                        onClick = { onOptionSelected(ExportScope.CUSTOM) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text("Custom Date Range...")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DateRangePickerDialog(
        onDismissRequest: () -> Unit,
        onConfirm: (Long?, Long?) -> Unit
    ) {
        val state = rememberDateRangePickerState()
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel")
                        }
                        Text(
                            text = "Select Date Range",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(
                            onClick = {
                                onConfirm(state.selectedStartDateMillis, state.selectedEndDateMillis)
                            },
                            enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
                        ) {
                            Text("Confirm")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DateRangePicker(
                        state = state,
                        modifier = Modifier.weight(1f)
                    )
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

    private fun performExportCsv(transactions: List<TransactionWithCategory>) {
        val uri = com.ankitsudegora.util.Exporter.exportToCsv(this, transactions)
        if (uri != null) {
            shareFile(uri, "text/csv", "Share CSV Expense Report")
        } else {
            Toast.makeText(this, "Failed to export CSV report", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performExportPdf(transactions: List<TransactionWithCategory>) {
        val uri = com.ankitsudegora.util.Exporter.exportToPdf(this, transactions)
        if (uri != null) {
            shareFile(uri, "application/pdf", "Share PDF Expense Report")
        } else {
            Toast.makeText(this, "Failed to export PDF report", Toast.LENGTH_SHORT).show()
        }
    }
}
