package com.ankitsudegora.data

import androidx.room.*
import androidx.room.Transaction as RoomTransactionAnnot
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedDao {
    @RoomTransactionAnnot
    @Query("SELECT * FROM planned_lists ORDER BY createdTimestamp DESC")
    fun getAllPlannedListsFlow(): Flow<List<PlannedListWithItems>>

    @RoomTransactionAnnot
    @Query("SELECT * FROM planned_lists WHERE id = :id")
    suspend fun getPlannedListWithItemsById(id: Long): PlannedListWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedList(plannedList: PlannedList): Long

    @Update
    suspend fun updatePlannedList(plannedList: PlannedList)

    @Delete
    suspend fun deletePlannedList(plannedList: PlannedList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedItem(item: PlannedItem): Long

    @Update
    suspend fun updatePlannedItem(item: PlannedItem)

    @Delete
    suspend fun deletePlannedItem(item: PlannedItem)

    @Query("SELECT price FROM planned_items WHERE LOWER(name) = LOWER(:name) AND price > 0 ORDER BY id DESC LIMIT 1")
    suspend fun getLastPriceForItem(name: String): Double?
}
