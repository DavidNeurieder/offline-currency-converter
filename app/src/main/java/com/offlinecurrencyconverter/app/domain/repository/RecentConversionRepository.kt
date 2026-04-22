package com.offlinecurrencyconverter.app.domain.repository

import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import kotlinx.coroutines.flow.Flow

interface RecentConversionRepository {
    fun getRecentConversions(limit: Int = 10): Flow<List<ConversionResult>>
    suspend fun saveConversion(conversion: ConversionResult)
    suspend fun clearHistory()
}
