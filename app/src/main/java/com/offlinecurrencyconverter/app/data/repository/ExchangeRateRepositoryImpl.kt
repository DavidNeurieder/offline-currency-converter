package com.offlinecurrencyconverter.app.data.repository

import android.util.Log
import com.offlinecurrencyconverter.app.data.local.dao.ExchangeRateDao
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.ExchangeRateEntity
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.domain.model.ExchangeRate
import com.offlinecurrencyconverter.app.domain.model.SyncError
import com.offlinecurrencyconverter.app.domain.model.SyncErrorException
import com.offlinecurrencyconverter.app.domain.model.isValidExchangeRate
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import com.offlinecurrencyconverter.app.domain.validation.ExchangeRateResponseValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class ExchangeRateRepositoryImpl @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val historicalRateDao: HistoricalRateDao,
    private val frankfurterApi: FrankfurterApi,
    private val responseValidator: ExchangeRateResponseValidator
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

    override suspend fun getAllRatesForCurrency(baseCurrency: String): List<ExchangeRate> {
        if (baseCurrency == BASE_CURRENCY) {
            return exchangeRateDao.getRatesForCurrencyOnce(baseCurrency)
                .map { it.toDomain() }
                .filter { it.rate.isValidExchangeRate() }
        }

        val eurToBase = exchangeRateDao.getRate(BASE_CURRENCY, baseCurrency)?.toDomain()
            ?: return emptyList()
        if (!eurToBase.rate.isValidExchangeRate()) return emptyList()
        val allEurRates = exchangeRateDao.getRatesForCurrencyOnce(BASE_CURRENCY)
            .map { it.toDomain() }
            .filter { it.rate.isValidExchangeRate() }

        return allEurRates.filter { it.targetCurrency != baseCurrency }.map { eurToTarget ->
            ExchangeRate(
                baseCurrency = baseCurrency,
                targetCurrency = eurToTarget.targetCurrency,
                rate = eurToTarget.rate / eurToBase.rate,
                lastUpdated = maxOf(eurToBase.lastUpdated, eurToTarget.lastUpdated),
                isOfflineAvailable = eurToBase.isOfflineAvailable && eurToTarget.isOfflineAvailable
            )
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
        if (dbRate != null && dbRate.rate.isValidExchangeRate()) return dbRate

        val crossRateFromDb = calculateCrossRateFromDb(baseCurrency, targetCurrency)
        if (crossRateFromDb != null) return crossRateFromDb

        return null
    }

    private suspend fun calculateCrossRateFromDb(baseCurrency: String, targetCurrency: String): ExchangeRate? {
        val eurToBase = exchangeRateDao.getRate(BASE_CURRENCY, baseCurrency)?.toDomain()
        val eurToTarget = exchangeRateDao.getRate(BASE_CURRENCY, targetCurrency)?.toDomain()

        if (eurToBase != null && eurToTarget != null &&
            eurToBase.rate.isValidExchangeRate() && eurToTarget.rate.isValidExchangeRate()
        ) {
            val crossRate = eurToTarget.rate / eurToBase.rate
            if (!crossRate.isValidExchangeRate()) return null
            val latestUpdate = maxOf(eurToBase.lastUpdated, eurToTarget.lastUpdated)
            return ExchangeRate(
                baseCurrency = baseCurrency,
                targetCurrency = targetCurrency,
                rate = crossRate,
                lastUpdated = latestUpdate,
                isOfflineAvailable = eurToBase.isOfflineAvailable && eurToTarget.isOfflineAvailable
            )
        }

        if (baseCurrency == BASE_CURRENCY && eurToTarget != null && eurToTarget.rate.isValidExchangeRate()) {
            return ExchangeRate(
                baseCurrency = baseCurrency,
                targetCurrency = targetCurrency,
                rate = eurToTarget.rate,
                lastUpdated = eurToTarget.lastUpdated,
                isOfflineAvailable = eurToTarget.isOfflineAvailable
            )
        }

        if (targetCurrency == BASE_CURRENCY && eurToBase != null && eurToBase.rate.isValidExchangeRate()) {
            val inverseRate = 1.0 / eurToBase.rate
            if (!inverseRate.isValidExchangeRate()) return null
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
        private const val TAG = "ExchangeRateRepository"
    }

    private fun reasonOf(error: Throwable): String {
        return (error as? SyncErrorException)?.syncError?.toString() ?: error.toString()
    }

    private fun requestedLabel(targets: List<String>): String {
        return if (targets.isEmpty()) "all" else targets.size.toString()
    }

    private fun mapHttpError(code: Int): Exception {
        return if (code >= 500) {
            SyncErrorException(SyncError.Server)
        } else {
            SyncErrorException(SyncError.Http(code))
        }
    }

    override suspend fun fetchLatestRates(
        baseCurrency: String,
        targetCurrencies: List<String>
    ): Result<Unit> {
        return try {
            val startedAt = System.currentTimeMillis()
            val quotesParam = targetCurrencies.takeIf { it.isNotEmpty() }
                ?.joinToString(",")
            val response = frankfurterApi.getRates(baseCurrency, quotesParam)

            if (!response.isSuccessful) {
                Log.d(TAG, "Rate sync failed: reason=${mapHttpError(response.code())} code=${response.code()}")
                return Result.failure(mapHttpError(response.code()))
            }

            val body = response.body()
                ?: return Result.failure(SyncErrorException(SyncError.EmptyResponse))

            responseValidator.validateLatest(body, baseCurrency, targetCurrencies)
                .getOrElse { error ->
                    Log.d(
                        TAG,
                        "Rate sync failed: reason=${reasonOf(error)} " +
                            "requested=${requestedLabel(targetCurrencies)} received=${body.size}"
                    )
                    return Result.failure(error)
                }

            val currentTime = System.currentTimeMillis()
            val rateEntities = body
                .filterNot { it.quote == baseCurrency }
                .map { item ->
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

            exchangeRateDao.replaceAll(rateEntities)
            Log.d(
                TAG,
                "Rate sync: base=$baseCurrency requested=${requestedLabel(targetCurrencies)} " +
                    "received=${body.size} validated=${body.size} stored=${rateEntities.size} " +
                    "duration=${System.currentTimeMillis() - startedAt}ms"
            )
            Result.success(Unit)
        } catch (e: java.io.IOException) {
            Log.d(TAG, "Rate sync failed: reason=Network")
            Result.failure(SyncErrorException(SyncError.Network))
        } catch (e: Exception) {
            Log.d(TAG, "Rate sync failed: reason=Unexpected")
            Result.failure(e)
        }
    }

    override suspend fun fetchAndStoreHistoricalRates(): Result<Unit> {
        return try {
            val startedAt = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -90)
            val startDate = dateFormat.format(calendar.time)

            val response = frankfurterApi.getHistoricalRates(
                baseCurrency = BASE_CURRENCY,
                targetCurrencies = null,
                startDate = startDate,
                endDate = endDate
            )

            if (!response.isSuccessful) {
                Log.d(TAG, "History sync failed: reason=${mapHttpError(response.code())} code=${response.code()}")
                return Result.failure(mapHttpError(response.code()))
            }

            val body = response.body()
                ?: return Result.failure(SyncErrorException(SyncError.EmptyResponse))

            responseValidator.validateHistorical(body, BASE_CURRENCY, startDate, endDate)
                .getOrElse { error ->
                    Log.d(
                        TAG,
                        "History sync failed: reason=${reasonOf(error)} " +
                            "window=$startDate..$endDate received=${body.size}"
                    )
                    return Result.failure(error)
                }

            val entities = body
                .filterNot { it.quote == BASE_CURRENCY }
                .map { item ->
                    HistoricalRateEntity(
                        baseCurrency = item.base,
                        targetCurrency = item.quote,
                        rate = item.rate,
                        date = item.date
                    )
                }
            historicalRateDao.replaceAll(entities)
            Log.d(
                TAG,
                "History sync: base=$BASE_CURRENCY window=$startDate..$endDate " +
                    "received=${body.size} validated=${body.size} stored=${entities.size} " +
                    "duration=${System.currentTimeMillis() - startedAt}ms"
            )
            Result.success(Unit)
        } catch (e: java.io.IOException) {
            Log.d(TAG, "History sync failed: reason=Network")
            Result.failure(SyncErrorException(SyncError.Network))
        } catch (e: Exception) {
            Log.d(TAG, "History sync failed: reason=Unexpected")
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
