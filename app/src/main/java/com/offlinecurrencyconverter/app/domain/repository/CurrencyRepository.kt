package com.offlinecurrencyconverter.app.domain.repository

import com.offlinecurrencyconverter.app.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    fun getAllCurrencies(): Flow<List<Currency>>
    fun getSelectedCurrencies(): Flow<List<Currency>>
    suspend fun getCurrencyByCode(code: String): Currency?
    suspend fun updateCurrencySelection(code: String, selected: Boolean)
    suspend fun initializeCurrencies(currencies: List<Currency>)
    suspend fun clearAllCurrencies()
    suspend fun fetchAndSaveCurrenciesFromApi(): Result<Unit>
    suspend fun hasCurrencies(): Boolean
}
