package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncExchangeRatesUseCaseTest {

    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var syncExchangeRatesUseCase: SyncExchangeRatesUseCase

    private val syncIntervalMillis = 24 * 60 * 60 * 1000L

    @Before
    fun setup() {
        exchangeRateRepository = mockk()
        syncExchangeRatesUseCase = SyncExchangeRatesUseCase(exchangeRateRepository)
    }

    @Test
    fun `invoke skips sync if within interval`() = runTest {
        val lastUpdateTime = System.currentTimeMillis() - (60 * 60 * 1000L)
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns lastUpdateTime

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { exchangeRateRepository.fetchLatestRates(any(), any()) }
    }

    @Test
    fun `invoke syncs rates when interval elapsed`() = runTest {
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns null
        coEvery {
            exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
        } returns Result.success(Unit)

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isSuccess)
        coVerify {
            exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
        }
    }

    @Test
    fun `invoke syncs when cache exactly at interval`() = runTest {
        val lastUpdateTime = System.currentTimeMillis() - syncIntervalMillis
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns lastUpdateTime
        coEvery { exchangeRateRepository.fetchLatestRates(any(), any()) } returns Result.success(Unit)

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { exchangeRateRepository.fetchLatestRates(any(), any()) }
    }

    @Test
    fun `invoke uses cache when last sync in the future`() = runTest {
        val lastUpdateTime = System.currentTimeMillis() + (60 * 60 * 1000L)
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns lastUpdateTime

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { exchangeRateRepository.fetchLatestRates(any(), any()) }
    }

    @Test
    fun `invoke returns failure when API fails`() = runTest {
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns null
        coEvery {
            exchangeRateRepository.fetchLatestRates(any(), any())
        } returns Result.failure(Exception("Network error"))

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `forceSync always fetches rates`() = runTest {
        coEvery {
            exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
        } returns Result.success(Unit)

        val result = syncExchangeRatesUseCase.forceSync()

        assertTrue(result.isSuccess)
        coVerify {
            exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
        }
    }

    @Test
    fun `isCacheStale returns true for null last update`() {
        assertTrue(SyncExchangeRatesUseCase.isCacheStale(null, syncIntervalMillis))
    }

    @Test
    fun `isCacheStale returns false for fresh cache`() {
        val lastUpdated = System.currentTimeMillis() - (60 * 60 * 1000L)
        assertFalse(SyncExchangeRatesUseCase.isCacheStale(lastUpdated, syncIntervalMillis))
    }

    @Test
    fun `isCacheStale returns true for expired cache`() {
        val lastUpdated = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        assertTrue(SyncExchangeRatesUseCase.isCacheStale(lastUpdated, syncIntervalMillis))
    }

    @Test
    fun `isCacheStale handles last sync after current time`() {
        val lastUpdated = System.currentTimeMillis() + (60 * 60 * 1000L)
        assertFalse(SyncExchangeRatesUseCase.isCacheStale(lastUpdated, syncIntervalMillis))
    }
}