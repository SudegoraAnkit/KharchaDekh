package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.worker.ReminderWorker
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

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ExpenseRepository(db.transactionDao(), db.categoryDao(), db.recurringScheduleDao())
    private val prefs = application.getSharedPreferences("kharchadekh_prefs", Context.MODE_PRIVATE)

    // Onboarding DPDP consent state
    private val _onboardingState = MutableStateFlow<OnboardingState>(
        if (prefs.getBoolean("dpdp_consent_granted", false)) OnboardingState.Completed else OnboardingState.Required
    )
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    // Active transactions & categories
    val allTransactions: StateFlow<List<TransactionWithCategory>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTransactions: StateFlow<List<TransactionWithCategory>> = repository.pendingTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<RecurringSchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for filter tab selection
    private val _timeboxFilter = MutableStateFlow(TimeboxFilter.MONTHLY)
    val timeboxFilter: StateFlow<TimeboxFilter> = _timeboxFilter.asStateFlow()

    // Analytics state calculated reactively based on transactions and selected filter
    val analyticsState: StateFlow<AnalyticsState> = combine(
        allTransactions,
        _timeboxFilter
    ) { txns, filter ->
        calculateAnalytics(txns, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    // Reminder configs
    private val _reminderHour = MutableStateFlow(prefs.getInt("reminder_hour", 20))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt("reminder_minute", 30))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    init {
        // Schedule daily reminders as per saved time on init
        if (_onboardingState.value == OnboardingState.Completed) {
            setupWorkReminder()
        }
        viewModelScope.launch {
            processRecurringSchedules()
        }
    }

    private suspend fun processRecurringSchedules() {
        val schedules = repository.getActiveSchedules()
        val now = System.currentTimeMillis()
        schedules.forEach { s ->
            var nextTime = s.nextTriggerTime
            var updatedSchedule = s
            while (now >= nextTime) {
                val transaction = Transaction(
                    amount = s.amount,
                    type = s.type,
                    merchant = s.merchant,
                    categoryId = s.categoryId,
                    notes = s.notes ?: "Scheduled payment auto-trigger",
                    timestamp = nextTime,
                    paymentMethod = s.paymentMethod,
                    isPending = true,  // Requires manual review
                    source = "RECURRING"
                )
                repository.insertTransaction(transaction)

                val prevTime = nextTime
                nextTime = calculateNextTriggerTime(s.frequency, prevTime)
                updatedSchedule = updatedSchedule.copy(lastTriggered = prevTime, nextTriggerTime = nextTime)
            }
            if (updatedSchedule != s) {
                repository.updateSchedule(updatedSchedule)
            }
        }
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
            setupWorkReminder()
        }
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("dpdp_consent_granted", false).apply()
        _onboardingState.value = OnboardingState.Required
        ReminderWorker.cancelReminder(getApplication())
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
        frequency: String
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
                isActive = true
            )
            repository.insertSchedule(schedule)
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
        recurringFrequency: String? = null
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
                isPending = false, // MANUAL log is never pending
                source = "MANUAL"
            )
            repository.insertTransaction(transaction)

            if (!recurringFrequency.isNullOrEmpty()) {
                addRecurringSchedule(
                    amount = amount,
                    type = type,
                    merchant = merchant.ifBlank { "Cash Expense" },
                    categoryId = categoryId,
                    notes = notes,
                    paymentMethod = paymentMethod,
                    frequency = recurringFrequency
                )
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun finalizeSmsTransaction(
        id: Long,
        categoryId: Long,
        notes: String?,
        amount: Double,
        merchant: String,
        type: String,
        recurringFrequency: String? = null
    ) {
        viewModelScope.launch {
            val existing = repository.getTransactionById(id)
            if (existing != null) {
                val updated = existing.copy(
                    categoryId = categoryId,
                    notes = notes,
                    amount = amount,
                    merchant = merchant.ifBlank { existing.merchant },
                    type = type,
                    isPending = false
                )
                repository.updateTransaction(updated)

                if (!recurringFrequency.isNullOrEmpty()) {
                    addRecurringSchedule(
                        amount = amount,
                        type = type,
                        merchant = merchant.ifBlank { existing.merchant },
                        categoryId = categoryId,
                        notes = notes,
                        paymentMethod = existing.paymentMethod,
                        frequency = recurringFrequency
                    )
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
            val isCredit = bodyLower.contains("credited") || bodyLower.contains("received") || bodyLower.contains("added")
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
                source = "SMS",
                smsSenderId = "TM-SIMULATED",
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

            val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
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
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Simulated SMS Auto-Parsed")
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
        setupWorkReminder()
    }

    private fun setupWorkReminder() {
        ReminderWorker.scheduleDailyReminder(
            getApplication(),
            _reminderHour.value,
            _reminderMinute.value
        )
    }

    private fun calculateAnalytics(
        txns: List<TransactionWithCategory>,
        filter: TimeboxFilter
    ): AnalyticsState {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        val filteredTxns = txns.filter { item ->
            // Exclude pending transactions from solid stats if desired (or keep them). 
            // In typical expense tracking, we exclude unresolved ones or include them. 
            // Let's include everything that has been finalized, or calculate for all finalized transactions.
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
                    // Check if it's within the preceding 7 days
                    now - itemTime <= 7L * 24 * 60 * 60 * 1000
                }
                TimeboxFilter.MONTHLY -> {
                    currentYear == itemYear && currentMonth == itemMonth
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
                totalExpense += amount // Expense is strictly Debits

                val cat = item.category ?: Category(name = "Uncategorized", iconResName = "category")
                categoryMap[cat] = categoryMap.getOrDefault(cat, 0.0) + amount
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

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
