package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.domain.repository.HistoricalRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoricalRateRepositoryImpl @Inject constructor(
    private val historicalRateDao: HistoricalRateDao
) : HistoricalRateRepository {

    override suspend fun getHistoricalRates(
        baseCurrency: String,
        targetCurrency: String
    ): List<HistoricalRateEntity> = observeHistoricalRates(baseCurrency, targetCurrency).first()

    override fun observeHistoricalRates(
        baseCurrency: String,
        targetCurrency: String
    ): Flow<List<HistoricalRateEntity>> {
        if (baseCurrency == targetCurrency) {
            return flowOf(emptyList())
        }

        if (baseCurrency == BASE_CURRENCY) {
            return historicalRateDao.getHistoricalRates(baseCurrency, targetCurrency)
        }

        val eurToBase = historicalRateDao.getHistoricalRates(BASE_CURRENCY, baseCurrency)

        if (targetCurrency == BASE_CURRENCY) {
            return eurToBase.map { rates ->
                rates.mapNotNull { eurToBaseRate ->
                    if (eurToBaseRate.rate == 0.0) return@mapNotNull null
                    HistoricalRateEntity(
                        baseCurrency = baseCurrency,
                        targetCurrency = targetCurrency,
                        rate = 1.0 / eurToBaseRate.rate,
                        date = eurToBaseRate.date
                    )
                }.sortedBy { it.date }
            }
        }

        val eurToTarget = historicalRateDao.getHistoricalRates(BASE_CURRENCY, targetCurrency)

        return combine(eurToBase, eurToTarget) { baseRates, targetRates ->
            if (baseRates.isEmpty() || targetRates.isEmpty()) {
                return@combine emptyList()
            }

            val baseRatesByDate = baseRates.associateBy { it.date }

            targetRates.mapNotNull { targetRate ->
                val baseRate = baseRatesByDate[targetRate.date] ?: return@mapNotNull null
                if (baseRate.rate == 0.0) return@mapNotNull null

                HistoricalRateEntity(
                    baseCurrency = baseCurrency,
                    targetCurrency = targetCurrency,
                    rate = targetRate.rate / baseRate.rate,
                    date = targetRate.date
                )
            }.sortedBy { it.date }
        }
    }

    companion object {
        private const val BASE_CURRENCY = "EUR"
    }
}
