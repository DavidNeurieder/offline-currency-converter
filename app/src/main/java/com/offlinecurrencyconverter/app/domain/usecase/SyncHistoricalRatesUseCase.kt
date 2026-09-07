package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncHistoricalRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(): Result<Unit> {
        if (!preferencesManager.historicalRatesChart.first()) {
            return Result.success(Unit)
        }
        return exchangeRateRepository.fetchAndStoreHistoricalRates()
    }
}