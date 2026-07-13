package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncExchangeRatesUseCaseTest {

    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var syncExchangeRatesUseCase: SyncExchangeRatesUseCase

    private val syncIntervalMillis = 24 * 60 * 60 * 1000L

    @Before
    fun setup() {
        exchangeRateRepository = mockk()
        preferencesManager = mockk()
        every { preferencesManager.historicalRatesChart } returns flowOf(true)
        syncExchangeRatesUseCase = SyncExchangeRatesUseCase(exchangeRateRepository, preferencesManager)
    }

    @Test
    fun `invoke skips sync if within interval`() = runTest {
        val lastUpdateTime = System.currentTimeMillis() - (60 * 60 * 1000L)
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns lastUpdateTime

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isSuccess)
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
        coEvery { exchangeRateRepository.fetchAndStoreHistoricalRates() } returns Result.success(Unit)

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isSuccess)
        coVerify {
            exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
        }
        coVerify { exchangeRateRepository.fetchAndStoreHistoricalRates() }
    }

    @Test
    fun `invoke fetches historical rates after successful latest rates`() = runTest {
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns null
        coEvery { exchangeRateRepository.fetchLatestRates(any(), any()) } returns Result.success(Unit)
        coEvery { exchangeRateRepository.fetchAndStoreHistoricalRates() } returns Result.success(Unit)

        syncExchangeRatesUseCase(syncIntervalMillis)

        coVerify(ordering = Ordering.ORDERED) {
            exchangeRateRepository.fetchLatestRates(any(), any())
            exchangeRateRepository.fetchAndStoreHistoricalRates()
        }
    }

    @Test
    fun `invoke skips historical rates when latest rates fail`() = runTest {
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns null
        coEvery { exchangeRateRepository.fetchLatestRates(any(), any()) } returns Result.failure(Exception("Network error"))

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { exchangeRateRepository.fetchAndStoreHistoricalRates() }
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
        coEvery { exchangeRateRepository.fetchAndStoreHistoricalRates() } returns Result.success(Unit)

        val result = syncExchangeRatesUseCase.forceSync()

        assertTrue(result.isSuccess)
        coVerify {
            exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
        }
        coVerify { exchangeRateRepository.fetchAndStoreHistoricalRates() }
    }

    @Test
    fun `invoke skips historical rates when chart disabled`() = runTest {
        every { preferencesManager.historicalRatesChart } returns flowOf(false)
        syncExchangeRatesUseCase = SyncExchangeRatesUseCase(exchangeRateRepository, preferencesManager)

        coEvery { exchangeRateRepository.getLastUpdateTime() } returns null
        coEvery { exchangeRateRepository.fetchLatestRates(any(), any()) } returns Result.success(Unit)

        val result = syncExchangeRatesUseCase(syncIntervalMillis)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { exchangeRateRepository.fetchAndStoreHistoricalRates() }
    }

    @Test
    fun `forceSync skips historical rates when chart disabled`() = runTest {
        every { preferencesManager.historicalRatesChart } returns flowOf(false)
        syncExchangeRatesUseCase = SyncExchangeRatesUseCase(exchangeRateRepository, preferencesManager)

        coEvery {
            exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
        } returns Result.success(Unit)

        val result = syncExchangeRatesUseCase.forceSync()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { exchangeRateRepository.fetchAndStoreHistoricalRates() }
    }
}
