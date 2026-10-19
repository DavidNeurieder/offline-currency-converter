package com.offlinecurrencyconverter.app.data.repository

import android.util.Log
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalSyncStateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalSyncStateEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.domain.model.SyncError
import com.offlinecurrencyconverter.app.domain.model.SyncErrorException
import com.offlinecurrencyconverter.app.domain.validation.ExchangeRateResponseValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Downloads historical exchange rates once and afterwards only the missing
 * interval, keeping all previously stored observations.
 *
 * All history is stored against the [ExchangeRateRepositoryImpl.BASE_CURRENCY]
 * base (the normalized schema), so the synchronization unit is a stored
 * (base, quote) pair. Successful coverage of each pair is recorded in
 * [historical_sync_state][HistoricalSyncStateEntity] so that:
 *
 *  - a range is only downloaded once (a wider range satisfies a narrower one),
 *  - subsequent syncs request only the gap after the last covered date,
 *  - an incomplete download is never reported as complete, and
 *  - responses are upserted, so overlapping or repeated fetches never duplicate.
 *
 * Reads stay in Room; this class only writes, and the chart reacts through Room.
 */
@Singleton
class HistoricalRateSyncer @Inject constructor(
    private val historicalRateDao: HistoricalRateDao,
    private val syncStateDao: HistoricalSyncStateDao,
    private val frankfurterApi: FrankfurterApi,
    private val responseValidator: ExchangeRateResponseValidator,
    @Named("minHistoricalRecords") private val minHistoricalRecords: Int
) {

    private val syncMutex = Mutex()

    /**
     * Ensures at least [days] of history exists for the given currency pair,
     * downloading only what is missing. Called when the user opens a wider chart
     * range (e.g. 1Y or 5Y) than has been cached so far.
     */
    suspend fun ensureHistory(
        sourceCurrency: String,
        targetCurrency: String,
        days: Int
    ): Result<Unit> {
        // A pair with itself always converts 1:1; there is nothing to fetch.
        if (sourceCurrency == targetCurrency) return Result.success(Unit)

        val quotes = listOfNotNull(
            sourceCurrency.takeUnless { it == BASE_CURRENCY },
            targetCurrency.takeUnless { it == BASE_CURRENCY }
        ).distinct()

        // EUR on both sides leaves no stored quote to fetch.
        if (quotes.isEmpty()) return Result.success(Unit)

        return syncMutex.withLock { ensureCoverage(quotes, days, label = "deep") }
    }

    /**
     * Keeps roughly [warmSyncDays] of history fresh for *all* quotes. Used for the
     * first-install bootstrap, the periodic background job, and pull-to-refresh.
     */
    suspend fun syncRecentHistory(warmSyncDays: Int = WARM_SYNC_DAYS): Result<Unit> =
        syncMutex.withLock {
            val today = LocalDate.now()
            val desiredStart = today.minusDays(warmSyncDays.toLong())
            val freshCutoff = today.minusDays(OVERLAP_DAYS.toLong())

            val aggregate = syncStateDao.getCoverageAggregate(BASE_CURRENCY)
            val coveredStart = aggregate?.minStart?.let(::parseDate)
            val coveredEnd = aggregate?.maxEnd?.let(::parseDate)

            val from = missingFrom(coveredStart, coveredEnd, desiredStart, freshCutoff)
            if (from == null) {
                Log.d(TAG, "Warm history up to date: coverage=${aggregate?.minStart}..${aggregate?.maxEnd}")
                return@withLock Result.success(Unit)
            }

            runChunks(from, today, requestedQuotes = emptyList(), label = "warm")
        }

    // ---------------------------------------------------------------------

    private suspend fun ensureCoverage(
        quotes: List<String>,
        days: Int,
        label: String
    ): Result<Unit> {
        val today = LocalDate.now()
        val desiredStart = today.minusDays(days.toLong())
        val freshCutoff = today.minusDays(OVERLAP_DAYS.toLong())

        // The window has to cover every quote involved in the chart, so take the
        // earliest date any of them is still missing.
        var from: LocalDate? = null
        for (quote in quotes) {
            val state = syncStateDao.get(BASE_CURRENCY, quote)
            val quoteFrom = missingFrom(
                coveredStart = state?.coveredStartDate?.let(::parseDate),
                coveredEnd = state?.coveredEndDate?.let(::parseDate),
                desiredStart = desiredStart,
                freshCutoff = freshCutoff
            ) ?: continue
            from = minOf(from ?: quoteFrom, quoteFrom)
        }

        if (from == null) {
            Log.d(TAG, "$label history up to date for quotes=$quotes days=$days")
            return Result.success(Unit)
        }
        if (from.isAfter(today)) return Result.success(Unit)

        return runChunks(from, today, requestedQuotes = quotes, label = label)
    }

    /**
     * Returns the earliest date that still needs fetching for a single quote, or
     * null when the quote is already covered for the requested window and fresh.
     */
    private fun missingFrom(
        coveredStart: LocalDate?,
        coveredEnd: LocalDate?,
        desiredStart: LocalDate,
        freshCutoff: LocalDate
    ): LocalDate? {
        // Enough depth for the requested window, and the tail is recent enough.
        val hasDepth = coveredStart != null && !coveredStart.isAfter(desiredStart)
        val fresh = coveredEnd != null && !coveredEnd.isBefore(freshCutoff)
        if (hasDepth && fresh) return null

        return when {
            // Never downloaded, or the cached history starts too late for this window.
            coveredStart == null || coveredStart.isAfter(desiredStart) -> desiredStart
            // Defensive: coverage always records both bounds together.
            coveredEnd == null -> desiredStart
            // Otherwise only the tail is stale; re-fetch it with a small overlap so
            // late publications or corrections are picked up.
            else -> coveredEnd.minusDays(OVERLAP_DAYS.toLong())
        }
    }

    /**
     * Downloads [from]..[to] in bounded chunks, in chronological order, stopping at
     * the first failure. Each successful chunk is upserted and advances coverage, so
     * a partially completed backfill resumes where it left off and never loses data.
     */
    private suspend fun runChunks(
        from: LocalDate,
        to: LocalDate,
        requestedQuotes: List<String>,
        label: String
    ): Result<Unit> {
        val startedAt = System.currentTimeMillis()
        var chunkStart = from
        var stored = 0

        while (!chunkStart.isAfter(to)) {
            val chunkEnd = minOf(chunkStart.plusDays(MAX_CHUNK_DAYS.toLong() - 1), to)
            val chunkFrom = chunkStart.format(DATE_FORMAT)
            val chunkTo = chunkEnd.format(DATE_FORMAT)
            val windowDays = ChronoUnit.DAYS.between(chunkStart, chunkEnd) + 1
            // Only a short incremental window may legitimately come back empty (no
            // new publications). A real backfill always expects observations.
            val allowEmpty = windowDays <= EMPTY_TOLERANCE_DAYS
            val minRecords = ExchangeRateResponseValidator.scaledMinRecords(windowDays, minHistoricalRecords)

            fetchChunk(requestedQuotes, chunkFrom, chunkTo, minRecords, allowEmpty)
                .onFailure { error ->
                    Log.w(TAG, "$label history stopped: window=$chunkFrom..$chunkTo reason=${describe(error)}")
                    return Result.failure(error)
                }
                .onSuccess { count -> stored += count }

            chunkStart = chunkEnd.plusDays(1)
        }

        Log.d(
            TAG,
            "$label history synced: window=${from.format(DATE_FORMAT)}..${to.format(DATE_FORMAT)} " +
                "quotes=${requestedQuotes.ifEmpty { listOf("all") }} stored=$stored " +
                "duration=${System.currentTimeMillis() - startedAt}ms"
        )
        return Result.success(Unit)
    }

    /** Fetches one window, validates it, upserts the rows and extends coverage. */
    private suspend fun fetchChunk(
        requestedQuotes: List<String>,
        from: String,
        to: String,
        minRecords: Int,
        allowEmpty: Boolean
    ): Result<Int> {
        val response = try {
            frankfurterApi.getHistoricalRates(
                baseCurrency = BASE_CURRENCY,
                targetCurrencies = requestedQuotes.takeIf { it.isNotEmpty() }?.joinToString(","),
                startDate = from,
                endDate = to
            )
        } catch (e: java.io.IOException) {
            return Result.failure(SyncErrorException(SyncError.Network))
        } catch (e: Exception) {
            return Result.failure(e)
        }

        if (!response.isSuccessful) {
            return Result.failure(mapHttpError(response.code()))
        }

        val body = response.body()
            ?: return Result.failure(SyncErrorException(SyncError.EmptyResponse))

        responseValidator.validateHistoricalRange(
            body, BASE_CURRENCY, from, to, minRecords, allowEmpty
        ).getOrElse { error -> return Result.failure(error) }

        val entities = body
            .filterNot { it.quote == BASE_CURRENCY }
            .map { item ->
                HistoricalRateEntity(
                    baseCurrency = item.base,
                    targetCurrency = item.quote,
                    rate = item.rate,
                    date = item.date
                )
            }
        // Primary key (base, target, date) + REPLACE makes this an upsert, so
        // overlapping or repeated responses never create duplicates.
        historicalRateDao.insertRates(entities)

        // Coverage is extended for every requested quote (or the returned ones for
        // an all-quotes fetch) so a currency absent from part of the window is not
        // re-downloaded on every sync.
        val coveredQuotes = requestedQuotes.ifEmpty {
            entities.map { it.targetCurrency }.distinct()
        }
        val now = System.currentTimeMillis()
        coveredQuotes.forEach { quote ->
            val current = syncStateDao.get(BASE_CURRENCY, quote)
            syncStateDao.upsert(
                HistoricalSyncStateEntity(
                    baseCurrency = BASE_CURRENCY,
                    targetCurrency = quote,
                    coveredStartDate = earlierOf(current?.coveredStartDate, from),
                    coveredEndDate = laterOf(current?.coveredEndDate, to),
                    lastSuccessfulSync = now
                )
            )
        }

        return Result.success(entities.size)
    }

    private fun mapHttpError(code: Int): Exception =
        if (code >= 500) SyncErrorException(SyncError.Server) else SyncErrorException(SyncError.Http(code))

    private fun describe(error: Throwable): String =
        (error as? SyncErrorException)?.syncError?.toString() ?: error.toString()

    private fun parseDate(value: String): LocalDate? =
        try {
            LocalDate.parse(value)
        } catch (e: Exception) {
            null
        }

    companion object {
        private const val BASE_CURRENCY = ExchangeRateRepositoryImpl.BASE_CURRENCY

        /** Days of history kept fresh by the bootstrap / periodic sync. */
        const val WARM_SYNC_DAYS = 90

        /** Re-fetch the most recent window even if it looks covered, to catch late publications. */
        private const val OVERLAP_DAYS = 7L

        /** Bounds a single request so a multi-year range is never fetched in one call. */
        private const val MAX_CHUNK_DAYS = 366L

        /** Windows up to this size may come back empty without being an error. */
        private const val EMPTY_TOLERANCE_DAYS = 14L

        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private const val TAG = "HistoricalRateSyncer"
    }
}

private fun earlierOf(a: String?, b: String): String = if (a == null) b else minOf(a, b)
private fun laterOf(a: String?, b: String): String = if (a == null) b else maxOf(a, b)
