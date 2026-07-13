package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.domain.repository.HistoricalRateRepository
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class HistoricalRateRepositoryImpl @Inject constructor(
    private val historicalRateDao: HistoricalRateDao,
    private val frankfurterApi: FrankfurterApi
) : HistoricalRateRepository {

    override fun getHistoricalRates(baseCurrency: String, targetCurrency: String): Flow<List<HistoricalRateEntity>> {
        return historicalRateDao.getHistoricalRates(baseCurrency, targetCurrency)
    }

    override suspend fun fetchAndStoreHistoricalRates(
        baseCurrency: String,
        targetCurrency: String,
        days: Int
    ): Result<Unit> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = dateFormat.format(calendar.time)

            val response = frankfurterApi.getHistoricalRates(
                baseCurrency = baseCurrency,
                targetCurrencies = targetCurrency,
                startDate = startDate,
                endDate = endDate
            )

            if (response.isSuccessful) {
                val rateItems = response.body() ?: emptyList()
                val entities = rateItems.map { item ->
                    HistoricalRateEntity(
                        baseCurrency = item.base,
                        targetCurrency = item.quote,
                        rate = item.rate,
                        date = item.date
                    )
                }
                historicalRateDao.deleteRates(baseCurrency, targetCurrency)
                historicalRateDao.insertRates(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch historical rates: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
