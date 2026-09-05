package com.ankitsudegora.viewmodel

import com.ankitsudegora.data.PlannedItem
import com.ankitsudegora.data.PlannedList
import com.ankitsudegora.data.PlannedListWithItems

/**
 * Immutable MVI UI State for Planned Shopping Lists.
 */
data class PlannedListsUiState(
    val lists: List<PlannedListWithItems> = emptyList(),
    val activeList: PlannedListWithItems? = null,
    val totalBudget: Double = 0.0,
    val totalEstimatedCost: Double = 0.0,
    val isAddListDialogOpen: Boolean = false,
    val isAddItemDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Sealed MVI UI Actions for Planned Lists Screen.
 */
sealed interface PlannedListsUiAction {
    data class OnCreateList(val name: String, val budgetCap: Double?) : PlannedListsUiAction
    data class OnUpdateList(val list: PlannedList) : PlannedListsUiAction
    data class OnDeleteList(val list: PlannedList) : PlannedListsUiAction
    data class OnAddItem(val listId: Long, val name: String, val quantity: Int, val price: Double) : PlannedListsUiAction
    data class OnToggleItemChecked(val item: PlannedItem) : PlannedListsUiAction
    data class OnDeleteItem(val item: PlannedItem) : PlannedListsUiAction
}
