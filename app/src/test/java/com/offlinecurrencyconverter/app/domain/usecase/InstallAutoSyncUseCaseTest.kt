package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.worker.SyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class InstallAutoSyncUseCaseTest {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var installAutoSyncUseCase: InstallAutoSyncUseCase

    private val currentVersion = 8

    @Before
    fun setup() {
        preferencesManager = mockk()
        syncScheduler = mockk(relaxed = true)
        every { preferencesManager.lastInstalledVersion } returns flowOf(0)
        every { preferencesManager.syncInterval } returns flowOf(24L)
        coEvery { preferencesManager.saveLastInstalledVersion(any()) } returns Unit
        installAutoSyncUseCase = InstallAutoSyncUseCase(preferencesManager, syncScheduler)
    }

    private suspend fun run() = installAutoSyncUseCase(currentVersion)

    @Test
    fun `first install enqueues bootstrap sync and schedules periodic sync`() = runTest {
        run()

        verify(exactly = 1) { syncScheduler.enqueueInitialSync() }
        verify(exactly = 1) { syncScheduler.schedulePeriodicSync(24L) }
        verify(exactly = 0) { syncScheduler.cancelSync() }
        coVerify(exactly = 1) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `first install with sync disabled cancels work and does not bootstrap`() = runTest {
        every { preferencesManager.syncInterval } returns flowOf(0L)

        run()

        verify(exactly = 0) { syncScheduler.enqueueInitialSync() }
        verify(exactly = 0) { syncScheduler.schedulePeriodicSync(any()) }
        verify(exactly = 1) { syncScheduler.cancelSync() }
        coVerify(exactly = 1) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `update enqueues bootstrap sync and schedules periodic sync`() = runTest {
        every { preferencesManager.lastInstalledVersion } returns flowOf(currentVersion - 1)

        run()

        verify(exactly = 1) { syncScheduler.enqueueInitialSync() }
        verify(exactly = 1) { syncScheduler.schedulePeriodicSync(24L) }
        coVerify(exactly = 1) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `same version does not sync`() = runTest {
        every { preferencesManager.lastInstalledVersion } returns flowOf(currentVersion)

        run()

        verify(exactly = 0) { syncScheduler.enqueueInitialSync() }
        verify(exactly = 0) { syncScheduler.schedulePeriodicSync(any()) }
        coVerify(exactly = 0) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }

    @Test
    fun `downgrade does not sync`() = runTest {
        every { preferencesManager.lastInstalledVersion } returns flowOf(currentVersion + 1)

        run()

        verify(exactly = 0) { syncScheduler.enqueueInitialSync() }
        coVerify(exactly = 0) { preferencesManager.saveLastInstalledVersion(currentVersion) }
    }
}
