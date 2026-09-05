package com.ankitsudegora.viewmodel

import com.ankitsudegora.data.Category
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.data.TransactionWithCategory

/**
 * Immutable MVI UI State for Activity Screen.
 */
data class ActivityUiState(
    val primaryCurrency: String = "INR",
    val billingCycleStartDay: Int = 1,
    val monthlyBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val selectedTypeFilter: String = "ALL", // ALL, DEBIT, CREDIT, TRANSFER, REFUND
    val transactions: List<TransactionWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Sealed MVI UI Actions for Activity Screen.
 */
sealed interface ActivityUiAction {
    data class OnFilterTypeSelected(val type: String) : ActivityUiAction
    data class OnDeleteTransaction(val transaction: Transaction) : ActivityUiAction
    data class OnEnrichTransaction(val transactionId: Long) : ActivityUiAction
    data object OnExportCsv : ActivityUiAction
    data object OnExportPdf : ActivityUiAction
}
