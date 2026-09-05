package com.ankitsudegora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringScheduleDao {
    @Query("SELECT * FROM recurring_schedules ORDER BY id DESC")
    fun getAllSchedulesFlow(): Flow<List<RecurringSchedule>>

    @Query("SELECT * FROM recurring_schedules WHERE isActive = 1")
    suspend fun getActiveSchedules(): List<RecurringSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: RecurringSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: RecurringSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: RecurringSchedule)
}
