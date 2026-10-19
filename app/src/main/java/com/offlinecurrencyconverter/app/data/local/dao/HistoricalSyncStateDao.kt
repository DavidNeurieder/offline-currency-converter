package com.offlinecurrencyconverter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalSyncStateEntity

/** Aggregate coverage across every quote stored for a base currency. */
data class HistoricalCoverageAggregate(
    val minStart: String?,
    val maxEnd: String?
)

@Dao
interface HistoricalSyncStateDao {
    @Query("SELECT * FROM historical_sync_state WHERE baseCurrency = :baseCurrency AND targetCurrency = :targetCurrency")
    suspend fun get(baseCurrency: String, targetCurrency: String): HistoricalSyncStateEntity?

    /**
     * Oldest covered start and newest covered end across every quote for [baseCurrency].
     *
     * A SQL aggregate always returns one row, so an empty table yields a row of
     * nulls rather than null; callers should treat null fields as "no coverage".
     */
    @Query(
        "SELECT MIN(coveredStartDate) AS minStart, MAX(coveredEndDate) AS maxEnd " +
            "FROM historical_sync_state WHERE baseCurrency = :baseCurrency"
    )
    suspend fun getCoverageAggregate(baseCurrency: String): HistoricalCoverageAggregate?

    @Upsert
    suspend fun upsert(state: HistoricalSyncStateEntity)

    @Query("DELETE FROM historical_sync_state")
    suspend fun deleteAll()
}
