package com.offlinecurrencyconverter.app.worker

import com.offlinecurrencyconverter.app.domain.model.SyncError
import com.offlinecurrencyconverter.app.domain.model.SyncErrorException
import com.offlinecurrencyconverter.app.domain.model.isRetryable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SyncHistoricalRatesWorkerTest {

    private lateinit var syncUseCase: MockHistoricalSyncUseCase
    private lateinit var workerLogic: TestableHistoricalSyncWorkerLogic

    @Before
    fun setup() {
        syncUseCase = MockHistoricalSyncUseCase()
        workerLogic = TestableHistoricalSyncWorkerLogic(syncUseCase)
    }

    @Test
    fun `doWork success returns success`() = runBlocking {
        syncUseCase.result = Result.success(Unit)

        assertEquals(TestableHistoricalSyncWorkerLogic.Result.SUCCESS, workerLogic.doWork())
    }

    @Test
    fun `doWork retryable failure returns retry`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.Network))

        assertEquals(TestableHistoricalSyncWorkerLogic.Result.RETRY, workerLogic.doWork())
    }

    @Test
    fun `doWork non-retryable failure retries before attempt budget`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.InvalidResponse))

        assertEquals(TestableHistoricalSyncWorkerLogic.Result.RETRY, workerLogic.doWork(runAttemptCount = 0))
        assertEquals(TestableHistoricalSyncWorkerLogic.Result.RETRY, workerLogic.doWork(runAttemptCount = 1))
    }

    @Test
    fun `doWork non-retryable failure fails after attempt budget`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.InvalidResponse))

        assertEquals(
            TestableHistoricalSyncWorkerLogic.Result.FAILURE,
            workerLogic.doWork(runAttemptCount = TestableHistoricalSyncWorkerLogic.MAX_ATTEMPTS - 1)
        )
    }

    @Test
    fun `doWork retryable failure retries beyond attempt budget`() = runBlocking {
        syncUseCase.result = Result.failure(SyncErrorException(SyncError.Server))

        assertEquals(
            TestableHistoricalSyncWorkerLogic.Result.RETRY,
            workerLogic.doWork(runAttemptCount = 10)
        )
    }

    private class MockHistoricalSyncUseCase {
        var result: Result<Unit> = Result.success(Unit)

        suspend fun invoke(): Result<Unit> = result
    }

    private class TestableHistoricalSyncWorkerLogic(
        private val syncUseCase: MockHistoricalSyncUseCase
    ) {
        suspend fun doWork(runAttemptCount: Int = 0): Result {
            val result = syncUseCase.invoke()
            return result.fold(
                onSuccess = { Result.SUCCESS },
                onFailure = { error ->
                    if (error.isRetryable() || runAttemptCount < MAX_ATTEMPTS - 1) {
                        Result.RETRY
                    } else {
                        Result.FAILURE
                    }
                }
            )
        }

        enum class Result {
            SUCCESS,
            RETRY,
            FAILURE
        }

        companion object {
            const val MAX_ATTEMPTS = SyncHistoricalRatesWorker.MAX_ATTEMPTS
        }
    }
}
