package com.offlinecurrencyconverter.app.domain.repository

import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import kotlinx.coroutines.flow.Flow

interface HistoricalRateRepository {
    suspend fun getHistoricalRates(baseCurrency: String, targetCurrency: String): List<HistoricalRateEntity>

    fun observeHistoricalRates(baseCurrency: String, targetCurrency: String): Flow<List<HistoricalRateEntity>>
}
