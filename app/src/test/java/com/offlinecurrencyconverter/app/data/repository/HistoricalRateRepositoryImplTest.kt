package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.data.remote.dto.ExchangeRateItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class HistoricalRateRepositoryImplTest {

    private lateinit var historicalRateDao: HistoricalRateDao
    private lateinit var frankfurterApi: FrankfurterApi
    private lateinit var repository: HistoricalRateRepositoryImpl

    @Before
    fun setup() {
        historicalRateDao = mockk(relaxed = true)
        frankfurterApi = mockk(relaxed = true)
        repository = HistoricalRateRepositoryImpl(historicalRateDao, frankfurterApi)
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

    @Test
    fun `fetchAndStoreHistoricalRates fetches from API and stores in dao`() = runTest {
        val rateItems = listOf(
            ExchangeRateItem("2024-01-01", "USD", "EUR", 0.92),
            ExchangeRateItem("2024-01-02", "USD", "EUR", 0.93)
        )
        coEvery { frankfurterApi.getHistoricalRates("USD", "EUR", any(), any()) } returns Response.success(rateItems)

        val result = repository.fetchAndStoreHistoricalRates("USD", "EUR", 30)

        assertTrue(result.isSuccess)
        coVerify { historicalRateDao.deleteRates("USD", "EUR") }
        coVerify { historicalRateDao.insertRates(any()) }
    }

    @Test
    fun `fetchAndStoreHistoricalRates handles API failure`() = runTest {
        coEvery { frankfurterApi.getHistoricalRates("USD", "EUR", any(), any()) } returns Response.error(500, mockk(relaxed = true))

        val result = repository.fetchAndStoreHistoricalRates("USD", "EUR", 30)

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchAndStoreHistoricalRates handles network exception`() = runTest {
        coEvery { frankfurterApi.getHistoricalRates("USD", "EUR", any(), any()) } throws java.io.IOException("No connection")

        val result = repository.fetchAndStoreHistoricalRates("USD", "EUR", 30)

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchAndStoreHistoricalRates stores entities with correct fields`() = runTest {
        val rateItems = listOf(
            ExchangeRateItem("2024-01-15", "EUR", "USD", 1.0873)
        )
        coEvery { frankfurterApi.getHistoricalRates("EUR", "USD", any(), any()) } returns Response.success(rateItems)

        repository.fetchAndStoreHistoricalRates("EUR", "USD", 30)

        coVerify {
            historicalRateDao.insertRates(match { entities ->
                entities.size == 1 &&
                entities[0].baseCurrency == "EUR" &&
                entities[0].targetCurrency == "USD" &&
                entities[0].rate == 1.0873 &&
                entities[0].date == "2024-01-15"
            })
        }
    }
}
