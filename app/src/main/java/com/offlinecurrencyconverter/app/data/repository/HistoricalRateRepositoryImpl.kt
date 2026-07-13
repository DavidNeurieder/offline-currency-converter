package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.domain.repository.HistoricalRateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HistoricalRateRepositoryImpl @Inject constructor(
    private val historicalRateDao: HistoricalRateDao
) : HistoricalRateRepository {

    private val BASE_CURRENCY = "EUR"

    override suspend fun getHistoricalRates(baseCurrency: String, targetCurrency: String): List<HistoricalRateEntity> {
        if (baseCurrency == targetCurrency) {
            return emptyList()
        }

        if (baseCurrency == BASE_CURRENCY) {
            return historicalRateDao.getHistoricalRates(baseCurrency, targetCurrency).first()
        }

        val eurToBase = historicalRateDao.getHistoricalRates(BASE_CURRENCY, baseCurrency).first()
        val eurToTarget = historicalRateDao.getHistoricalRates(BASE_CURRENCY, targetCurrency).first()

        if (eurToBase.isEmpty() || eurToTarget.isEmpty()) {
            return emptyList()
        }

        val baseRatesByDate = eurToBase.associateBy { it.date }
        val targetRatesByDate = eurToTarget.associateBy { it.date }

        return targetRatesByDate.mapNotNull { (date, targetRate) ->
            val baseRate = baseRatesByDate[date] ?: return@mapNotNull null
            if (baseRate.rate == 0.0) return@mapNotNull null

            HistoricalRateEntity(
                baseCurrency = baseCurrency,
                targetCurrency = targetCurrency,
                rate = targetRate.rate / baseRate.rate,
                date = date
            )
        }.sortedBy { it.date }
    }
}
