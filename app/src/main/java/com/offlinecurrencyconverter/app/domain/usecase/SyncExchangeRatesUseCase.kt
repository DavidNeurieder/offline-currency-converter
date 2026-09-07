package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class SyncExchangeRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend operator fun invoke(syncIntervalMillis: Long): Result<Unit> {
        val lastUpdateTime = exchangeRateRepository.getLastUpdateTime()

        if (!isCacheStale(lastUpdateTime, syncIntervalMillis)) {
            return Result.success(Unit)
        }

        return exchangeRateRepository.fetchLatestRates(
            baseCurrency = BASE_CURRENCY,
            targetCurrencies = emptyList()
        )
    }

    suspend fun forceSync(): Result<Unit> {
        return exchangeRateRepository.fetchLatestRates(
            baseCurrency = BASE_CURRENCY,
            targetCurrencies = emptyList()
        )
    }

    companion object {
        const val BASE_CURRENCY = "EUR"

        fun isCacheStale(lastUpdated: Long?, maxAgeMillis: Long): Boolean {
            if (lastUpdated == null) return true
            val elapsed = maxOf(0L, System.currentTimeMillis() - lastUpdated)
            return elapsed >= maxAgeMillis
        }
    }
}