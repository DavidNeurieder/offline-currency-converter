package com.offlinecurrencyconverter.app.worker

import com.offlinecurrencyconverter.app.domain.model.SyncError
import com.offlinecurrencyconverter.app.domain.model.SyncErrorException
import com.offlinecurrencyconverter.app.domain.model.isRetryable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncWorkerTest {

    private lateinit var syncUseCase: MockSyncUseCase
    private lateinit var preferencesManager: MockSyncPreferencesManager
    private lateinit var workerLogic: TestableSyncWorkerLogic

    @Before
    fun setup() {
        syncUseCase = MockSyncUseCase()
        preferencesManager = MockSyncPreferencesManager()
        workerLogic = TestableSyncWorkerLogic(syncUseCase, preferencesManager)
    }

    @Test
    fun `doWork success returns success`() = runBlocking {
        syncUseCase.result = Result.success(Unit)
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork()

        assertEquals(TestableSyncWorkerLogic.Result.SUCCESS, result)
    }

    @Test
    fun `doWork retryable failure returns retry`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.Network))
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork()

        assertEquals(TestableSyncWorkerLogic.Result.RETRY, result)
    }

    @Test
    fun `doWork server failure returns retry`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.Server))
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork()

        assertEquals(TestableSyncWorkerLogic.Result.RETRY, result)
    }

    @Test
    fun `doWork non-retryable failure returns failure`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.InvalidResponse))
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork()

        assertEquals(TestableSyncWorkerLogic.Result.FAILURE, result)
    }

    @Test
    fun `doWork empty response failure returns failure`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.EmptyResponse))
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork()

        assertEquals(TestableSyncWorkerLogic.Result.FAILURE, result)
    }

    @Test
    fun `doWork http 400 failure returns failure`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.Http(400)))
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork()

        assertEquals(TestableSyncWorkerLogic.Result.FAILURE, result)
    }

    @Test
    fun `doWork respects sync interval`() = runBlocking {
        syncUseCase.result = Result.success(Unit)
        preferencesManager.syncIntervalHours = 48L

        workerLogic.doWork()

        val expectedMillis = 48L * 60 * 60 * 1000
        assertEquals(expectedMillis, syncUseCase.lastSyncIntervalMillis)
    }

    @Test
    fun `doWork exception returns retry`() = runBlocking {
        syncUseCase.shouldThrowException = true
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork()

        assertEquals(TestableSyncWorkerLogic.Result.RETRY, result)
    }

    @Test
    fun `isRetryable classifies network error as retryable`() {
        assertTrue(SyncErrorException(SyncError.Network).isRetryable())
        assertTrue(SyncErrorException(SyncError.Server).isRetryable())
        assertTrue(SyncErrorException(SyncError.Http(503)).isRetryable())
        assertTrue(java.io.IOException("timeout").isRetryable())
    }

    @Test
    fun `isRetryable classifies permanent errors as non-retryable`() {
        assertFalse(SyncErrorException(SyncError.InvalidResponse).isRetryable())
        assertFalse(SyncErrorException(SyncError.EmptyResponse).isRetryable())
        assertFalse(SyncErrorException(SyncError.Http(400)).isRetryable())
        assertFalse(SyncErrorException(SyncError.Http(404)).isRetryable())
    }

    private class MockSyncPreferencesManager {
        var syncIntervalHours: Long = 24L
    }

    private class MockSyncUseCase {
        var result: Result<Unit> = Result.success(Unit)
        var shouldThrowException = false
        var lastSyncIntervalMillis: Long = 0L

        suspend fun doSync(syncIntervalMillis: Long): Result<Unit> {
            lastSyncIntervalMillis = syncIntervalMillis
            if (shouldThrowException) {
                throw Exception("Unexpected error")
            }
            return result
        }
    }

    private class TestableSyncWorkerLogic(
        private val syncUseCase: MockSyncUseCase,
        private val preferencesManager: MockSyncPreferencesManager
    ) {
        suspend fun doWork(): Result {
            return try {
                val syncIntervalHours = preferencesManager.syncIntervalHours
                val syncIntervalMillis = syncIntervalHours * 60 * 60 * 1000

                val result = syncUseCase.doSync(syncIntervalMillis)
                result.fold(
                    onSuccess = { Result.SUCCESS },
                    onFailure = { error ->
                        if (error.isRetryable()) {
                            Result.RETRY
                        } else {
                            Result.FAILURE
                        }
                    }
                )
            } catch (e: Exception) {
                if (e.isRetryable()) Result.RETRY else Result.FAILURE
            }
        }

        enum class Result {
            SUCCESS,
            RETRY,
            FAILURE
        }
    }
}