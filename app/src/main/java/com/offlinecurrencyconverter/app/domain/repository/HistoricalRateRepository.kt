package com.offlinecurrencyconverter.app.domain.repository

import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity

interface HistoricalRateRepository {
    suspend fun getHistoricalRates(baseCurrency: String, targetCurrency: String): List<HistoricalRateEntity>
}
