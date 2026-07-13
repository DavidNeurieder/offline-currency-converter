package com.offlinecurrencyconverter.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "historical_rates",
    primaryKeys = ["baseCurrency", "targetCurrency", "date"]
)
data class HistoricalRateEntity(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val date: String
)
