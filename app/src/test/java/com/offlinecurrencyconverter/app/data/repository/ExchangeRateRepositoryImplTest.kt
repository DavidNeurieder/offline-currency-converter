package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.ExchangeRateDao
import com.offlinecurrencyconverter.app.data.local.entity.ExchangeRateEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.data.remote.dto.ExchangeRateItem
import com.offlinecurrencyconverter.app.domain.model.SyncError
import com.offlinecurrencyconverter.app.domain.model.SyncErrorException
import com.offlinecurrencyconverter.app.domain.validation.ExchangeRateResponseValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ExchangeRateRepositoryImplTest {

    private lateinit var exchangeRateDao: ExchangeRateDao
    private lateinit var historicalRateSyncer: HistoricalRateSyncer
    private lateinit var frankfurterApi: FrankfurterApi
    private lateinit var responseValidator: ExchangeRateResponseValidator
    private lateinit var repository: ExchangeRateRepositoryImpl

    @Before
    fun setup() {
        exchangeRateDao = mockk(relaxed = true)
        historicalRateSyncer = mockk(relaxed = true)
        frankfurterApi = mockk(relaxed = true)
        responseValidator = mockk()
        coEvery { responseValidator.validateLatest(any(), any(), any()) } returns Result.success(Unit)
        coEvery { historicalRateSyncer.syncRecentHistory(any()) } returns Result.success(Unit)
        repository = ExchangeRateRepositoryImpl(
            exchangeRateDao,
            frankfurterApi,
            responseValidator,
            historicalRateSyncer
        )
    }

    private fun createEntity(
        baseCurrency: String,
        targetCurrency: String,
        rate: Double,
        isOfflineAvailable: Boolean = true,
        lastUpdated: Long = System.currentTimeMillis()
    ): ExchangeRateEntity = ExchangeRateEntity(
        baseCurrency = baseCurrency,
        targetCurrency = targetCurrency,
        rate = rate,
        isOfflineAvailable = isOfflineAvailable,
        lastUpdated = lastUpdated
    )

    @Test
    fun `getRate returns 1_0 for same currency`() = runTest {
        val result = repository.getRate("USD", "USD")

        assertNotNull(result)
        assertEquals(1.0, result!!.rate, 0.0)
        assertEquals("USD", result.baseCurrency)
        assertEquals("USD", result.targetCurrency)
    }

    @Test
    fun `getRate returns direct rate when available`() = runTest {
        val eurToUsd = createEntity("EUR", "USD", 1.09)
        coEvery { exchangeRateDao.getRate("EUR", "USD") } returns eurToUsd

        val result = repository.getRate("EUR", "USD")

        assertNotNull(result)
        assertEquals(1.09, result!!.rate, 0.0001)
        assertEquals("EUR", result.baseCurrency)
        assertEquals("USD", result.targetCurrency)
    }

    @Test
    fun `getRate calculates cross-rate via EUR when direct not available`() = runTest {
        val currentTime = System.currentTimeMillis()
        val eurToUsd = createEntity("EUR", "USD", 1.09, lastUpdated = currentTime)
        val eurToJpy = createEntity("EUR", "JPY", 163.45, lastUpdated = currentTime + 1000)
        
        coEvery { exchangeRateDao.getRate("EUR", "EUR") } returns null
        coEvery { exchangeRateDao.getRate("USD", "JPY") } returns null
        coEvery { exchangeRateDao.getRate("EUR", "USD") } returns eurToUsd
        coEvery { exchangeRateDao.getRate("EUR", "JPY") } returns eurToJpy

        val result = repository.getRate("USD", "JPY")

        assertNotNull(result)
        val expectedRate = 163.45 / 1.09
        assertEquals(expectedRate, result!!.rate, 0.01)
        assertEquals("USD", result.baseCurrency)
        assertEquals("JPY", result.targetCurrency)
        assertEquals(currentTime + 1000, result.lastUpdated)
        assertTrue(result.isOfflineAvailable)
    }

    @Test
    fun `getRate returns null when no rates available`() = runTest {
        coEvery { exchangeRateDao.getRate(any(), any()) } returns null

        val result = repository.getRate("USD", "JPY")

        assertNull(result)
    }

    @Test
    fun `getRate handles EUR as base with direct rate`() = runTest {
        val eurToJpy = createEntity("EUR", "JPY", 163.45)
        coEvery { exchangeRateDao.getRate("EUR", "EUR") } returns null
        coEvery { exchangeRateDao.getRate("EUR", "JPY") } returns eurToJpy

        val result = repository.getRate("EUR", "JPY")

        assertNotNull(result)
        assertEquals(163.45, result!!.rate, 0.0001)
        assertEquals("EUR", result.baseCurrency)
        assertEquals("JPY", result.targetCurrency)
    }

    @Test
    fun `getRate handles EUR as target with inverse rate`() = runTest {
        val eurToUsd = createEntity("EUR", "USD", 1.09)
        coEvery { exchangeRateDao.getRate("EUR", "EUR") } returns null
        coEvery { exchangeRateDao.getRate("USD", "EUR") } returns null
        coEvery { exchangeRateDao.getRate("EUR", "USD") } returns eurToUsd

        val result = repository.getRate("USD", "EUR")

        assertNotNull(result)
        val expectedRate = 1.0 / 1.09
        assertEquals(expectedRate, result!!.rate, 0.0001)
        assertEquals("USD", result.baseCurrency)
        assertEquals("EUR", result.targetCurrency)
    }

    @Test
    fun `getRate preserves offline availability in cross-rate`() = runTest {
        val eurToUsd = createEntity("EUR", "USD", 1.09, isOfflineAvailable = true)
        val eurToJpy = createEntity("EUR", "JPY", 163.45, isOfflineAvailable = false)
        
        coEvery { exchangeRateDao.getRate("EUR", "EUR") } returns null
        coEvery { exchangeRateDao.getRate("USD", "JPY") } returns null
        coEvery { exchangeRateDao.getRate("EUR", "USD") } returns eurToUsd
        coEvery { exchangeRateDao.getRate("EUR", "JPY") } returns eurToJpy

        val result = repository.getRate("USD", "JPY")

        assertNotNull(result)
        assertTrue(!result!!.isOfflineAvailable)
    }

    @Test
    fun `getRate rejects invalid direct rate`() = runTest {
        coEvery { exchangeRateDao.getRate("EUR", "EUR") } returns null
        coEvery { exchangeRateDao.getRate("EUR", "USD") } returns createEntity("EUR", "USD", Double.NaN)
        coEvery { exchangeRateDao.getRate("USD", "EUR") } returns null

        val result = repository.getRate("EUR", "USD")

        assertNull(result)
    }

    @Test
    fun `getRate rejects invalid cross-rate components`() = runTest {
        coEvery { exchangeRateDao.getRate("EUR", "EUR") } returns null
        coEvery { exchangeRateDao.getRate("USD", "JPY") } returns null
        coEvery { exchangeRateDao.getRate("EUR", "USD") } returns createEntity("EUR", "USD", 0.0)
        coEvery { exchangeRateDao.getRate("EUR", "JPY") } returns createEntity("EUR", "JPY", Double.POSITIVE_INFINITY)

        val result = repository.getRate("USD", "JPY")

        assertNull(result)
    }

    @Test
    fun `getAllRatesForCurrency filters invalid rates`() = runTest {
        val rates = listOf(
            createEntity("EUR", "USD", 1.09),
            createEntity("EUR", "GBP", Double.NaN),
            createEntity("EUR", "JPY", Double.NEGATIVE_INFINITY)
        )
        coEvery { exchangeRateDao.getRatesForCurrencyOnce("EUR") } returns rates

        val result = repository.getAllRatesForCurrency("EUR")

        assertEquals(1, result.size)
        assertEquals("USD", result[0].targetCurrency)
    }

    @Test
    fun `fetchLatestRates stores rates correctly`() = runTest {
        val rateItems = listOf(
            ExchangeRateItem("2024-01-15", "EUR", "USD", 1.09),
            ExchangeRateItem("2024-01-15", "EUR", "GBP", 0.8562),
            ExchangeRateItem("2024-01-15", "EUR", "EUR", 1.0)
        )
        coEvery { frankfurterApi.getRates("EUR", null) } returns Response.success(rateItems)

        val result = repository.fetchLatestRates("EUR", emptyList())

        assertTrue(result.isSuccess)
        coVerify { exchangeRateDao.replaceAll(match { rates ->
            rates.any { it.baseCurrency == "EUR" && it.targetCurrency == "USD" && it.rate == 1.09 } &&
            rates.any { it.baseCurrency == "EUR" && it.targetCurrency == "GBP" && it.rate == 0.8562 } &&
            rates.count { it.targetCurrency == "EUR" } == 1 &&
            rates.any { it.baseCurrency == "EUR" && it.targetCurrency == "EUR" && it.rate == 1.0 }
        }) }
    }

    @Test
    fun `fetchLatestRates with one malformed rate does not replace cache`() = runTest {
        coEvery { responseValidator.validateLatest(any(), any(), any()) } returns
            Result.failure(SyncErrorException(SyncError.InvalidResponse))

        val result = repository.fetchLatestRates("EUR", emptyList())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { exchangeRateDao.replaceAll(any()) }
    }

    @Test
    fun `fetchLatestRates with partial coverage does not replace cache`() = runTest {
        val rateItems = listOf(
            ExchangeRateItem("2024-01-15", "EUR", "USD", 1.09),
            ExchangeRateItem("2024-01-15", "EUR", "GBP", 0.8562)
        )
        coEvery { frankfurterApi.getRates("EUR", null) } returns Response.success(rateItems)
        coEvery { responseValidator.validateLatest(any(), any(), any()) } returns
            Result.failure(SyncErrorException(SyncError.IncompleteResponse))

        val result = repository.fetchLatestRates("EUR", emptyList())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { exchangeRateDao.replaceAll(any()) }
    }

    @Test
    fun `fetchLatestRates rejects null body`() = runTest {
        coEvery { frankfurterApi.getRates("EUR", null) } returns
            Response.success<List<ExchangeRateItem>>(null)

        val result = repository.fetchLatestRates("EUR", emptyList())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { exchangeRateDao.replaceAll(any()) }
    }

    @Test
    fun `fetchLatestRates with quotes filter only fetches specified currencies`() = runTest {
        val rateItems = listOf(
            ExchangeRateItem("2024-01-15", "EUR", "USD", 1.09),
            ExchangeRateItem("2024-01-15", "EUR", "GBP", 0.8562)
        )
        coEvery { frankfurterApi.getRates("EUR", "USD,GBP") } returns Response.success(rateItems)

        val result = repository.fetchLatestRates("EUR", listOf("USD", "GBP"))

        assertTrue(result.isSuccess)
        coVerify { frankfurterApi.getRates("EUR", "USD,GBP") }
    }

    @Test
    fun `fetchLatestRates handles API failure`() = runTest {
        coEvery { frankfurterApi.getRates("EUR", null) } returns Response.error(500, mockk(relaxed = true))

        val result = repository.fetchLatestRates("EUR", emptyList())

        assertTrue(result.isFailure)
    }

    @Test
    fun `getRatesForCurrency returns flow of rates`() = runTest {
        val rates = listOf(
            createEntity("EUR", "USD", 1.09),
            createEntity("EUR", "GBP", 0.86)
        )
        coEvery { exchangeRateDao.getRatesForCurrency("EUR") } returns flowOf(rates)

        val result = repository.getRatesForCurrency("EUR")

        assertNotNull(result)
    }

    @Test
    fun `fetchAndStoreHistoricalRates delegates to the incremental syncer`() = runTest {
        val result = repository.fetchAndStoreHistoricalRates()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { historicalRateSyncer.syncRecentHistory(any()) }
    }

    @Test
    fun `fetchAndStoreHistoricalRates propagates syncer failure`() = runTest {
        coEvery { historicalRateSyncer.syncRecentHistory(any()) } returns
            Result.failure(SyncErrorException(SyncError.Network))

        val result = repository.fetchAndStoreHistoricalRates()

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchAndStoreHistoricalRates does not hit the API directly`() = runTest {
        repository.fetchAndStoreHistoricalRates()

        coVerify(exactly = 0) { frankfurterApi.getHistoricalRates(any(), any(), any(), any()) }
    }
}
