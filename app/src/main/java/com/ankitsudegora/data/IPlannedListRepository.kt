package com.ankitsudegora.data

import kotlinx.coroutines.flow.Flow

interface IPlannedListRepository {
    val allPlannedLists: Flow<List<PlannedListWithItems>>

    suspend fun getPlannedListWithItemsById(id: Long): PlannedListWithItems?
    suspend fun insertPlannedList(plannedList: PlannedList): Long
    suspend fun updatePlannedList(plannedList: PlannedList)
    suspend fun deletePlannedList(plannedList: PlannedList)
    suspend fun insertPlannedItem(item: PlannedItem): Long
    suspend fun updatePlannedItem(item: PlannedItem)
    suspend fun deletePlannedItem(item: PlannedItem)
    suspend fun getLastPriceForItem(name: String): Double?
}
