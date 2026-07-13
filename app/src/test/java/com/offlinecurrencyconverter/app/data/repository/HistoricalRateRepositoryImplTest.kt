package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoricalRateRepositoryImplTest {

    private lateinit var historicalRateDao: HistoricalRateDao
    private lateinit var repository: HistoricalRateRepositoryImpl

    @Before
    fun setup() {
        historicalRateDao = mockk(relaxed = true)
        repository = HistoricalRateRepositoryImpl(historicalRateDao)
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
}
