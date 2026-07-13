package com.offlinecurrencyconverter.app.domain.repository

import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import kotlinx.coroutines.flow.Flow

interface HistoricalRateRepository {
    fun getHistoricalRates(baseCurrency: String, targetCurrency: String): Flow<List<HistoricalRateEntity>>
}
