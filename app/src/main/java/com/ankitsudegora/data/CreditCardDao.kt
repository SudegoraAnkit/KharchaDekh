package com.ankitsudegora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    fun getAllCardsFlow(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    suspend fun getAllCards(): List<CreditCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCard): Long

    @Update
    suspend fun updateCard(card: CreditCard)

    @Delete
    suspend fun deleteCard(card: CreditCard)
}
