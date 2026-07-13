package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
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
    fun `getHistoricalRates returns flow from dao`() = runTest {
        val entities = listOf(
            HistoricalRateEntity("USD", "EUR", 0.92, "2024-01-01"),
            HistoricalRateEntity("USD", "EUR", 0.93, "2024-01-02")
        )
        coEvery { historicalRateDao.getHistoricalRates("USD", "EUR") } returns flowOf(entities)

        val result = repository.getHistoricalRates("USD", "EUR").first()

        assertEquals(2, result.size)
        assertEquals("2024-01-01", result[0].date)
        assertEquals(0.92, result[0].rate, 0.001)
    }

    @Test
    fun `getHistoricalRates returns empty flow when no data`() = runTest {
        coEvery { historicalRateDao.getHistoricalRates("USD", "EUR") } returns flowOf(emptyList())

        val result = repository.getHistoricalRates("USD", "EUR").first()

        assertTrue(result.isEmpty())
    }
}
