package com.offlinecurrencyconverter.app.domain.model

data class ExchangeRate(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val lastUpdated: Long,
    val isOfflineAvailable: Boolean = false
) {
    override fun equals(other: Any?): Boolean = other is ExchangeRate &&
        baseCurrency == other.baseCurrency && targetCurrency == other.targetCurrency
    override fun hashCode(): Int = 31 * baseCurrency.hashCode() + targetCurrency.hashCode()
}
