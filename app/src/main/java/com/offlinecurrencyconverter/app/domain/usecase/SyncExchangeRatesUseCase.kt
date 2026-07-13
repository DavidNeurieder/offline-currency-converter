package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncExchangeRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(syncIntervalMillis: Long): Result<Unit> {
        val lastUpdateTime = exchangeRateRepository.getLastUpdateTime()
        
        if (lastUpdateTime != null) {
            val timeSinceLastSync = System.currentTimeMillis() - lastUpdateTime
            if (timeSinceLastSync < syncIntervalMillis) {
                return Result.success(Unit)
            }
        }

        val latestResult = exchangeRateRepository.fetchLatestRates(
            baseCurrency = "EUR",
            targetCurrencies = emptyList()
        )
        if (latestResult.isSuccess && preferencesManager.historicalRatesChart.first()) {
            exchangeRateRepository.fetchAndStoreHistoricalRates()
        }
        return latestResult
    }

    suspend fun forceSync(): Result<Unit> {
        val latestResult = exchangeRateRepository.fetchLatestRates(
            baseCurrency = "EUR",
            targetCurrencies = emptyList()
        )
        if (latestResult.isSuccess && preferencesManager.historicalRatesChart.first()) {
            exchangeRateRepository.fetchAndStoreHistoricalRates()
        }
        return latestResult
    }
}
