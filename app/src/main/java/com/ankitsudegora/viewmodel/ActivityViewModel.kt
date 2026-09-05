package com.ankitsudegora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsudegora.data.ITransactionRepository
import com.ankitsudegora.data.ICategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActivityViewModel(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        loadActivityData()
    }

    private fun loadActivityData() {
        viewModelScope.launch {
            combine(
                transactionRepository.allTransactions,
                categoryRepository.allCategories
            ) { allTxns, categories ->
                val nonPending = allTxns.filter { !it.transaction.isPending }
                val totalSpent = nonPending
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && it.category?.name != "CreditCard Payment" }
                    .sumOf { it.transaction.amount }

                val totalIncome = nonPending
                    .filter { it.transaction.type.equals("CREDIT", ignoreCase = true) }
                    .sumOf { it.transaction.amount }

                _uiState.update { current ->
                    current.copy(
                        transactions = allTxns,
                        categories = categories,
                        totalSpent = totalSpent,
                        totalIncome = totalIncome,
                        netBalance = totalIncome - totalSpent,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onAction(action: ActivityUiAction) {
        when (action) {
            is ActivityUiAction.OnFilterTypeSelected -> {
                _uiState.update { it.copy(selectedTypeFilter = action.type) }
            }
            is ActivityUiAction.OnDeleteTransaction -> {
                viewModelScope.launch {
                    transactionRepository.deleteTransaction(action.transaction)
                }
            }
            is ActivityUiAction.OnEnrichTransaction -> {
                // Handled by navigation
            }
            is ActivityUiAction.OnExportCsv -> {
                // Handled by exporter
            }
            is ActivityUiAction.OnExportPdf -> {
                // Handled by exporter
            }
        }
    }
}
