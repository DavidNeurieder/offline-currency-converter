package com.offlinecurrencyconverter.app.domain.repository

import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import kotlinx.coroutines.flow.Flow

interface HistoricalRateRepository {
    suspend fun getHistoricalRates(baseCurrency: String, targetCurrency: String): List<HistoricalRateEntity>

    fun observeHistoricalRates(baseCurrency: String, targetCurrency: String): Flow<List<HistoricalRateEntity>>

    /**
     * Makes sure at least [days] of history is cached for the given pair,
     * downloading only the missing interval on demand. A wider cached range
     * satisfies a narrower request without any network access. The result is
     * written to Room, so [observeHistoricalRates] updates the chart reactively.
     */
    suspend fun ensureHistory(sourceCurrency: String, targetCurrency: String, days: Int): Result<Unit>

    /**
     * Days of history currently cached for the given pair, measured from the
     * oldest covered date up to today. Zero when the pair has never been
     * downloaded.
     *
     * Used by the UI to tell "this range is already cached" from "this range
     * still has to be downloaded", so a range that is not cached yet can be
     * disabled while offline instead of drawing a shorter series under a
     * misleading label.
     */
    suspend fun coveredHistoryDays(sourceCurrency: String, targetCurrency: String): Int
}
