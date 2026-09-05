package com.ankitsudegora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.ICreditCardRepository
import com.ankitsudegora.data.ITransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CreditCardViewModel(
    private val creditCardRepository: ICreditCardRepository,
    private val transactionRepository: ITransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardUiState())
    val uiState: StateFlow<CreditCardUiState> = _uiState.asStateFlow()

    init {
        loadCreditCardData()
    }

    private fun loadCreditCardData() {
        viewModelScope.launch {
            combine(
                creditCardRepository.allCreditCards,
                transactionRepository.allTransactions
            ) { cards, txns ->
                val unbilled = txns.filter {
                    it.transaction.paidViaCcId != null && it.transaction.ccRepaymentId == null && !it.transaction.isPending
                }
                val totalOut = unbilled.sumOf { it.transaction.amount }

                _uiState.update { current ->
                    current.copy(
                        cards = cards,
                        unbilledTransactions = unbilled,
                        totalOutstanding = totalOut,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onAction(action: CreditCardUiAction) {
        when (action) {
            is CreditCardUiAction.OnAddCard -> {
                viewModelScope.launch {
                    creditCardRepository.insertCreditCard(CreditCard(cardName = action.cardName.trim()))
                }
            }
            is CreditCardUiAction.OnUpdateCard -> {
                viewModelScope.launch {
                    creditCardRepository.updateCreditCard(action.card)
                }
            }
            is CreditCardUiAction.OnDeleteCard -> {
                viewModelScope.launch {
                    creditCardRepository.deleteCreditCard(action.card)
                }
            }
            is CreditCardUiAction.OnRepayBill -> {
                viewModelScope.launch {
                    val repaymentTxnId = System.currentTimeMillis()
                    transactionRepository.repayCreditCardTransactionsAtomically(
                        repaymentTxnId,
                        action.amount,
                        action.txnIds
                    )
                }
            }
            is CreditCardUiAction.OnToggleTransactionSelection -> {
                _uiState.update { current ->
                    val updated = if (current.selectedRepaidTxnIds.contains(action.txnId)) {
                        current.selectedRepaidTxnIds - action.txnId
                    } else {
                        current.selectedRepaidTxnIds + action.txnId
                    }
                    current.copy(selectedRepaidTxnIds = updated)
                }
            }
        }
    }
}
