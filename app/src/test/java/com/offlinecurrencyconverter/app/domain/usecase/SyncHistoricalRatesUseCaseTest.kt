package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
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

class SyncHistoricalRatesUseCaseTest {

    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var syncHistoricalRatesUseCase: SyncHistoricalRatesUseCase

    @Before
    fun setup() {
        exchangeRateRepository = mockk()
        preferencesManager = mockk()
        every { preferencesManager.historicalRatesChart } returns flowOf(true)
        syncHistoricalRatesUseCase = SyncHistoricalRatesUseCase(exchangeRateRepository, preferencesManager)
    }

    @Test
    fun `invoke fetches historical rates when chart enabled`() = runTest {
        coEvery { exchangeRateRepository.fetchAndStoreHistoricalRates() } returns Result.success(Unit)

        val result = syncHistoricalRatesUseCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { exchangeRateRepository.fetchAndStoreHistoricalRates() }
    }

    @Test
    fun `invoke returns success from repository`() = runTest {
        coEvery { exchangeRateRepository.fetchAndStoreHistoricalRates() } returns Result.success(Unit)

        val result = syncHistoricalRatesUseCase()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        coEvery { exchangeRateRepository.fetchAndStoreHistoricalRates() } returns Result.failure(Exception("Network error"))

        val result = syncHistoricalRatesUseCase()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke skips fetch when chart disabled`() = runTest {
        every { preferencesManager.historicalRatesChart } returns flowOf(false)
        syncHistoricalRatesUseCase = SyncHistoricalRatesUseCase(exchangeRateRepository, preferencesManager)

        val result = syncHistoricalRatesUseCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { exchangeRateRepository.fetchAndStoreHistoricalRates() }
    }
}