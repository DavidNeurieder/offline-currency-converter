package com.offlinecurrencyconverter.app.worker

import android.content.Context
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
                if (error.isRetryable()) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        )
    }

    companion object {
        const val WORK_NAME = "historical_rates_sync"
    }
}