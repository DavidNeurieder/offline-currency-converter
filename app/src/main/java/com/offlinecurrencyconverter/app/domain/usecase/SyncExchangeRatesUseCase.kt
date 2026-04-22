package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class SyncExchangeRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend operator fun invoke(syncIntervalMillis: Long): Result<Unit> {
        val lastUpdateTime = exchangeRateRepository.getLastUpdateTime()
        
        if (lastUpdateTime != null) {
            val timeSinceLastSync = System.currentTimeMillis() - lastUpdateTime
            if (timeSinceLastSync < syncIntervalMillis) {
                return Result.success(Unit)
            }
        }

        return exchangeRateRepository.fetchLatestRates(
            baseCurrency = "EUR",
            targetCurrencies = emptyList()
        )
    }

    suspend fun forceSync(): Result<Unit> {
        return exchangeRateRepository.fetchLatestRates(
            baseCurrency = "EUR",
            targetCurrencies = emptyList()
        )
    }
}
