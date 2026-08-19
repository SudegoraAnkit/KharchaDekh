package com.ankitsudegora.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ankitsudegora.R
import com.ankitsudegora.data.*
import com.ankitsudegora.worker.ReminderWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

sealed interface OnboardingState {
    object Required : OnboardingState
    object Completed : OnboardingState
}

enum class TimeboxFilter {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

data class CategoryUsage(
    val category: Category,
    val amount: Double,
    val percentage: Float
)

data class AnalyticsState(
    val totalExpense: Double = 0.0,
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val categoryBreakdown: List<CategoryUsage> = emptyList()
)

data class ForecastAllowance(
    val dailyAllowance: Double = 0.0,
    val weeklyAllowance: Double = 0.0,
    val isOverspent: Boolean = false,
    val remainingDays: Int = 1,
    val fixedCommitments: Double = 0.0,
    val discretionarySpent: Double = 0.0,
    val discretionarySpendingCap: Double = 0.0
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ExpenseRepository(db.transactionDao(), db.categoryDao(), db.recurringScheduleDao(), db.plannedDao(), db.creditCardDao())
    private val prefs = application.getSharedPreferences("kharchadekh_prefs", Context.MODE_PRIVATE)

    private val _billingCycleStartDay = MutableStateFlow(prefs.getInt("billing_cycle_start_day", 1))
    val billingCycleStartDay: StateFlow<Int> = _billingCycleStartDay.asStateFlow()

    private val _monthlyIncome = MutableStateFlow(prefs.getFloat("monthly_income", 0f).toDouble())
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _savingsTargetPct = MutableStateFlow(prefs.getInt("savings_target_pct", 20))
    val savingsTargetPct: StateFlow<Int> = _savingsTargetPct.asStateFlow()

    private val _spendingTargetPct = MutableStateFlow(prefs.getInt("spending_target_pct", 50))
    val spendingTargetPct: StateFlow<Int> = _spendingTargetPct.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt("reminder_hour", 20))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt("reminder_minute", 30))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_name", "User") ?: "User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _autoBackupNight = MutableStateFlow(prefs.getBoolean("auto_backup_night", false))
    val autoBackupNight: StateFlow<Boolean> = _autoBackupNight.asStateFlow()

    private val _isMultiCurrencyEnabled = MutableStateFlow(prefs.getBoolean("multi_currency_enabled", false))
    val isMultiCurrencyEnabled: StateFlow<Boolean> = _isMultiCurrencyEnabled.asStateFlow()

    private val _primaryCurrency = MutableStateFlow(prefs.getString("primary_currency", "INR") ?: "INR")
    val primaryCurrency: StateFlow<String> = _primaryCurrency.asStateFlow()

    private val defaultRates = mapOf(
        "INR" to 1.0,
        "USD" to 83.5,
        "EUR" to 90.0,
        "GBP" to 106.0,
        "JPY" to 0.52,
        "AED" to 22.7,
        "AUD" to 55.0,
        "CAD" to 61.0,
        "SGD" to 62.0
    )

    val exchangeRatesMap: StateFlow<Map<String, Double>> = db.exchangeRateDao().getAllRatesFlow()
        .map { list ->
            val map = defaultRates.toMutableMap()
            list.forEach { rate ->
                map[rate.baseCurrency.uppercase()] = rate.rate
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultRates)

    fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val rates = exchangeRatesMap.value
        val fromUpper = fromCurrency.uppercase()
        val toUpper = toCurrency.uppercase()
        if (fromUpper == toUpper) return amount

        val rateFromToInr = rates[fromUpper] ?: defaultRates[fromUpper] ?: 1.0
        val rateToToInr = rates[toUpper] ?: defaultRates[toUpper] ?: 1.0

        val amountInInr = amount * rateFromToInr
        return amountInInr / rateToToInr
    }

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    private val _activeEnrichId = MutableStateFlow<Long?>(null)
    val activeEnrichId: StateFlow<Long?> = _activeEnrichId.asStateFlow()

    fun setActiveEnrichId(id: Long?) {
        _activeEnrichId.value = id
    }

    private val _onboardingState = MutableStateFlow<OnboardingState>(
        if (prefs.getBoolean("dpdp_consent_granted", false)) OnboardingState.Completed else OnboardingState.Required
    )
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    val allTransactions: StateFlow<List<TransactionWithCategory>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditCards: StateFlow<List<CreditCard>> = repository.allCreditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlannedLists: StateFlow<List<PlannedListWithItems>> = repository.allPlannedLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTransactions: StateFlow<List<TransactionWithCategory>> = repository.pendingTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<RecurringSchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timeboxFilter = MutableStateFlow(TimeboxFilter.MONTHLY)
    val timeboxFilter: StateFlow<TimeboxFilter> = _timeboxFilter.asStateFlow()

    val analyticsState: StateFlow<AnalyticsState> = combine(
        allTransactions,
        _timeboxFilter,
        _currentTimeMillis,
        _billingCycleStartDay
    ) { txns, filter, now, startDay ->
        calculateAnalytics(txns, filter, now, startDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    fun updateBillingCycleStartDay(day: Int) {
        val validDay = day.coerceIn(1, 30)
        _billingCycleStartDay.value = validDay
        prefs.edit().putInt("billing_cycle_start_day", validDay).apply()
    }

    val monthlyCategorySpends: StateFlow<Map<Long, Double>> = combine(
        allTransactions,
        _billingCycleStartDay,
        _currentTimeMillis,
        _primaryCurrency,
        exchangeRatesMap
    ) { txns, startDay, now, primCurr, rates ->
        val startOfCycle = getStartOfBillingCycleTimestamp(startDay, now)
        txns.filter {
            !it.transaction.isPending &&
            it.transaction.type == "DEBIT" &&
            it.transaction.timestamp >= startOfCycle
        }.groupBy { it.transaction.categoryId ?: -1L }
         .mapValues { entry ->
             entry.value.sumOf {
                 convertAmount(it.transaction.amount, it.transaction.currency, primCurr)
             }
         }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val forecastAllowance: StateFlow<ForecastAllowance> = combine(
        allTransactions,
        allSchedules,
        _monthlyIncome,
        _savingsTargetPct,
        _spendingTargetPct,
        _billingCycleStartDay,
        _currentTimeMillis,
        _primaryCurrency,
        exchangeRatesMap
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val txns = array[0] as List<TransactionWithCategory>
        @Suppress("UNCHECKED_CAST")
        val schedules = array[1] as List<RecurringSchedule>
        val income = array[2] as Double
        val savingsPct = array[3] as Int
        val spendingPct = array[4] as Int
        val startDay = array[5] as Int
        val now = array[6] as Long
        val primCurr = array[7] as String
        @Suppress("UNCHECKED_CAST")
        val rates = array[8] as Map<String, Double>

        if (income <= 0.0) {
            return@combine ForecastAllowance()
        }
        val startOfCycle = getStartOfBillingCycleTimestamp(startDay, now)
        val remainingDays = getRemainingDaysInBillingCycle(startDay, now)
        
        val discretionarySpent = txns.filter {
            !it.transaction.isPending &&
            it.transaction.type == "DEBIT" &&
            it.transaction.source != "RECURRING" &&
            it.transaction.timestamp >= startOfCycle
        }.sumOf { convertAmount(it.transaction.amount, it.transaction.currency, primCurr) }
        
        val targetSavings = income * (savingsPct / 100.0)
        val spendingCap = income * (spendingPct / 100.0)
        
        val rawFixedCommitments = schedules.filter { it.isActive }.sumOf { s ->
            val convertedAmt = convertAmount(s.amount, s.currency, primCurr)
            when (s.frequency.uppercase()) {
                "DAILY" -> convertedAmt * 30.0
                "WEEKLY" -> convertedAmt * 4.33
                "MONTHLY" -> convertedAmt
                "YEARLY" -> convertedAmt / 12.0
                else -> convertedAmt
            }
        }

        val realizedRecurring = txns.filter {
            !it.transaction.isPending &&
            it.transaction.type == "DEBIT" &&
            it.transaction.source == "RECURRING" &&
            it.transaction.timestamp >= startOfCycle
        }.sumOf { convertAmount(it.transaction.amount, it.transaction.currency, primCurr) }

        val fixedCommitments = (rawFixedCommitments - realizedRecurring).coerceAtLeast(0.0)
        val discretionarySpendingCap = (spendingCap - rawFixedCommitments).coerceAtLeast(0.0)
        val discretionaryLeft = spendingCap - rawFixedCommitments - discretionarySpent
        val daily = (discretionaryLeft / remainingDays).coerceAtLeast(0.0)
        val weekly = daily * 7.0
        
        ForecastAllowance(
            dailyAllowance = daily,
            weeklyAllowance = weekly,
            isOverspent = discretionaryLeft < 0.0,
            remainingDays = remainingDays,
            fixedCommitments = fixedCommitments,
            discretionarySpent = discretionarySpent,
            discretionarySpendingCap = discretionarySpendingCap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForecastAllowance())

    fun updateUserName(name: String) {
        val trimmed = name.trim()
        val finalName = if (trimmed.isBlank()) "User" else trimmed
        _userName.value = finalName
        prefs.edit().putString("user_name", finalName).apply()
    }

    fun updateBudgetGoals(income: Double, savingsPct: Int, spendingPct: Int) {
        _monthlyIncome.value = income
        _savingsTargetPct.value = savingsPct
        _spendingTargetPct.value = spendingPct
        prefs.edit()
            .putFloat("monthly_income", income.toFloat())
            .putInt("savings_target_pct", savingsPct)
            .putInt("spending_target_pct", spendingPct)
            .apply()
    }

    fun updateAutoBackupNight(enabled: Boolean) {
        _autoBackupNight.value = enabled
        prefs.edit().putBoolean("auto_backup_night", enabled).apply()
    }

    fun updateMultiCurrencyEnabled(enabled: Boolean) {
        _isMultiCurrencyEnabled.value = enabled
        prefs.edit().putBoolean("multi_currency_enabled", enabled).apply()
        if (enabled) {
            com.ankitsudegora.worker.ExchangeRateSyncWorker.scheduleDailySync(getApplication(), forceRestart = true)
        } else {
            com.ankitsudegora.worker.ExchangeRateSyncWorker.cancelDailySync(getApplication())
        }
    }

    fun updatePrimaryCurrency(currency: String) {
        _primaryCurrency.value = currency
        prefs.edit().putString("primary_currency", currency).apply()
    }

    init {
        // Core Bug Resolution: pass forceRestart = false on startup init tasks to preserve queue timers
        if (_onboardingState.value == OnboardingState.Completed) {
            setupWorkReminder(forceRestart = false)
            if (_isMultiCurrencyEnabled.value) {
                com.ankitsudegora.worker.ExchangeRateSyncWorker.scheduleDailySync(getApplication(), forceRestart = false)
            }
        }
        viewModelScope.launch {
            processRecurringSchedules()
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000)
                _currentTimeMillis.value = System.currentTimeMillis()
            }
        }
    }

    private suspend fun processRecurringSchedules() {
        val schedules = repository.getActiveSchedules()
        val now = System.currentTimeMillis()
        schedules.forEach { s ->
            if (s.nextTriggerTime <= 0) {
                val nextTime = calculateNextTriggerTime(s.frequency, now)
                val updatedSchedule = s.copy(lastTriggered = now, nextTriggerTime = nextTime)
                repository.updateSchedule(updatedSchedule)
            } else if (now >= s.nextTriggerTime) {
                val transaction = Transaction(
                    amount = s.amount,
                    type = s.type,
                    merchant = s.merchant,
                    categoryId = s.categoryId,
                    notes = s.notes ?: "Scheduled payment auto-trigger",
                    timestamp = s.nextTriggerTime,
                    paymentMethod = s.paymentMethod,
                    isPending = true,
                    source = "RECURRING",
                    subCategory = s.subCategory,
                    currency = s.currency
                )
                repository.insertTransaction(transaction)

                val (prevTime, nextTime) = advanceTriggerTimeToFuture(s.frequency, s.nextTriggerTime, now)
                val updatedSchedule = s.copy(lastTriggered = prevTime, nextTriggerTime = nextTime)
                repository.updateSchedule(updatedSchedule)
            }
        }
    }

    private fun advanceTriggerTimeToFuture(frequency: String, nextTriggerTime: Long, now: Long): Pair<Long, Long> {
        val field = when (frequency.uppercase()) {
            "DAILY" -> Calendar.DAY_OF_YEAR
            "WEEKLY" -> Calendar.WEEK_OF_YEAR
            "MONTHLY" -> Calendar.MONTH
            "YEARLY" -> Calendar.YEAR
            else -> Calendar.MONTH
        }
        val approxUnitMs = when (frequency.uppercase()) {
            "DAILY" -> 24 * 3600 * 1000L
            "WEEKLY" -> 7 * 24 * 3600 * 1000L
            "MONTHLY" -> 30 * 24 * 3600 * 1000L
            "YEARLY" -> 365 * 24 * 3600 * 1000L
            else -> 30 * 24 * 3600 * 1000L
        }
        
        val diff = now - nextTriggerTime
        var n = (diff / approxUnitMs).toInt()
        if (n < 1) {
            n = 1
        }
        
        fun getTimeAfterSteps(steps: Int): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = nextTriggerTime
            cal.add(field, steps)
            return cal.timeInMillis
        }

        var nextTime = getTimeAfterSteps(n)
        while (nextTime <= now) {
            n++
            nextTime = getTimeAfterSteps(n)
        }
        
        while (n > 1) {
            val prevTimeCandidate = getTimeAfterSteps(n - 1)
            if (prevTimeCandidate > now) {
                n--
                nextTime = prevTimeCandidate
            } else {
                break
            }
        }
        
        val prevTime = if (n == 1) {
            nextTriggerTime
        } else {
            getTimeAfterSteps(n - 1)
        }
        
        return Pair(prevTime, nextTime)
    }

    private fun calculateNextTriggerTime(frequency: String, fromTime: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = fromTime
        when (frequency.uppercase()) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun completeOnboarding(consent: Boolean) {
        prefs.edit().putBoolean("dpdp_consent_granted", consent).apply()
        _onboardingState.value = if (consent) OnboardingState.Completed else OnboardingState.Required
        if (consent) {
            setupWorkReminder(forceRestart = true)
        }
    }

    fun resetOnboarding() {
        prefs.edit().clear().apply()
        _userName.value = "User"
        _monthlyIncome.value = 0.0
        _savingsTargetPct.value = 20
        _spendingTargetPct.value = 50
        _reminderHour.value = 20
        _reminderMinute.value = 30
        _autoBackupNight.value = false
        _billingCycleStartDay.value = 1
        _onboardingState.value = OnboardingState.Required
        ReminderWorker.cancelAllReminders(getApplication())
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
            val defaultCategories = listOf(
                Category(name = "Food & Dining", iconResName = "restaurant"),
                Category(name = "Groceries", iconResName = "shopping_cart"),
                Category(name = "Rent & Maintenance", iconResName = "home"),
                Category(name = "Fuel & Travel", iconResName = "directions_car"),
                Category(name = "Shopping", iconResName = "shopping_bag"),
                Category(name = "Bills & Utilities", iconResName = "receipt_long"),
                Category(name = "Entertainment", iconResName = "movie"),
                Category(name = "Health & Medical", iconResName = "medical_services"),
                Category(name = "EMI & Loans", iconResName = "account_balance"),
                Category(name = "Others", iconResName = "category"),
                Category(name = "Salary", iconResName = "payments"),
                Category(name = "Refund", iconResName = "restore"),
                Category(name = "Interest", iconResName = "trending_up"),
                Category(name = "Other Inflow", iconResName = "savings")
            )
            defaultCategories.forEach {
                repository.insertCategory(it)
            }
        }
    }

    fun setTimeboxFilter(filter: TimeboxFilter) {
        _timeboxFilter.value = filter
    }

    fun updateCategoryBudget(category: Category, budgetLimit: Double?) {
        viewModelScope.launch {
            repository.updateCategory(category.copy(budgetLimit = budgetLimit))
        }
    }

    fun addRecurringSchedule(
        amount: Double,
        type: String,
        merchant: String,
        categoryId: Long?,
        notes: String?,
        paymentMethod: String,
        frequency: String,
        subCategory: String? = null,
        currency: String = "INR"
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextTime = calculateNextTriggerTime(frequency, now)
            val schedule = RecurringSchedule(
                amount = amount,
                type = type,
                merchant = merchant,
                categoryId = categoryId,
                notes = notes,
                paymentMethod = paymentMethod,
                frequency = frequency.uppercase(),
                lastTriggered = now,
                nextTriggerTime = nextTime,
                isActive = true,
                subCategory = subCategory,
                currency = currency
            )
            repository.insertSchedule(schedule)
            processRecurringSchedules()
        }
    }

    fun deleteRecurringSchedule(schedule: RecurringSchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    fun toggleRecurringSchedule(schedule: RecurringSchedule) {
        viewModelScope.launch {
            repository.updateSchedule(schedule.copy(isActive = !schedule.isActive))
            processRecurringSchedules()
        }
    }

    fun addManualTransaction(
        amount: Double,
        type: String,
        merchant: String,
        categoryId: Long?,
        notes: String?,
        paymentMethod: String,
        timestamp: Long = System.currentTimeMillis(),
        recurringFrequency: String? = null,
        subCategory: String? = null,
        paidViaCcId: Long? = null,
        repaidCcId: Long? = null,
        selectedRepaidTxnIds: List<Long> = emptyList(),
        currency: String = "INR"
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                type = type,
                merchant = merchant.ifBlank { "Cash Expense" },
                categoryId = categoryId,
                notes = notes,
                timestamp = timestamp,
                paymentMethod = paymentMethod,
                isPending = false,
                source = if (!recurringFrequency.isNullOrEmpty()) "RECURRING" else "MANUAL",
                subCategory = subCategory,
                paidViaCcId = paidViaCcId,
                currency = currency
            )
            val repaymentTxnId = repository.insertTransaction(transaction)

            if (repaidCcId != null && selectedRepaidTxnIds.isNotEmpty()) {
                repository.repayCreditCardTransactionsAtomically(repaymentTxnId, amount, selectedRepaidTxnIds)
            }

            if (!recurringFrequency.isNullOrEmpty()) {
                addRecurringSchedule(
                    amount = amount,
                    type = type,
                    merchant = merchant.ifBlank { "Cash Expense" },
                    categoryId = categoryId,
                    notes = notes,
                    paymentMethod = paymentMethod,
                    frequency = recurringFrequency,
                    subCategory = subCategory,
                    currency = currency
                )
            }

            if (categoryId != null && type == "DEBIT") {
                checkBudgetThresholds(categoryId)
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            if (transaction.categoryId != null && transaction.type == "DEBIT" && !transaction.isPending) {
                checkBudgetThresholds(transaction.categoryId)
            }
        }
    }

    fun finalizeSmsTransaction(
        id: Long,
        categoryId: Long,
        notes: String?,
        amount: Double,
        merchant: String,
        type: String,
        recurringFrequency: String? = null,
        subCategory: String? = null,
        paidViaCcId: Long? = null,
        repaidCcId: Long? = null,
        selectedRepaidTxnIds: List<Long> = emptyList(),
        linkedListId: Long? = null,
        refundedTxnId: Long? = null
    ) {
        viewModelScope.launch {
            val existing = repository.getTransactionById(id)
            if (existing != null) {
                // If linking to a list, delete any old duplicate manual checkout transaction
                if (linkedListId != null) {
                    val oldManualTxn = repository.getTransactionByLinkedListId(linkedListId)
                    if (oldManualTxn != null && oldManualTxn.id != id) {
                        repository.deleteTransaction(oldManualTxn)
                    }
                    // Also mark the planned list as completed if it wasn't
                    val plannedListWithItems = repository.getPlannedListWithItemsById(linkedListId)
                    if (plannedListWithItems != null && plannedListWithItems.plannedList.status != "COMPLETED") {
                        repository.updatePlannedList(plannedListWithItems.plannedList.copy(status = "COMPLETED"))
                    }
                }

                val updated = existing.copy(
                    categoryId = categoryId,
                    notes = notes,
                    amount = amount,
                    merchant = merchant.ifBlank { existing.merchant },
                    type = type,
                    isPending = false,
                    source = if (!recurringFrequency.isNullOrEmpty()) "RECURRING" else if (existing.source == "RECURRING") "MANUAL" else existing.source,
                    subCategory = subCategory,
                    paidViaCcId = paidViaCcId,
                    linkedListId = linkedListId ?: existing.linkedListId,
                    refundedTxnId = refundedTxnId ?: existing.refundedTxnId
                )
                repository.updateTransaction(updated)

                if (repaidCcId != null && selectedRepaidTxnIds.isNotEmpty()) {
                    repository.repayCreditCardTransactionsAtomically(id, amount, selectedRepaidTxnIds)
                }

                if (!recurringFrequency.isNullOrEmpty()) {
                    addRecurringSchedule(
                        amount = amount,
                        type = type,
                        merchant = merchant.ifBlank { existing.merchant },
                        categoryId = categoryId,
                        notes = notes,
                        paymentMethod = existing.paymentMethod,
                        frequency = recurringFrequency,
                        subCategory = subCategory
                      )
                }

                if (type == "DEBIT") {
                    checkBudgetThresholds(categoryId)
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return repository.getTransactionById(id)
    }

    fun simulateSmsTransaction(body: String) {
        viewModelScope.launch {
            val bodyLower = body.lowercase()
            val isDebit = bodyLower.contains("debited") || bodyLower.contains("withdrawn") || bodyLower.contains("spent") || bodyLower.contains("paid") || bodyLower.contains("sent")
            val type = if (isDebit) "DEBIT" else "CREDIT"
            
            val amountRegex = Regex("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.find(body)
            val amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "")
            val amount = amountStr?.toDoubleOrNull() ?: 250.0

            val merchantRegex = Regex("(?:at|to|from|vpa|into|thru)\\s+([a-zA-Z0-9]{3,20})", RegexOption.IGNORE_CASE)
            val merchantMatch = merchantRegex.find(body)
            var merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Simulated Merchant"
            if (merchant.isBlank() || merchant.all { it.isDigit() }) {
                merchant = "Simulated Merchant"
            } else {
                merchant = merchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            val transaction = Transaction(
                amount = amount,
                type = type,
                merchant = merchant,
                paymentMethod = if (type == "DEBIT") "UPI" else "NETBANKING",
                isPending = true,
                source = "NOTIFICATION",
                smsSenderId = "ALERT-SIMULATED",
                timestamp = System.currentTimeMillis()
            )
            val id = repository.insertTransaction(transaction)

            val context = getApplication<Application>()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "kharchadekh_notifications"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Transaction Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val intent = android.content.Intent(context, com.ankitsudegora.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_TRANSACTION_ID", id)
            }

            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = android.app.PendingIntent.getActivity(context, id.toInt(), intent, flags)
            val emoji = if (type == "DEBIT") "💸" else "🏦"
            val label = if (type == "DEBIT") "Spent" else "Received"

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Simulated Alert Auto-Parsed")
                .setContentText("$label ₹$amount at $merchant? Tap to categorize it. $emoji")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(id.toInt(), notification)
        }
    }

    fun addCustomCategory(name: String, iconResName: String = "category") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val category = Category(
                    name = name.trim(),
                    iconResName = iconResName,
                    isCustom = true
                )
                repository.insertCategory(category)
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        prefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
        // Force work update cleanly when changing user runtime properties
        setupWorkReminder(forceRestart = true)
    }

    private fun setupWorkReminder(forceRestart: Boolean = false) {
        ReminderWorker.scheduleAllReminders(
            getApplication(),
            _reminderHour.value,
            _reminderMinute.value,
            forceRestart
        )
    }

    private fun calculateAnalytics(
        txns: List<TransactionWithCategory>,
        filter: TimeboxFilter,
        now: Long,
        startDay: Int
    ): AnalyticsState {
        val calendar = Calendar.getInstance()

        val filteredTxns = txns.filter { item ->
            if (item.transaction.isPending) return@filter false

            val itemTime = item.transaction.timestamp
            calendar.timeInMillis = now
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

            calendar.timeInMillis = itemTime
            val itemYear = calendar.get(Calendar.YEAR)
            val itemMonth = calendar.get(Calendar.MONTH)
            val itemDay = calendar.get(Calendar.DAY_OF_YEAR)

            when (filter) {
                TimeboxFilter.DAILY -> {
                    currentYear == itemYear && currentDay == itemDay
                }
                TimeboxFilter.WEEKLY -> {
                    now - itemTime <= 7L * 24 * 60 * 60 * 1000
                }
                TimeboxFilter.MONTHLY -> {
                    val startOfCycle = getStartOfBillingCycleTimestamp(startDay, now)
                    val endOfCycle = getStartOfNextBillingCycleTimestamp(startDay, now)
                    itemTime in startOfCycle until endOfCycle
                }
                TimeboxFilter.YEARLY -> {
                    currentYear == itemYear
                }
            }
        }

        var totalExpense = 0.0
        var totalDebit = 0.0
        var totalCredit = 0.0
        val categoryMap = mutableMapOf<Category, Double>()

        filteredTxns.forEach { item ->
            val amount = item.transaction.amount
            if (item.transaction.type == "DEBIT") {
                totalDebit += amount
                if (item.category?.name != "CreditCard Payment") {
                    totalExpense += amount

                    val cat = item.category ?: Category(name = "Uncategorized", iconResName = "category")
                    categoryMap[cat] = categoryMap.getOrDefault(cat, 0.0) + amount
                }
            } else {
                totalCredit += amount
            }
        }

        val breakdown = categoryMap.map { (cat, amount) ->
            CategoryUsage(
                category = cat,
                amount = amount,
                percentage = if (totalExpense > 0) ((amount / totalExpense) * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }

        return AnalyticsState(
            totalExpense = totalExpense,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            categoryBreakdown = breakdown
        )
    }

    private suspend fun checkBudgetThresholds(categoryId: Long) {
        val category = repository.getCategoryById(categoryId) ?: return
        val limit = category.budgetLimit ?: return
        if (limit <= 0) return

        val now = System.currentTimeMillis()
        val since = getStartOfBillingCycleTimestamp(_billingCycleStartDay.value, now)
        val spent = repository.getCategorySpentSince(categoryId, since) ?: 0.0

        val key80 = "budget_alert_80_${categoryId}_cycle_$since"
        val key100 = "budget_alert_100_${categoryId}_cycle_$since"

        val alreadyNotified80 = prefs.getBoolean(key80, false)
        val alreadyNotified100 = prefs.getBoolean(key100, false)

        if (spent >= limit && !alreadyNotified100) {
            prefs.edit().putBoolean(key100, true).putBoolean(key80, true).apply()
            ReminderWorker.triggerBudgetNotification(getApplication(), category.name, spent, limit, isExceeded = true)
        } else if (spent >= limit * 0.8 && spent < limit && !alreadyNotified80) {
            prefs.edit().putBoolean(key80, true).apply()
            ReminderWorker.triggerBudgetNotification(getApplication(), category.name, spent, limit, isExceeded = false)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun getStartOfBillingCycleTimestamp(startDay: Int, now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (currentDay >= startDay) {
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDays))
        } else {
            calendar.add(Calendar.MONTH, -1)
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDays))
        }
        return calendar.timeInMillis
    }

    fun getStartOfNextBillingCycleTimestamp(startDay: Int, now: Long = System.currentTimeMillis()): Long {
        val startOfCurrent = getStartOfBillingCycleTimestamp(startDay, now)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startOfCurrent
        calendar.add(Calendar.MONTH, 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDays))
        return calendar.timeInMillis
    }

    fun getRemainingDaysInBillingCycle(startDay: Int, now: Long = System.currentTimeMillis()): Int {
        val nextCycle = Calendar.getInstance()
        nextCycle.timeInMillis = now
        val currentDay = nextCycle.get(Calendar.DAY_OF_MONTH)
        if (currentDay >= startDay) {
            nextCycle.add(Calendar.MONTH, 1)
        }
        val maxDays = nextCycle.getActualMaximum(Calendar.DAY_OF_MONTH)
        nextCycle.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDays))
        nextCycle.set(Calendar.HOUR_OF_DAY, 0)
        nextCycle.set(Calendar.MINUTE, 0)
        nextCycle.set(Calendar.SECOND, 0)
        nextCycle.set(Calendar.MILLISECOND, 0)
        
        val todayStart = Calendar.getInstance()
        todayStart.timeInMillis = now
        todayStart.set(Calendar.HOUR_OF_DAY, 0)
        todayStart.set(Calendar.MINUTE, 0)
                val diffMs = nextCycle.timeInMillis - todayStart.timeInMillis
        val days = (diffMs / (24L * 60 * 60 * 1000)).toInt()
        return days.coerceAtLeast(1)
    }

    // --- Credit Card Operations ---

    fun addCreditCard(cardName: String) {
        viewModelScope.launch {
            repository.insertCreditCard(CreditCard(cardName = cardName))
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
        }
    }

    // --- Planned Lists & Items Operations ---

    fun addPlannedList(name: String, budgetCap: Double? = null, categoryId: Long? = null) {
        viewModelScope.launch {
            repository.insertPlannedList(PlannedList(name = name, budgetCap = budgetCap, categoryId = categoryId))
        }
    }

    fun deletePlannedList(list: PlannedList) {
        viewModelScope.launch {
            repository.deletePlannedList(list)
        }
    }

    fun duplicatePlannedList(listWithItems: PlannedListWithItems, newName: String) {
        viewModelScope.launch {
            val newListId = repository.insertPlannedList(
                PlannedList(
                    name = newName,
                    budgetCap = listWithItems.plannedList.budgetCap,
                    status = "DRAFT",
                    categoryId = listWithItems.plannedList.categoryId
                )
            )
            listWithItems.items.forEach { item ->
                repository.insertPlannedItem(
                    PlannedItem(
                        listId = newListId,
                        name = item.name,
                        quantity = item.quantity,
                        price = item.price,
                        isChecked = false
                    )
                )
            }
        }
    }

    fun addPlannedItem(listId: Long, name: String, quantity: Int = 1, price: Double = 0.0) {
        viewModelScope.launch {
            repository.insertPlannedItem(
                PlannedItem(
                    listId = listId,
                    name = name,
                    quantity = quantity,
                    price = price,
                    isChecked = false
                )
            )
        }
    }

    fun updatePlannedItem(item: PlannedItem) {
        viewModelScope.launch {
            repository.updatePlannedItem(item)
        }
    }

    fun deletePlannedItem(item: PlannedItem) {
        viewModelScope.launch {
            repository.deletePlannedItem(item)
        }
    }

    fun togglePlannedItemChecked(item: PlannedItem) {
        viewModelScope.launch {
            repository.updatePlannedItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun markPlannedListAsPaid(
        listWithItems: PlannedListWithItems,
        paymentMethod: String,
        categoryId: Long?,
        carryUnboughtToNewList: Boolean,
        linkedPendingTxnId: Long? = null
    ) {
        viewModelScope.launch {
            val checkedItems = listWithItems.items.filter { it.isChecked }
            val total = checkedItems.sumOf { it.price * it.quantity }

            if (linkedPendingTxnId != null) {
                val pendingTxn = repository.getTransactionById(linkedPendingTxnId)
                if (pendingTxn != null) {
                    val updated = pendingTxn.copy(
                        isPending = false,
                        linkedListId = listWithItems.plannedList.id,
                        categoryId = categoryId ?: pendingTxn.categoryId,
                        merchant = listWithItems.plannedList.name.ifBlank { pendingTxn.merchant },
                        notes = "Settled Planned List: ${listWithItems.plannedList.name}",
                        paymentMethod = paymentMethod
                    )
                    repository.updateTransaction(updated)
                    if (categoryId != null) {
                        checkBudgetThresholds(categoryId)
                    }
                }
            } else if (total > 0.0) {
                val transaction = Transaction(
                    amount = total,
                    type = "DEBIT",
                    merchant = listWithItems.plannedList.name.ifBlank { "Groceries" },
                    categoryId = categoryId,
                    notes = "Settled Planned List: ${listWithItems.plannedList.name}",
                    timestamp = System.currentTimeMillis(),
                    paymentMethod = paymentMethod,
                    source = "MANUAL",
                    linkedListId = listWithItems.plannedList.id,
                    currency = primaryCurrency.value
                )
                repository.insertTransaction(transaction)
                if (categoryId != null) {
                    checkBudgetThresholds(categoryId)
                }
            }

            // Update status to COMPLETED
            repository.updatePlannedList(listWithItems.plannedList.copy(status = "COMPLETED"))

            // Carry forward unbought items
            if (carryUnboughtToNewList) {
                val unboughtItems = listWithItems.items.filter { !it.isChecked }
                if (unboughtItems.isNotEmpty()) {
                    val newListId = repository.insertPlannedList(
                        PlannedList(
                            name = "${listWithItems.plannedList.name} (Carry Forward)",
                            budgetCap = listWithItems.plannedList.budgetCap,
                            status = "DRAFT",
                            categoryId = listWithItems.plannedList.categoryId
                        )
                    )
                    unboughtItems.forEach { item ->
                        repository.insertPlannedItem(
                            PlannedItem(
                                listId = newListId,
                                name = item.name,
                                quantity = item.quantity,
                                price = item.price,
                                isChecked = false
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun getTransactionByLinkedListId(listId: Long): Transaction? {
        return repository.getTransactionByLinkedListId(listId)
    }

    suspend fun getTransactionsByLinkedListId(listId: Long): List<Transaction> {
        return repository.getTransactionsByLinkedListId(listId)
    }

    fun updateTransactionAmount(transactionId: Long, newAmount: Double) {
        viewModelScope.launch {
            val transaction = repository.getTransactionById(transactionId)
            if (transaction != null) {
                repository.updateTransaction(transaction.copy(amount = newAmount))
            }
        }
    }

    fun updateTransactionAmountByLinkedListId(listId: Long, newAmount: Double) {
        viewModelScope.launch {
            val transaction = repository.getTransactionByLinkedListId(listId)
            if (transaction != null) {
                repository.updateTransaction(transaction.copy(amount = newAmount))
            } else {
                val listWithItems = repository.getPlannedListWithItemsById(listId)
                if (listWithItems != null) {
                    val newTxn = Transaction(
                        amount = newAmount,
                        type = "DEBIT",
                        merchant = listWithItems.plannedList.name.ifBlank { "Groceries" },
                        categoryId = listWithItems.plannedList.categoryId,
                        notes = "Settled Planned List: ${listWithItems.plannedList.name}",
                        timestamp = System.currentTimeMillis(),
                        paymentMethod = "UPI",
                        source = "MANUAL",
                        linkedListId = listId,
                        currency = primaryCurrency.value
                    )
                    repository.insertTransaction(newTxn)
                }
            }
        }
    }

    // --- Price Estimate & Credit Card Repayment Operations ---

    suspend fun getLastPriceForItem(name: String): Double? {
        return repository.getLastPriceForItem(name)
    }

    fun repayCreditCardBill(card: CreditCard, amount: Double, selectedTxnIds: List<Long>) {
        viewModelScope.launch {
            val categoriesList = repository.getAllCategoriesList()
            val ccPaymentCategory = categoriesList.find { it.name == "CreditCard Payment" }
            val transaction = Transaction(
                amount = amount,
                type = "DEBIT",
                merchant = "Repayment: ${card.cardName}",
                categoryId = ccPaymentCategory?.id,
                notes = "Settle Credit Card: ${card.cardName} (${selectedTxnIds.size} transactions)",
                timestamp = System.currentTimeMillis(),
                paymentMethod = "UPI",
                source = "MANUAL",
                ccRepaymentId = null,
                currency = primaryCurrency.value
            )
            val repaymentId = repository.insertTransaction(transaction)
            if (selectedTxnIds.isNotEmpty()) {
                repository.repayCreditCardTransactionsAtomically(repaymentId, amount, selectedTxnIds)
            }
        }
    }

    fun unlinkTransactionFromPlannedList(txnId: Long) {
        viewModelScope.launch {
            val transaction = repository.getTransactionById(txnId)
            if (transaction != null) {
                repository.updateTransaction(transaction.copy(linkedListId = null))
            }
        }
    }

    fun linkTransactionToPlannedList(txnId: Long, listId: Long) {
        viewModelScope.launch {
            val transaction = repository.getTransactionById(txnId)
            if (transaction != null) {
                repository.updateTransaction(transaction.copy(linkedListId = listId, isPending = false))
            }
        }
    }
}
