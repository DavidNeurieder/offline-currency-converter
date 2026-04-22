package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.ExchangeRateDao
import com.offlinecurrencyconverter.app.data.local.entity.ExchangeRateEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.domain.model.ExchangeRate
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExchangeRateRepositoryImpl @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val frankfurterApi: FrankfurterApi
) : ExchangeRateRepository {

    override fun getRatesForCurrency(baseCurrency: String): Flow<List<ExchangeRate>> {
        return exchangeRateDao.getRatesForCurrency(baseCurrency).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOfflineAvailableRates(): Flow<List<ExchangeRate>> {
        return exchangeRateDao.getOfflineAvailableRates().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRate(baseCurrency: String, targetCurrency: String): ExchangeRate? {
        if (baseCurrency == targetCurrency) {
            return ExchangeRate(
                baseCurrency = baseCurrency,
                targetCurrency = targetCurrency,
                rate = 1.0,
                lastUpdated = System.currentTimeMillis(),
                isOfflineAvailable = true
            )
        }

        val dbRate = exchangeRateDao.getRate(baseCurrency, targetCurrency)?.toDomain()
        if (dbRate != null) return dbRate

        val crossRateFromDb = calculateCrossRateFromDb(baseCurrency, targetCurrency)
        if (crossRateFromDb != null) return crossRateFromDb

        return null
    }

    private suspend fun calculateCrossRateFromDb(baseCurrency: String, targetCurrency: String): ExchangeRate? {
        val eurToBase = exchangeRateDao.getRate(BASE_CURRENCY, baseCurrency)?.toDomain()
        val eurToTarget = exchangeRateDao.getRate(BASE_CURRENCY, targetCurrency)?.toDomain()

        if (eurToBase != null && eurToTarget != null) {
            val crossRate = eurToTarget.rate / eurToBase.rate
            val latestUpdate = maxOf(eurToBase.lastUpdated, eurToTarget.lastUpdated)
            return ExchangeRate(
                baseCurrency = baseCurrency,
                targetCurrency = targetCurrency,
                rate = crossRate,
                lastUpdated = latestUpdate,
                isOfflineAvailable = eurToBase.isOfflineAvailable && eurToTarget.isOfflineAvailable
            )
        }

        if (baseCurrency == BASE_CURRENCY && eurToTarget != null) {
            return ExchangeRate(
                baseCurrency = baseCurrency,
                targetCurrency = targetCurrency,
                rate = eurToTarget.rate,
                lastUpdated = eurToTarget.lastUpdated,
                isOfflineAvailable = eurToTarget.isOfflineAvailable
            )
        }

        if (targetCurrency == BASE_CURRENCY && eurToBase != null) {
            val inverseRate = 1.0 / eurToBase.rate
            return ExchangeRate(
                baseCurrency = baseCurrency,
                targetCurrency = targetCurrency,
                rate = inverseRate,
                lastUpdated = eurToBase.lastUpdated,
                isOfflineAvailable = eurToBase.isOfflineAvailable
            )
        }

        return null
    }

    companion object {
        const val BASE_CURRENCY = "EUR"
    }

    override suspend fun fetchLatestRates(
        baseCurrency: String,
        targetCurrencies: List<String>
    ): Result<Unit> {
        return try {
            val quotesParam = if (targetCurrencies.isNotEmpty()) {
                targetCurrencies.joinToString(",")
            } else {
                null
            }
            val response = frankfurterApi.getRates(baseCurrency, quotesParam)
            if (response.isSuccessful) {
                val currentTime = System.currentTimeMillis()
                val rateItems = response.body() ?: emptyList()

                val rateEntities = rateItems.map { item ->
                    ExchangeRateEntity(
                        baseCurrency = item.base,
                        targetCurrency = item.quote,
                        rate = item.rate,
                        lastUpdated = currentTime,
                        isOfflineAvailable = true
                    )
                } + ExchangeRateEntity(
                    baseCurrency = baseCurrency,
                    targetCurrency = baseCurrency,
                    rate = 1.0,
                    lastUpdated = currentTime,
                    isOfflineAvailable = true
                )

                exchangeRateDao.insertRates(rateEntities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch rates: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastUpdateTime(): Long? {
        return exchangeRateDao.getLastUpdateTime()
    }

    private fun ExchangeRateEntity.toDomain(): ExchangeRate = ExchangeRate(
        baseCurrency = baseCurrency,
        targetCurrency = targetCurrency,
        rate = rate,
        lastUpdated = lastUpdated,
        isOfflineAvailable = isOfflineAvailable
    )
}
