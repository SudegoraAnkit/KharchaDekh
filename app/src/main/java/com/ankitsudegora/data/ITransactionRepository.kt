package com.ankitsudegora.data

import kotlinx.coroutines.flow.Flow

interface ITransactionRepository {
    val allTransactions: Flow<List<TransactionWithCategory>>
    val pendingTransactions: Flow<List<TransactionWithCategory>>

    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun getPendingCount(): Int
    suspend fun getTransactionsCountSince(since: Long): Int
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getCategorySpentSince(categoryId: Long, since: Long): Double
    suspend fun updateCcRepaymentIdForTransactions(repaymentId: Long, txnIds: List<Long>)
    suspend fun repayCreditCardTransactionsAtomically(repaymentId: Long, repaymentAmount: Double, txnIds: List<Long>)
    suspend fun getTransactionByLinkedListId(listId: Long): Transaction?
    suspend fun getTransactionsByLinkedListId(listId: Long): List<Transaction>
}
