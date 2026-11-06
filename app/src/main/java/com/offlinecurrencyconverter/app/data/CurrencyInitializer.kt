package com.offlinecurrencyconverter.app.data

import com.offlinecurrencyconverter.app.BuildConfig
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyInitializer @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        val DEFAULT_FAVORITES = listOf("USD", "EUR", "GBP", "JPY", "CNY")
        private const val CURRENCY_REFRESH_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
    }

    private val initMutex = Mutex()

    suspend fun initializeIfNeeded(force: Boolean = false): Result<Unit> = initMutex.withLock {
        val isInitialized = preferencesManager.currenciesInitialized.first()
        val hasCurrencies = currencyRepository.hasCurrencies()
        val lastInstalledVersion = preferencesManager.lastInstalledVersion.first()
        val lastRefreshed = preferencesManager.currenciesLastRefreshed.first()

        val isStale = force || !isInitialized || !hasCurrencies ||
            lastInstalledVersion < BuildConfig.VERSION_CODE ||
            System.currentTimeMillis() - lastRefreshed > CURRENCY_REFRESH_INTERVAL_MS

        if (isStale) {
            val result = currencyRepository.fetchAndSaveCurrenciesFromApi()
            if (result.isSuccess) {
                preferencesManager.setCurrenciesInitialized(true)
                preferencesManager.setCurrenciesLastRefreshed(System.currentTimeMillis())
            }
            seedDefaultFavoritesIfNeeded()
            result
        } else {
            seedDefaultFavoritesIfNeeded()
            Result.success(Unit)
        }
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
