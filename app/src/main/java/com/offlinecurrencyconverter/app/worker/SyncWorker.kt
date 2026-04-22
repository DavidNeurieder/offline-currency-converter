package com.offlinecurrencyconverter.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.usecase.SyncExchangeRatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val syncIntervalHours = preferencesManager.syncInterval.first()
            val syncIntervalMillis = syncIntervalHours * 60 * 60 * 1000
            
            val result = syncExchangeRatesUseCase(syncIntervalMillis)
            result.fold(
                onSuccess = { Result.success() },
                onFailure = {
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            )
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "exchange_rate_sync"
    }
}
