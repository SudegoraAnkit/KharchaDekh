package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val allTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getAllTransactionsWithCategoryFlow()

    val pendingTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getPendingTransactionsWithCategoryFlow()

    val allCategories: Flow<List<Category>> =
        categoryDao.getAllCategoriesFlow()

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun getPendingCount(): Int {
        return transactionDao.getPendingTransactionsCount()
    }

    suspend fun getTransactionsCountSince(since: Long): Int {
        return transactionDao.getTransactionsCountSince(since)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun getAllCategoriesList(): List<Category> {
        return categoryDao.getAllCategories()
    }

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}
