package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncHistoricalRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(force: Boolean = false): Result<Unit> {
        if (!force && !preferencesManager.historicalRatesChart.first()) {
            return Result.success(Unit)
        }
        return exchangeRateRepository.fetchAndStoreHistoricalRates()
    }
}