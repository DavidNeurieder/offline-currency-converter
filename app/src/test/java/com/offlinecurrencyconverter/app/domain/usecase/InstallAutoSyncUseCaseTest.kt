package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
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

class InstallAutoSyncUseCaseTest {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var syncExchangeRatesUseCase: SyncExchangeRatesUseCase
    private lateinit var syncHistoricalRatesUseCase: SyncHistoricalRatesUseCase
    private lateinit var installAutoSyncUseCase: InstallAutoSyncUseCase
    private var onSyncAttemptCount = 0

    private val currentVersion = 6

    @Before
    fun setup() {
        preferencesManager = mockk()
        syncExchangeRatesUseCase = mockk()
        syncHistoricalRatesUseCase = mockk()
        onSyncAttemptCount = 0
        every { preferencesManager.lastInstalledVersion } returns flowOf(0)
        every { preferencesManager.syncInterval } returns flowOf(24L)
        coEvery { preferencesManager.saveLastInstalledVersion(any()) } returns Unit
        installAutoSyncUseCase = InstallAutoSyncUseCase(
            preferencesManager,
            syncExchangeRatesUseCase,
            syncHistoricalRatesUseCase
        )
    }

    private suspend fun run() {
        installAutoSyncUseCase(currentVersion) { onSyncAttemptCount++ }
    }

    @Test
    fun `first install syncs latest and historical rates`() = runTest {
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.success(Unit)
        coEvery { syncHistoricalRatesUseCase() } returns Result.success(Unit)

        run()

        coVerify(exactly = 1) { syncExchangeRatesUseCase.forceSync() }
        coVerify(exactly = 1) { syncHistoricalRatesUseCase() }
        assertEquals(1, onSyncAttemptCount)
        coVerify(exactly = 1) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `first install with failed latest sync skips historical sync`() = runTest {
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.failure(Exception("Network error"))
        coEvery { syncHistoricalRatesUseCase() } returns Result.success(Unit)

        run()

        coVerify(exactly = 1) { syncExchangeRatesUseCase.forceSync() }
        coVerify(exactly = 0) { syncHistoricalRatesUseCase() }
        assertEquals(1, onSyncAttemptCount)
        coVerify(exactly = 1) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `first install with sync disabled saves version without syncing`() = runTest {
        every { preferencesManager.syncInterval } returns flowOf(0L)
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.success(Unit)

        run()

        coVerify(exactly = 0) { syncExchangeRatesUseCase.forceSync() }
        coVerify(exactly = 0) { syncHistoricalRatesUseCase() }
        assertEquals(0, onSyncAttemptCount)
        coVerify(exactly = 1) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `update syncs latest and historical rates`() = runTest {
        every { preferencesManager.lastInstalledVersion } returns flowOf(currentVersion - 1)
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.success(Unit)
        coEvery { syncHistoricalRatesUseCase() } returns Result.success(Unit)

        run()

        coVerify(exactly = 1) { syncExchangeRatesUseCase.forceSync() }
        coVerify(exactly = 1) { syncHistoricalRatesUseCase() }
        assertEquals(1, onSyncAttemptCount)
        coVerify(exactly = 1) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `same version does not resync`() = runTest {
        every { preferencesManager.lastInstalledVersion } returns flowOf(currentVersion)
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.success(Unit)

        run()

        coVerify(exactly = 0) { syncExchangeRatesUseCase.forceSync() }
        coVerify(exactly = 0) { syncHistoricalRatesUseCase() }
        assertEquals(0, onSyncAttemptCount)
        coVerify(exactly = 0) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `downgrade does not resync`() = runTest {
        every { preferencesManager.lastInstalledVersion } returns flowOf(currentVersion + 1)
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.success(Unit)

        run()

        coVerify(exactly = 0) { syncExchangeRatesUseCase.forceSync() }
        assertEquals(0, onSyncAttemptCount)
        coVerify(exactly = 0) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }
}