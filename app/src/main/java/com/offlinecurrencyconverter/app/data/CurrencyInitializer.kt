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
    companion object {
        val DEFAULT_FAVORITES = listOf("USD", "EUR", "GBP", "JPY", "CNY")
    }

    suspend fun initializeIfNeeded(): Result<Unit> {
        val isInitialized = preferencesManager.currenciesInitialized.first()
        val hasCurrencies = currencyRepository.hasCurrencies()
        
        if (!isInitialized || !hasCurrencies) {
            val result = currencyRepository.fetchAndSaveCurrenciesFromApi()
            if (result.isSuccess) {
                preferencesManager.setCurrenciesInitialized(true)
            }
            seedDefaultFavoritesIfNeeded()
            return result
        }
        
        seedDefaultFavoritesIfNeeded()
        return Result.success(Unit)
    }

    private suspend fun seedDefaultFavoritesIfNeeded() {
        val favoritesInitialized = preferencesManager.favoritesInitialized.first()
        if (favoritesInitialized) return

        val hasCurrencies = currencyRepository.hasCurrencies()
        if (!hasCurrencies) return

        for (code in DEFAULT_FAVORITES) {
            currencyRepository.updateFavorite(code, true)
        }
        preferencesManager.saveFavoritesInitialized(true)
    }
}
