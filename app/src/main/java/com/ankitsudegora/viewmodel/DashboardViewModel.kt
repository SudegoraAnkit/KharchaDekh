package com.ankitsudegora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsudegora.data.ITransactionRepository
import com.ankitsudegora.data.ICategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                transactionRepository.allTransactions,
                transactionRepository.pendingTransactions,
                categoryRepository.allCategories
            ) { allTxns, pendingTxns, categories ->
                val nonPending = allTxns.filter { !it.transaction.isPending }
                val totalSpent = nonPending
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && it.category?.name != "CreditCard Payment" }
                    .sumOf { it.transaction.amount }

                _uiState.update { current ->
                    current.copy(
                        totalExpense = totalSpent,
                        pendingTransactions = pendingTxns,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onAction(action: DashboardUiAction) {
        when (action) {
            is DashboardUiAction.OnFilterChange -> {
                _uiState.update { it.copy(selectedFilter = action.filter) }
            }
            is DashboardUiAction.OnDeleteTransaction -> {
                viewModelScope.launch {
                    val txn = transactionRepository.getTransactionById(action.txnId)
                    if (txn != null) {
                        transactionRepository.deleteTransaction(txn)
                    }
                }
            }
            is DashboardUiAction.OnUpdateIncome -> {
                _uiState.update { it.copy(monthlyIncome = action.income) }
            }
            is DashboardUiAction.OnEnrichTransaction -> {
                // Handled via navigation
            }
        }
    }
}
