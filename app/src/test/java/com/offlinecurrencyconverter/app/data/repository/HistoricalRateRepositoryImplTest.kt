package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalSyncStateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalSyncStateEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalRateRepositoryImplTest {

    private lateinit var historicalRateDao: HistoricalRateDao
    private lateinit var historicalRateSyncer: HistoricalRateSyncer
    private lateinit var syncStateDao: HistoricalSyncStateDao
    private lateinit var repository: HistoricalRateRepositoryImpl

    @Before
    fun setup() {
        historicalRateDao = mockk(relaxed = true)
        historicalRateSyncer = mockk(relaxed = true)
        syncStateDao = mockk(relaxed = true)
        coEvery { historicalRateSyncer.ensureHistory(any(), any(), any()) } returns Result.success(Unit)
        repository = HistoricalRateRepositoryImpl(historicalRateDao, historicalRateSyncer, syncStateDao)
    }

    @Test
    fun `getHistoricalRates returns direct EUR-based rates`() = runTest {
        val entities = listOf(
            HistoricalRateEntity("EUR", "USD", 1.09, "2024-01-01"),
            HistoricalRateEntity("EUR", "USD", 1.08, "2024-01-02")
        )
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(entities)

        val result = repository.getHistoricalRates("EUR", "USD")

        assertEquals(2, result.size)
        assertEquals("2024-01-01", result[0].date)
        assertEquals(1.09, result[0].rate, 0.001)
    }

    @Test
    fun `getHistoricalRates returns empty list when no data`() = runTest {
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(emptyList())

        val result = repository.getHistoricalRates("EUR", "USD")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getHistoricalRates returns empty list for same currency`() = runTest {
        val result = repository.getHistoricalRates("USD", "USD")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getHistoricalRates calculates cross-rate USD to JPY`() = runTest {
        val eurToUsd = listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-01"),
            HistoricalRateEntity("EUR", "USD", 1.08, "2024-01-02")
        )
        val eurToJpy = listOf(
            HistoricalRateEntity("EUR", "JPY", 165.0, "2024-01-01"),
            HistoricalRateEntity("EUR", "JPY", 162.0, "2024-01-02")
        )
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(eurToUsd)
        coEvery { historicalRateDao.getHistoricalRates("EUR", "JPY") } returns flowOf(eurToJpy)

        val result = repository.getHistoricalRates("USD", "JPY")

        assertEquals(2, result.size)
        assertEquals(165.0 / 1.10, result[0].rate, 0.01)
        assertEquals(162.0 / 1.08, result[1].rate, 0.01)
        assertEquals("USD", result[0].baseCurrency)
        assertEquals("JPY", result[0].targetCurrency)
        assertEquals("2024-01-01", result[0].date)
        assertEquals("2024-01-02", result[1].date)
    }

    @Test
    fun `getHistoricalRates returns empty when EUR to base has no data`() = runTest {
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(emptyList())
        coEvery { historicalRateDao.getHistoricalRates("EUR", "JPY") } returns flowOf(
            listOf(HistoricalRateEntity("EUR", "JPY", 165.0, "2024-01-01"))
        )

        val result = repository.getHistoricalRates("USD", "JPY")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getHistoricalRates returns empty when EUR to target has no data`() = runTest {
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(
            listOf(HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-01"))
        )
        coEvery { historicalRateDao.getHistoricalRates("EUR", "JPY") } returns flowOf(emptyList())

        val result = repository.getHistoricalRates("USD", "JPY")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getHistoricalRates only includes dates present in both pairs`() = runTest {
        val eurToUsd = listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-01"),
            HistoricalRateEntity("EUR", "USD", 1.08, "2024-01-03")
        )
        val eurToGbp = listOf(
            HistoricalRateEntity("EUR", "GBP", 0.86, "2024-01-01"),
            HistoricalRateEntity("EUR", "GBP", 0.85, "2024-01-02")
        )
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(eurToUsd)
        coEvery { historicalRateDao.getHistoricalRates("EUR", "GBP") } returns flowOf(eurToGbp)

        val result = repository.getHistoricalRates("USD", "GBP")

        assertEquals(1, result.size)
        assertEquals("2024-01-01", result[0].date)
        assertEquals(0.86 / 1.10, result[0].rate, 0.01)
    }

    @Test
    fun `getHistoricalRates calculates inverse rate when target is EUR`() = runTest {
        val eurToUsd = listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-01"),
            HistoricalRateEntity("EUR", "USD", 1.08, "2024-01-02")
        )
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(eurToUsd)

        val result = repository.getHistoricalRates("USD", "EUR")

        assertEquals(2, result.size)
        assertEquals(1.0 / 1.10, result[0].rate, 0.01)
        assertEquals(1.0 / 1.08, result[1].rate, 0.01)
        assertEquals("USD", result[0].baseCurrency)
        assertEquals("EUR", result[0].targetCurrency)
        assertEquals("2024-01-01", result[0].date)
        assertEquals("2024-01-02", result[1].date)
    }

    @Test
    fun `getHistoricalRates returns empty when target is EUR and no EUR data`() = runTest {
        coEvery { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(emptyList())

        val result = repository.getHistoricalRates("USD", "EUR")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeHistoricalRates emits direct EUR-based rates`() = runTest {
        val entities = listOf(
            HistoricalRateEntity("EUR", "USD", 1.09, "2024-01-01"),
            HistoricalRateEntity("EUR", "USD", 1.08, "2024-01-02")
        )
        every { historicalRateDao.getHistoricalRates("EUR", "USD") } returns flowOf(entities)

        val result = repository.observeHistoricalRates("EUR", "USD").first()

        assertEquals(2, result.size)
        assertEquals("2024-01-01", result[0].date)
    }

    @Test
    fun `observeHistoricalRates returns empty flow for same currency`() = runTest {
        val result = repository.observeHistoricalRates("USD", "USD").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeHistoricalRates recalculates cross-rate when dao emits`() = runTest {
        val eurToUsd = MutableStateFlow(
            listOf(HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-01"))
        )
        val eurToJpy = MutableStateFlow(
            listOf(HistoricalRateEntity("EUR", "JPY", 165.0, "2024-01-01"))
        )
        every { historicalRateDao.getHistoricalRates("EUR", "USD") } returns eurToUsd
        every { historicalRateDao.getHistoricalRates("EUR", "JPY") } returns eurToJpy

        val emissions = mutableListOf<List<HistoricalRateEntity>>()
        val job = launch {
            repository.observeHistoricalRates("USD", "JPY").collect { emissions.add(it) }
        }
        advanceUntilIdle()

        eurToJpy.value = listOf(
            HistoricalRateEntity("EUR", "JPY", 180.0, "2024-01-01"),
            HistoricalRateEntity("EUR", "JPY", 181.0, "2024-01-02")
        )
        eurToUsd.value = listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-01"),
            HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-02")
        )
        advanceUntilIdle()
        job.cancel()

        assertEquals(165.0 / 1.10, emissions.first()[0].rate, 0.01)
        assertEquals(180.0 / 1.10, emissions.last()[0].rate, 0.01)
        assertEquals(2, emissions.last().size)
    }

    @Test
    fun `observeHistoricalRates recalculates inverse when dao emits`() = runTest {
        val eurToUsd = MutableStateFlow(
            listOf(HistoricalRateEntity("EUR", "USD", 1.10, "2024-01-01"))
        )
        every { historicalRateDao.getHistoricalRates("EUR", "USD") } returns eurToUsd

        val emissions = mutableListOf<List<HistoricalRateEntity>>()
        val job = launch {
            repository.observeHistoricalRates("USD", "EUR").collect { emissions.add(it) }
        }
        advanceUntilIdle()

        eurToUsd.value = listOf(HistoricalRateEntity("EUR", "USD", 1.25, "2024-01-01"))
        advanceUntilIdle()
        job.cancel()

        assertEquals(1.0 / 1.10, emissions.first()[0].rate, 0.01)
        assertEquals(1.0 / 1.25, emissions.last()[0].rate, 0.01)
    }

    @Test
    fun `ensureHistory delegates to the syncer`() = runTest {
        val result = repository.ensureHistory("USD", "EUR", 1825)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `ensureHistory propagates syncer failure`() = runTest {
        coEvery { historicalRateSyncer.ensureHistory(any(), any(), any()) } returns
            Result.failure(RuntimeException("no network"))

        val result = repository.ensureHistory("USD", "EUR", 365)

        assertTrue(result.isFailure)
    }

    @Test
    fun `coveredHistoryDays reports the depth of a direct EUR pair`() = runTest {
        coEvery { syncStateDao.get("EUR", "USD") } returns coverage(29)

        assertEquals(30, repository.coveredHistoryDays("EUR", "USD"))
        assertEquals(30, repository.coveredHistoryDays("USD", "EUR"))
    }

    @Test
    fun `coveredHistoryDays is 0 when the pair was never downloaded`() = runTest {
        coEvery { syncStateDao.get("EUR", "USD") } returns null

        assertEquals(0, repository.coveredHistoryDays("EUR", "USD"))
    }

    @Test
    fun `coveredHistoryDays is limited by the shallower side of a cross pair`() = runTest {
        coEvery { syncStateDao.get("EUR", "USD") } returns coverage(179)
        coEvery { syncStateDao.get("EUR", "GBP") } returns coverage(29)

        // The chart joins both series by date, so the usable depth is the smaller one.
        assertEquals(30, repository.coveredHistoryDays("USD", "GBP"))
    }

    @Test
    fun `coveredHistoryDays is 0 for pairs without stored history`() = runTest {
        assertEquals(0, repository.coveredHistoryDays("USD", "USD"))
        assertEquals(0, repository.coveredHistoryDays("EUR", "EUR"))
    }

    private fun coverage(startDaysAgo: Long): HistoricalSyncStateEntity =
        HistoricalSyncStateEntity(
            baseCurrency = "EUR",
            targetCurrency = "USD",
            coveredStartDate = LocalDate.now().minusDays(startDaysAgo).toString(),
            coveredEndDate = LocalDate.now().toString(),
            lastSuccessfulSync = System.currentTimeMillis()
        )
}
