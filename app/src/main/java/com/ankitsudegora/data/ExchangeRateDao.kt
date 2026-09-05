package com.ankitsudegora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    fun getAllRatesFlow(): Flow<List<ExchangeRate>>

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAllRates(): List<ExchangeRate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRate>)

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAllRates()
}
