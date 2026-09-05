package com.ankitsudegora.data

import androidx.room.*
import androidx.room.Transaction as RoomTransactionAnnot
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @RoomTransactionAnnot
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionWithCategory>>

    @RoomTransactionAnnot
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithCategoryFlow(): Flow<List<TransactionWithCategory>>

    @RoomTransactionAnnot
    @Query("SELECT * FROM transactions WHERE isPending = 1 ORDER BY timestamp DESC")
    fun getPendingTransactionsWithCategoryFlow(): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT COUNT(*) FROM transactions WHERE isPending = 1")
    suspend fun getPendingTransactionsCount(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :since")
    suspend fun getTransactionsCountSince(since: Long): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND type = 'DEBIT' AND isPending = 0 AND timestamp >= :since")
    suspend fun getCategorySpentSince(categoryId: Long, since: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND type = :type AND LOWER(merchant) = LOWER(:merchant) AND timestamp >= :minTimestamp")
    suspend fun getMatchingTransactionCount(amount: Double, type: String, merchant: String, minTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE refNumber = :refNumber")
    suspend fun getTransactionCountByRefNumber(refNumber: String): Int

    @Query("SELECT * FROM transactions WHERE amount = :amount AND type = :type AND refNumber IS NULL AND timestamp >= :since LIMIT 1")
    suspend fun getMatchingPendingTransaction(amount: Double, type: String, since: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET ccRepaymentId = :repaymentId WHERE id IN (:txnIds)")
    suspend fun updateCcRepaymentIdForTransactions(repaymentId: Long, txnIds: List<Long>)

    @RoomTransactionAnnot
    suspend fun repayCreditCardTransactionsAtomically(repaymentId: Long, repaymentAmount: Double, txnIds: List<Long>) {
        var remaining = repaymentAmount
        for (id in txnIds) {
            val txn = getTransactionById(id) ?: continue
            val amt = txn.amount
            if (remaining >= amt) {
                updateTransaction(txn.copy(ccRepaymentId = repaymentId))
                remaining -= amt
            } else if (remaining > 0.0) {
                updateTransaction(txn.copy(amount = remaining, ccRepaymentId = repaymentId))
                val carryForward = txn.copy(
                    id = 0,
                    amount = amt - remaining,
                    ccRepaymentId = null
                )
                insertTransaction(carryForward)
                remaining = 0.0
            } else {
                break
            }
        }
    }

    @Query("SELECT * FROM transactions WHERE linkedListId = :listId LIMIT 1")
    suspend fun getTransactionByLinkedListId(listId: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE linkedListId = :listId ORDER BY timestamp DESC")
    suspend fun getTransactionsByLinkedListId(listId: Long): List<Transaction>
}
