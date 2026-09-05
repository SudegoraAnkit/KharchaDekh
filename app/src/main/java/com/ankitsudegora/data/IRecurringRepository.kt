package com.ankitsudegora.data

import kotlinx.coroutines.flow.Flow

interface IRecurringRepository {
    val allSchedules: Flow<List<RecurringSchedule>>
    suspend fun getActiveSchedules(): List<RecurringSchedule>
    suspend fun insertSchedule(schedule: RecurringSchedule): Long
    suspend fun updateSchedule(schedule: RecurringSchedule)
    suspend fun deleteSchedule(schedule: RecurringSchedule)
}
