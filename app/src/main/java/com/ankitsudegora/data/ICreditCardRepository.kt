package com.ankitsudegora.data

import kotlinx.coroutines.flow.Flow

interface ICreditCardRepository {
    val allCreditCards: Flow<List<CreditCard>>
    suspend fun getAllCreditCards(): List<CreditCard>
    suspend fun insertCreditCard(card: CreditCard): Long
    suspend fun updateCreditCard(card: CreditCard)
    suspend fun deleteCreditCard(card: CreditCard)
}
