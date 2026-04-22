package com.offlinecurrencyconverter.app.ui.settings

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import com.offlinecurrencyconverter.app.domain.usecase.SyncExchangeRatesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var syncExchangeRatesUseCase: SyncExchangeRatesUseCase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        exchangeRateRepository = mockk(relaxed = true)
        syncExchangeRatesUseCase = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)

        coEvery { exchangeRateRepository.getLastUpdateTime() } returns null
        every { preferencesManager.syncInterval } returns flowOf(24L)
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.success(Unit)

        viewModel = SettingsViewModel(
            exchangeRateRepository,
            syncExchangeRatesUseCase,
            preferencesManager
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(SyncInterval.TWENTY_FOUR_HOURS, state.syncInterval)
        assertNull(state.lastSyncTime)
        assertFalse(state.isSyncing)
        assertNull(state.syncError)
        assertFalse(state.syncSuccess)
    }

    @Test
    fun `onSyncIntervalChange updates interval`() = runTest {
        viewModel.onSyncIntervalChange(SyncInterval.TWELVE_HOURS)

        assertEquals(SyncInterval.TWELVE_HOURS, viewModel.uiState.value.syncInterval)
    }

    @Test
    fun `syncNow triggers sync successfully`() = runTest {
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.success(Unit)

        viewModel.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.syncSuccess)
        assertNotNull(viewModel.uiState.value.lastSyncTime)
        assertFalse(viewModel.uiState.value.isSyncing)
    }

    @Test
    fun `syncNow handles failure`() = runTest {
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.failure(Exception("Network error"))

        viewModel.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.syncSuccess)
        assertNotNull(viewModel.uiState.value.syncError)
        assertFalse(viewModel.uiState.value.isSyncing)
    }

    @Test
    fun `clearSyncStatus clears error and success`() = runTest {
        coEvery { syncExchangeRatesUseCase.forceSync() } returns Result.failure(Exception("Error"))

        viewModel.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearSyncStatus()

        assertNull(viewModel.uiState.value.syncError)
        assertFalse(viewModel.uiState.value.syncSuccess)
    }

    @Test
    fun `all sync intervals are available`() = runTest {
        val intervals = SyncInterval.entries

        assertEquals(5, intervals.size)
        assertTrue(intervals.contains(SyncInterval.SIX_HOURS))
        assertTrue(intervals.contains(SyncInterval.TWELVE_HOURS))
        assertTrue(intervals.contains(SyncInterval.TWENTY_FOUR_HOURS))
        assertTrue(intervals.contains(SyncInterval.FORTY_EIGHT_HOURS))
        assertTrue(intervals.contains(SyncInterval.WEEKLY))
    }
}
