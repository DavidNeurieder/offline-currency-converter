package com.offlinecurrencyconverter.app.data

import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyInitializer @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun initializeIfNeeded(): Result<Unit> {
        val isInitialized = preferencesManager.currenciesInitialized.first()
        val hasCurrencies = currencyRepository.hasCurrencies()
        
        if (!isInitialized || !hasCurrencies) {
            val result = currencyRepository.fetchAndSaveCurrenciesFromApi()
            if (result.isSuccess) {
                preferencesManager.setCurrenciesInitialized(true)
            }
            return result
        }
        
        return Result.success(Unit)
    }
}
