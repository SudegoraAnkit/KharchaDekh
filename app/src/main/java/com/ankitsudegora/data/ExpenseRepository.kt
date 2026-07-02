package com.ankitsudegora.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val recurringScheduleDao: RecurringScheduleDao,
    private val plannedDao: PlannedDao,
    private val creditCardDao: CreditCardDao
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

    // --- Credit Card CRUD ---
    val allCreditCards: Flow<List<CreditCard>> = creditCardDao.getAllCardsFlow()
    suspend fun getAllCreditCards(): List<CreditCard> = creditCardDao.getAllCards()
    suspend fun insertCreditCard(card: CreditCard): Long = creditCardDao.insertCard(card)
    suspend fun deleteCreditCard(card: CreditCard) = creditCardDao.deleteCard(card)
    suspend fun updateCcRepaymentIdForTransactions(repaymentId: Long, txnIds: List<Long>) {
        transactionDao.updateCcRepaymentIdForTransactions(repaymentId, txnIds)
    }

    // --- Planned Lists & Items CRUD ---
    val allPlannedLists: Flow<List<PlannedListWithItems>> =
        plannedDao.getAllPlannedListsFlow()

    suspend fun getPlannedListWithItemsById(id: Long): PlannedListWithItems? {
        return plannedDao.getPlannedListWithItemsById(id)
    }

    suspend fun insertPlannedList(plannedList: PlannedList): Long {
        return plannedDao.insertPlannedList(plannedList)
    }

    suspend fun updatePlannedList(plannedList: PlannedList) {
        plannedDao.updatePlannedList(plannedList)
    }

    suspend fun deletePlannedList(plannedList: PlannedList) {
        plannedDao.deletePlannedList(plannedList)
    }

    suspend fun insertPlannedItem(item: PlannedItem): Long {
        return plannedDao.insertPlannedItem(item)
    }

    suspend fun updatePlannedItem(item: PlannedItem) {
        plannedDao.updatePlannedItem(item)
    }

    suspend fun deletePlannedItem(item: PlannedItem) {
        plannedDao.deletePlannedItem(item)
    }

    suspend fun getLastPriceForItem(name: String): Double? {
        return plannedDao.getLastPriceForItem(name)
    }

    suspend fun getTransactionByLinkedListId(listId: Long): Transaction? {
        return transactionDao.getTransactionByLinkedListId(listId)
    }
}
