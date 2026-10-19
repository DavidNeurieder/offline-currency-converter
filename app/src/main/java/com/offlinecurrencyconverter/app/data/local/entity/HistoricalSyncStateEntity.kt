package com.offlinecurrencyconverter.app.data.local.entity

import androidx.room.Entity

/**
 * Tracks the historical window that has been *successfully* downloaded for a
 * stored (EUR base, quote) pair.
 *
 * ISO-8601 `yyyy-MM-dd` dates compare correctly as plain strings, so the
 * coverage columns can be compared lexicographically in SQL and in Kotlin
 * without parsing. A date being absent from the `historical_rates` table does
 * not mean it was never fetched (weekends and holidays have no publication),
 * which is why coverage is recorded explicitly rather than inferred from the
 * latest stored observation.
 *
 * An incomplete download leaves [coveredStartDate]/[coveredEndDate] spanning only
 * the successfully fetched contiguous window, so the next sync retries the gap —
 * an incomplete history is never reported as complete.
 */
@Entity(
    tableName = "historical_sync_state",
    primaryKeys = ["baseCurrency", "targetCurrency"]
)
data class HistoricalSyncStateEntity(
    val baseCurrency: String,
    val targetCurrency: String,
    /** First date (inclusive, ISO) covered by a successful download, or null if never downloaded. */
    val coveredStartDate: String?,
    /** Last date (inclusive, ISO) covered by a successful download, or null if never downloaded. */
    val coveredEndDate: String?,
    /** Epoch millis of the last successful sync for this pair. */
    val lastSuccessfulSync: Long
)
