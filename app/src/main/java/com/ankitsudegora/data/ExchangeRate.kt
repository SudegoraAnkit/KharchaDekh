package com.ankitsudegora.data

import androidx.room.Entity

@Entity(tableName = "exchange_rates", primaryKeys = ["baseCurrency", "targetCurrency"])
data class ExchangeRate(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val timestamp: Long
)
