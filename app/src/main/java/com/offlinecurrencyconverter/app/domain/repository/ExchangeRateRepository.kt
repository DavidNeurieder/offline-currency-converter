package com.offlinecurrencyconverter.app.domain.repository

import com.offlinecurrencyconverter.app.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun getRatesForCurrency(baseCurrency: String): Flow<List<ExchangeRate>>
    fun getOfflineAvailableRates(): Flow<List<ExchangeRate>>
    suspend fun getRate(baseCurrency: String, targetCurrency: String): ExchangeRate?
    suspend fun fetchLatestRates(baseCurrency: String, targetCurrencies: List<String>): Result<Unit>
    suspend fun getLastUpdateTime(): Long?
}
