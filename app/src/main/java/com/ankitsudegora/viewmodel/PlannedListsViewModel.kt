package com.ankitsudegora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsudegora.data.IPlannedListRepository
import com.ankitsudegora.data.PlannedItem
import com.ankitsudegora.data.PlannedList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlannedListsViewModel(
    private val plannedRepository: IPlannedListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannedListsUiState())
    val uiState: StateFlow<PlannedListsUiState> = _uiState.asStateFlow()

    init {
        loadPlannedLists()
    }

    private fun loadPlannedLists() {
        viewModelScope.launch {
            plannedRepository.allPlannedLists.collect { lists ->
                val totalBudget = lists.sumOf { it.plannedList.budgetCap ?: 0.0 }
                val totalCost = lists.sumOf { listWithItems ->
                    listWithItems.items.sumOf { it.quantity * it.price }
                }

                _uiState.update { current ->
                    current.copy(
                        lists = lists,
                        totalBudget = totalBudget,
                        totalEstimatedCost = totalCost,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onAction(action: PlannedListsUiAction) {
        when (action) {
            is PlannedListsUiAction.OnCreateList -> {
                viewModelScope.launch {
                    val list = PlannedList(name = action.name.trim(), budgetCap = action.budgetCap)
                    plannedRepository.insertPlannedList(list)
                }
            }
            is PlannedListsUiAction.OnUpdateList -> {
                viewModelScope.launch {
                    plannedRepository.updatePlannedList(action.list)
                }
            }
            is PlannedListsUiAction.OnDeleteList -> {
                viewModelScope.launch {
                    plannedRepository.deletePlannedList(action.list)
                }
            }
            is PlannedListsUiAction.OnAddItem -> {
                viewModelScope.launch {
                    val item = PlannedItem(
                        listId = action.listId,
                        name = action.name.trim(),
                        quantity = action.quantity,
                        price = action.price
                    )
                    plannedRepository.insertPlannedItem(item)
                }
            }
            is PlannedListsUiAction.OnToggleItemChecked -> {
                viewModelScope.launch {
                    plannedRepository.updatePlannedItem(action.item.copy(isChecked = !action.item.isChecked))
                }
            }
            is PlannedListsUiAction.OnDeleteItem -> {
                viewModelScope.launch {
                    plannedRepository.deletePlannedItem(action.item)
                }
            }
        }
    }
}
