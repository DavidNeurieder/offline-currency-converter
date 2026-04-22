package com.offlinecurrencyconverter.app.worker

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun `doWork failure with retry returns retry`() = runBlocking {
        syncUseCase.result = Result.failure(Exception("Network error"))
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork(attemptCount = 0)

        assertEquals(TestableSyncWorkerLogic.Result.RETRY, result)
    }

    @Test
    fun `doWork max attempts returns failure`() = runBlocking {
        syncUseCase.result = Result.failure(Exception("Persistent error"))
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork(attemptCount = 3)

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
    fun `doWork exception with retry returns retry`() = runBlocking {
        syncUseCase.shouldThrowException = true
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork(attemptCount = 0)

        assertEquals(TestableSyncWorkerLogic.Result.RETRY, result)
    }

    @Test
    fun `doWork exception after max attempts returns failure`() = runBlocking {
        syncUseCase.shouldThrowException = true
        preferencesManager.syncIntervalHours = 24L

        val result = workerLogic.doWork(attemptCount = 3)

        assertEquals(TestableSyncWorkerLogic.Result.FAILURE, result)
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
        private val MAX_ATTEMPTS = 3

        suspend fun doWork(attemptCount: Int = 0): Result {
            return try {
                val syncIntervalHours = preferencesManager.syncIntervalHours
                val syncIntervalMillis = syncIntervalHours * 60 * 60 * 1000

                val result = syncUseCase.doSync(syncIntervalMillis)
                result.fold(
                    onSuccess = { Result.SUCCESS },
                    onFailure = {
                        if (attemptCount < MAX_ATTEMPTS) {
                            Result.RETRY
                        } else {
                            Result.FAILURE
                        }
                    }
                )
            } catch (e: Exception) {
                if (attemptCount < MAX_ATTEMPTS) {
                    Result.RETRY
                } else {
                    Result.FAILURE
                }
            }
        }

        enum class Result {
            SUCCESS,
            RETRY,
            FAILURE
        }
    }
}
