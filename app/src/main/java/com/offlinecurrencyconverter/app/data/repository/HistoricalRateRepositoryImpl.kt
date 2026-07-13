package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.domain.repository.HistoricalRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HistoricalRateRepositoryImpl @Inject constructor(
    private val historicalRateDao: HistoricalRateDao
) : HistoricalRateRepository {

    override fun getHistoricalRates(baseCurrency: String, targetCurrency: String): Flow<List<HistoricalRateEntity>> {
        return historicalRateDao.getHistoricalRates(baseCurrency, targetCurrency)
    }
}
