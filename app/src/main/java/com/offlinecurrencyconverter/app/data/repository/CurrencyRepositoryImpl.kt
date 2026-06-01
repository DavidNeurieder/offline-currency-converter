package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.entity.CurrencyEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val currencyDao: CurrencyDao,
    private val frankfurterApi: FrankfurterApi
) : CurrencyRepository {

    override fun getAllCurrencies(): Flow<List<Currency>> {
        return currencyDao.getAllCurrencies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSelectedCurrencies(): Flow<List<Currency>> {
        return currencyDao.getSelectedCurrencies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCurrencyByCode(code: String): Currency? {
        return currencyDao.getCurrencyByCode(code)?.toDomain()
    }

    override suspend fun updateCurrencySelection(code: String, selected: Boolean) {
        currencyDao.updateSelection(code, selected)
    }

    override suspend fun initializeCurrencies(currencies: List<Currency>) {
        currencyDao.insertCurrencies(currencies.map { it.toEntity() })
    }

    override suspend fun clearAllCurrencies() {
        currencyDao.deleteAll()
    }

    override suspend fun hasCurrencies(): Boolean {
        return currencyDao.getCount() > 0
    }

    override suspend fun fetchAndSaveCurrenciesFromApi(): Result<Unit> {
        return try {
            val response = frankfurterApi.getCurrencies()
            if (response.isSuccessful) {
                val currencies = response.body() ?: emptyList()
                val entities = currencies.map { item ->
                    CurrencyEntity(
                        code = item.isoCode,
                        name = item.name,
                        symbol = item.symbol ?: "",
                        isoNumeric = item.isoNumeric,
                        startDate = item.startDate,
                        endDate = item.endDate,
                        isSelectedForOffline = item.isoCode in DEFAULT_SELECTED_CURRENCIES,
                    )
                }
                currencyDao.deleteAll()
                currencyDao.insertCurrencies(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch currencies: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun CurrencyEntity.toDomain(): Currency = Currency(
        code = code,
        name = name,
        symbol = symbol,
        isSelectedForOffline = isSelectedForOffline
    )

    private fun Currency.toEntity(): CurrencyEntity = CurrencyEntity(
        code = code,
        name = name,
        symbol = symbol,
        isSelectedForOffline = isSelectedForOffline
    )

    companion object {
        private val DEFAULT_SELECTED_CURRENCIES = setOf("EUR", "GBP", "USD")
    }
}
