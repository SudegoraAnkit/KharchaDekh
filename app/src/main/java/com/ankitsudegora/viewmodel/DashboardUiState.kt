package com.ankitsudegora.viewmodel

import com.ankitsudegora.data.Category
import com.ankitsudegora.data.TransactionWithCategory

/**
 * Immutable MVI UI State for Dashboard Screen.
 */
data class DashboardUiState(
    val userName: String = "User",
    val primaryCurrency: String = "INR",
    val monthlyIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val spendingTargetPct: Int = 50,
    val savingsTargetPct: Int = 20,
    val selectedFilter: TimeboxFilter = TimeboxFilter.MONTHLY,
    val pendingTransactions: List<TransactionWithCategory> = emptyList(),
    val currentCycleTransactions: List<TransactionWithCategory> = emptyList(),
    val previousCycleTransactions: List<TransactionWithCategory> = emptyList(),
    val categoryBreakdown: List<CategoryUsage> = emptyList(),
    val forecastAllowance: ForecastAllowance = ForecastAllowance(),
    val isLoading: Boolean = false
)

/**
 * Sealed MVI UI Actions for Dashboard Screen.
 */
sealed interface DashboardUiAction {
    data class OnFilterChange(val filter: TimeboxFilter) : DashboardUiAction
    data class OnEnrichTransaction(val txnId: Long) : DashboardUiAction
    data class OnDeleteTransaction(val txnId: Long) : DashboardUiAction
    data class OnUpdateIncome(val income: Double) : DashboardUiAction
}
