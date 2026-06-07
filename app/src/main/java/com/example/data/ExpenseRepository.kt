package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val recurringScheduleDao: RecurringScheduleDao
) {
    val allTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getAllTransactionsWithCategoryFlow()

    val pendingTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getPendingTransactionsWithCategoryFlow()

    val allCategories: Flow<List<Category>> =
        categoryDao.getAllCategoriesFlow()

    val allSchedules: Flow<List<RecurringSchedule>> =
        recurringScheduleDao.getAllSchedulesFlow()

    suspend fun getActiveSchedules(): List<RecurringSchedule> {
        return recurringScheduleDao.getActiveSchedules()
    }

    suspend fun insertSchedule(schedule: RecurringSchedule): Long {
        return recurringScheduleDao.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: RecurringSchedule) {
        recurringScheduleDao.updateSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: RecurringSchedule) {
        recurringScheduleDao.deleteSchedule(schedule)
    }

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

    suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun getCategorySpentSince(categoryId: Long, since: Long): Double {
        return transactionDao.getCategorySpentSince(categoryId, since) ?: 0.0
    }

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}
