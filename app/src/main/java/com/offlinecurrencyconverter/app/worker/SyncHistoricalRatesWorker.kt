package com.offlinecurrencyconverter.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.offlinecurrencyconverter.app.domain.model.isRetryable
import com.offlinecurrencyconverter.app.domain.usecase.SyncHistoricalRatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncHistoricalRatesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncHistoricalRatesUseCase: SyncHistoricalRatesUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = syncHistoricalRatesUseCase()
        return result.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                // Retry retryable errors indefinitely, but give other errors a
                // few extra attempts: a single rejected response (e.g. the
                // upstream feed briefly listing a currency we do not know yet)
                // would otherwise leave the app without history for good.
                if (error.isRetryable() || runAttemptCount < MAX_ATTEMPTS - 1) {
                    Result.retry()
                } else {
                    Log.w(TAG, "Historical sync failed permanently: ${error.message}")
                    Result.failure()
                }
            }
        )
    }

    companion object {
        const val WORK_NAME = "historical_rates_sync"
        const val MAX_ATTEMPTS = 3
        private const val TAG = "SyncHistoricalRates"
    }
}