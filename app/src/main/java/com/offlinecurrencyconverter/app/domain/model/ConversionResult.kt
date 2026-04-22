package com.offlinecurrencyconverter.app.domain.model

data class ConversionResult(
    val sourceAmount: Double,
    val sourceCurrency: Currency,
    val targetAmount: Double,
    val targetCurrency: Currency,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis()
)
