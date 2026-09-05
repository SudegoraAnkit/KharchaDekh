package com.ankitsudegora.viewmodel

import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.TransactionWithCategory

/**
 * Immutable MVI UI State for Credit Card Management.
 */
data class CreditCardUiState(
    val cards: List<CreditCard> = emptyList(),
    val unbilledTransactions: List<TransactionWithCategory> = emptyList(),
    val totalOutstanding: Double = 0.0,
    val selectedRepaidTxnIds: Set<Long> = emptySet(),
    val isRepaymentDialogOpen: Boolean = false,
    val isAddCardDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Sealed MVI UI Actions for Credit Card Screen.
 */
sealed interface CreditCardUiAction {
    data class OnAddCard(val cardName: String) : CreditCardUiAction
    data class OnUpdateCard(val card: CreditCard) : CreditCardUiAction
    data class OnDeleteCard(val card: CreditCard) : CreditCardUiAction
    data class OnRepayBill(val card: CreditCard, val amount: Double, val txnIds: List<Long>) : CreditCardUiAction
    data class OnToggleTransactionSelection(val txnId: Long) : CreditCardUiAction
}
