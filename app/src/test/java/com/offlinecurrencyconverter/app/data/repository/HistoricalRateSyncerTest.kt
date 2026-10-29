package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalSyncStateDao
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalCoverageAggregate
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalSyncStateEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.data.remote.dto.ExchangeRateItem
import com.offlinecurrencyconverter.app.domain.validation.ExchangeRateResponseValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class HistoricalRateSyncerTest {

    private lateinit var historicalRateDao: HistoricalRateDao
    private lateinit var syncStateDao: HistoricalSyncStateDao
    private lateinit var frankfurterApi: FrankfurterApi
    private lateinit var responseValidator: ExchangeRateResponseValidator
    private lateinit var syncer: HistoricalRateSyncer

    private val storedChunks = mutableListOf<List<HistoricalRateEntity>>()
    private val coverageUpdates = mutableListOf<HistoricalSyncStateEntity>()

    private val today: LocalDate = LocalDate.now()

    @Before
    fun setup() {
        historicalRateDao = mockk(relaxed = true)
        syncStateDao = mockk(relaxed = true)
        frankfurterApi = mockk(relaxed = true)
        responseValidator = mockk()

        storedChunks.clear()
        coverageUpdates.clear()

        coEvery { historicalRateDao.insertRates(any()) } answers { storedChunks += arg<List<HistoricalRateEntity>>(0) }
        coEvery { syncStateDao.upsert(any()) } answers { coverageUpdates += arg<HistoricalSyncStateEntity>(0) }
        coEvery {
            responseValidator.validateHistoricalRange(any(), any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        syncer = HistoricalRateSyncer(
            historicalRateDao,
            syncStateDao,
            frankfurterApi,
            responseValidator,
            30
        )
    }

    private fun items(vararg dates: LocalDate, quote: String = "USD"): List<ExchangeRateItem> =
        dates.map { ExchangeRateItem(it.toString(), "EUR", quote, 1.09) }

    // ------------------------------------------------------------------
    // Warm (bootstrap / periodic) sync
    // ------------------------------------------------------------------

    @Test
    fun `warm sync on first run fetches the window, filters self-row and records coverage`() = runTest {
        coEvery { syncStateDao.getCoverageAggregate("EUR") } returns null
        val response = items(today.minusDays(10), today, quote = "USD") +
            items(today, quote = "GBP") +
            listOf(ExchangeRateItem(today.toString(), "EUR", "EUR", 1.0))
        coEvery { frankfurterApi.getHistoricalRates("EUR", null, any(), any()) } returns Response.success(response)

        val result = syncer.syncRecentHistory()

        assertTrue(result.isSuccess)
        val stored = storedChunks.flatten()
        assertTrue(stored.isNotEmpty())
        assertTrue(stored.none { it.targetCurrency == "EUR" })
        // Coverage is recorded for both non-EUR quotes that were returned.
        val coveredQuotes = coverageUpdates.map { it.targetCurrency }.toSet()
        assertTrue(coveredQuotes.containsAll(setOf("USD", "GBP")))
    }

    @Test
    fun `warm sync skips the network when coverage is already fresh`() = runTest {
        coEvery { syncStateDao.getCoverageAggregate("EUR") } returns HistoricalCoverageAggregate(
            minStart = today.minusDays(HistoricalRateSyncer.WARM_SYNC_DAYS.toLong()).toString(),
            maxEnd = today.toString()
        )

        val result = syncer.syncRecentHistory()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { frankfurterApi.getHistoricalRates(any(), any(), any(), any()) }
    }

    @Test
    fun `warm sync refreshes only the stale tail, not the whole window`() = runTest {
        val coveredEnd = today.minusDays(20)
        coEvery { syncStateDao.getCoverageAggregate("EUR") } returns HistoricalCoverageAggregate(
            minStart = today.minusDays(HistoricalRateSyncer.WARM_SYNC_DAYS.toLong()).toString(),
            maxEnd = coveredEnd.toString()
        )
        val windows = mutableListOf<Pair<String, String>>()
        coEvery { frankfurterApi.getHistoricalRates(any(), any(), any(), any()) } answers {
            windows += arg<String>(2) to arg<String>(3)
            Response.success(items(today))
        }

        val result = syncer.syncRecentHistory()

        assertTrue(result.isSuccess)
        // The request starts just before the stale end (with overlap), not at day 0.
        assertEquals(coveredEnd.minusDays(7).toString(), windows.first().first)
        assertEquals(today.toString(), windows.first().second)
    }

    // ------------------------------------------------------------------
    // Deep (on-demand) backfill
    // ------------------------------------------------------------------

    @Test
    fun `deep backfill downloads history once and a narrower range reuses it`() = runTest {
        coEvery { syncStateDao.get("EUR", "USD") } returns null
        var apiCalls = 0
        coEvery { frankfurterApi.getHistoricalRates(eq("EUR"), eq("USD"), any(), any()) } answers {
            apiCalls++
            Response.success(items(today.minusDays(1825), today))
        }

        val first = syncer.ensureHistory("USD", "EUR", 1825)
        assertTrue(first.isSuccess)
        val callsAfterBackfill = apiCalls
        assertTrue(callsAfterBackfill > 0)

        // Now the pair has full coverage; a 1Y request must not re-download.
        coEvery { syncStateDao.get("EUR", "USD") } returns HistoricalSyncStateEntity(
            baseCurrency = "EUR",
            targetCurrency = "USD",
            coveredStartDate = today.minusDays(1825).toString(),
            coveredEndDate = today.toString(),
            lastSuccessfulSync = 0L
        )
        val second = syncer.ensureHistory("USD", "EUR", 365)
        assertTrue(second.isSuccess)

        assertEquals(callsAfterBackfill, apiCalls)
    }

    @Test
    fun `widening from 1Y to 5Y backfills the older range`() = runTest {
        coEvery { syncStateDao.get("EUR", "USD") } returns HistoricalSyncStateEntity(
            baseCurrency = "EUR",
            targetCurrency = "USD",
            coveredStartDate = today.minusDays(365).toString(),
            coveredEndDate = today.toString(),
            lastSuccessfulSync = 0L
        )
        val windows = mutableListOf<Pair<String, String>>()
        coEvery { frankfurterApi.getHistoricalRates(eq("EUR"), eq("USD"), any(), any()) } answers {
            windows += arg<String>(2) to arg<String>(3)
            Response.success(items(today.minusDays(1825)))
        }

        syncer.ensureHistory("USD", "EUR", 1825)

        assertTrue(windows.isNotEmpty())
        assertEquals(today.minusDays(1825).toString(), windows.first().first)
    }

    @Test
    fun `a multi-year range is split into bounded chunks`() = runTest {
        coEvery { syncStateDao.get("EUR", "USD") } returns null
        val windows = mutableListOf<Pair<String, String>>()
        coEvery { frankfurterApi.getHistoricalRates(eq("EUR"), eq("USD"), any(), any()) } answers {
            windows += arg<String>(2) to arg<String>(3)
            Response.success(items(today.minusDays(1825)))
        }

        syncer.ensureHistory("USD", "EUR", 1825)

        assertTrue(windows.size > 1)
        windows.forEach { (from, to) ->
            val span = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.parse(from), LocalDate.parse(to)
            ) + 1
            assertTrue("chunk $from..$to exceeds the bound", span <= 366)
        }
        // Chunks are contiguous and end at today.
        assertEquals(today.toString(), windows.last().second)
    }

    @Test
    fun `failed backfill propagates the error and stores nothing`() = runTest {
        coEvery { syncStateDao.get("EUR", "USD") } returns null
        coEvery { frankfurterApi.getHistoricalRates(eq("EUR"), eq("USD"), any(), any()) } throws
            IOException("offline")

        val result = syncer.ensureHistory("USD", "EUR", 1825)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { historicalRateDao.insertRates(any()) }
        coVerify(exactly = 0) { syncStateDao.upsert(any()) }
    }

    @Test
    fun `an interrupted backfill never claims the years it has not downloaded`() = runTest {
        // The bootstrap warmed only the tail before the 5 year backfill started.
        coEvery { syncStateDao.get("EUR", "USD") } returns HistoricalSyncStateEntity(
            baseCurrency = "EUR",
            targetCurrency = "USD",
            coveredStartDate = today.minusDays(90).toString(),
            coveredEndDate = today.toString(),
            lastSuccessfulSync = 0L
        )
        var calls = 0
        coEvery { frankfurterApi.getHistoricalRates(eq("EUR"), eq("USD"), any(), any()) } answers {
            calls++
            if (calls > 1) throw IOException("offline")
            Response.success(items(LocalDate.parse(arg<String>(2))))
        }

        val result = syncer.ensureHistory("USD", "EUR", 1825)

        assertTrue(result.isFailure)
        val windowStart = today.minusDays(1825)
        val covered = coverageUpdates.last()
        assertEquals(windowStart.toString(), covered.coveredStartDate)
        // Only the first chunk is covered: the record must not reach across the
        // years that were skipped when the download stopped.
        assertEquals(windowStart.plusDays(365).toString(), covered.coveredEndDate)
    }

    @Test
    fun `an interrupted backfill resumes from where it stopped`() = runTest {
        val windowStart = today.minusDays(1825)
        val stoppedAt = windowStart.plusDays(365)
        coEvery { syncStateDao.get("EUR", "USD") } returns HistoricalSyncStateEntity(
            baseCurrency = "EUR",
            targetCurrency = "USD",
            coveredStartDate = windowStart.toString(),
            coveredEndDate = stoppedAt.toString(),
            lastSuccessfulSync = 0L
        )
        val windows = mutableListOf<Pair<String, String>>()
        coEvery { frankfurterApi.getHistoricalRates(eq("EUR"), eq("USD"), any(), any()) } answers {
            windows += arg<String>(2) to arg<String>(3)
            Response.success(items(LocalDate.parse(arg<String>(2))))
        }

        val result = syncer.ensureHistory("USD", "EUR", 1825)

        assertTrue(result.isSuccess)
        // The hole after the last covered date is downloaded, with the usual overlap.
        assertEquals(stoppedAt.minusDays(7).toString(), windows.first().first)
        assertEquals(today.toString(), windows.last().second)
    }

    @Test
    fun `a completed backfill covers the whole range including the earlier warm tail`() = runTest {
        var coverage = HistoricalSyncStateEntity(
            baseCurrency = "EUR",
            targetCurrency = "USD",
            coveredStartDate = today.minusDays(90).toString(),
            coveredEndDate = today.toString(),
            lastSuccessfulSync = 0L
        )
        coEvery { syncStateDao.get("EUR", "USD") } answers { coverage }
        coEvery { syncStateDao.upsert(any()) } answers {
            val entity = arg<HistoricalSyncStateEntity>(0)
            coverageUpdates += entity
            coverage = entity
        }
        coEvery { frankfurterApi.getHistoricalRates(eq("EUR"), eq("USD"), any(), any()) } answers {
            Response.success(items(LocalDate.parse(arg<String>(2))))
        }

        val result = syncer.ensureHistory("USD", "EUR", 1825)

        assertTrue(result.isSuccess)
        // Each chunk joined the previous one, so the warm tail ends up included.
        assertEquals(today.minusDays(1825).toString(), coverage.coveredStartDate)
        assertEquals(today.toString(), coverage.coveredEndDate)
    }

    // ------------------------------------------------------------------
    // Trivial pairs
    // ------------------------------------------------------------------

    @Test
    fun `ensureHistory with the same currency does nothing`() = runTest {
        val result = syncer.ensureHistory("USD", "USD", 1825)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { frankfurterApi.getHistoricalRates(any(), any(), any(), any()) }
    }

    @Test
    fun `ensureHistory with EUR on both sides does nothing`() = runTest {
        val result = syncer.ensureHistory("EUR", "EUR", 365)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { frankfurterApi.getHistoricalRates(any(), any(), any(), any()) }
    }
}
