package com.ankitsudegora.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val recurringScheduleDao: RecurringScheduleDao,
    private val plannedDao: PlannedDao,
    private val creditCardDao: CreditCardDao
) : ITransactionRepository, ICreditCardRepository, IPlannedListRepository, ICategoryRepository, IRecurringRepository {

    // --- Transactions ---
    override val allTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getAllTransactionsWithCategoryFlow()

    override val pendingTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getPendingTransactionsWithCategoryFlow()

    override suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getTransactionById(id)

    override suspend fun getPendingCount(): Int =
        transactionDao.getPendingTransactionsCount()

    override suspend fun getTransactionsCountSince(since: Long): Int =
        transactionDao.getTransactionsCountSince(since)

    override suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun getCategorySpentSince(categoryId: Long, since: Long): Double =
        transactionDao.getCategorySpentSince(categoryId, since) ?: 0.0

    override suspend fun updateCcRepaymentIdForTransactions(repaymentId: Long, txnIds: List<Long>) =
        transactionDao.updateCcRepaymentIdForTransactions(repaymentId, txnIds)

    override suspend fun repayCreditCardTransactionsAtomically(repaymentId: Long, repaymentAmount: Double, txnIds: List<Long>) =
        transactionDao.repayCreditCardTransactionsAtomically(repaymentId, repaymentAmount, txnIds)

    override suspend fun getTransactionByLinkedListId(listId: Long): Transaction? =
        transactionDao.getTransactionByLinkedListId(listId)

    override suspend fun getTransactionsByLinkedListId(listId: Long): List<Transaction> =
        transactionDao.getTransactionsByLinkedListId(listId)

    // --- Categories ---
    override val allCategories: Flow<List<Category>> =
        categoryDao.getAllCategoriesFlow()

    override suspend fun getAllCategoriesList(): List<Category> =
        categoryDao.getAllCategories()

    override suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)

    override suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category)

    // --- Credit Cards ---
    override val allCreditCards: Flow<List<CreditCard>> =
        creditCardDao.getAllCardsFlow()

    override suspend fun getAllCreditCards(): List<CreditCard> =
        creditCardDao.getAllCards()

    override suspend fun insertCreditCard(card: CreditCard): Long =
        creditCardDao.insertCard(card)

    override suspend fun updateCreditCard(card: CreditCard) =
        creditCardDao.updateCard(card)

    override suspend fun deleteCreditCard(card: CreditCard) =
        creditCardDao.deleteCard(card)

    // --- Planned Lists & Items ---
    override val allPlannedLists: Flow<List<PlannedListWithItems>> =
        plannedDao.getAllPlannedListsFlow()

    override suspend fun getPlannedListWithItemsById(id: Long): PlannedListWithItems? =
        plannedDao.getPlannedListWithItemsById(id)

    override suspend fun insertPlannedList(plannedList: PlannedList): Long =
        plannedDao.insertPlannedList(plannedList)

    override suspend fun updatePlannedList(plannedList: PlannedList) =
        plannedDao.updatePlannedList(plannedList)

    override suspend fun deletePlannedList(plannedList: PlannedList) =
        plannedDao.deletePlannedList(plannedList)

    override suspend fun insertPlannedItem(item: PlannedItem): Long =
        plannedDao.insertPlannedItem(item)

    override suspend fun updatePlannedItem(item: PlannedItem) =
        plannedDao.updatePlannedItem(item)

    override suspend fun deletePlannedItem(item: PlannedItem) =
        plannedDao.deletePlannedItem(item)

    override suspend fun getLastPriceForItem(name: String): Double? =
        plannedDao.getLastPriceForItem(name)

    // --- Recurring Schedules ---
    override val allSchedules: Flow<List<RecurringSchedule>> =
        recurringScheduleDao.getAllSchedulesFlow()

    override suspend fun getActiveSchedules(): List<RecurringSchedule> =
        recurringScheduleDao.getActiveSchedules()

    override suspend fun insertSchedule(schedule: RecurringSchedule): Long =
        recurringScheduleDao.insertSchedule(schedule)

    override suspend fun updateSchedule(schedule: RecurringSchedule) =
        recurringScheduleDao.updateSchedule(schedule)

    override suspend fun deleteSchedule(schedule: RecurringSchedule) =
        recurringScheduleDao.deleteSchedule(schedule)
}
