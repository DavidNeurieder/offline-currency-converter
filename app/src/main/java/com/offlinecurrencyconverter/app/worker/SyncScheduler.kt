package com.offlinecurrencyconverter.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedulePeriodicSync(intervalHours: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val latestRatesRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalHours, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        val historicalRatesRequest = PeriodicWorkRequestBuilder<SyncHistoricalRatesWorker>(
            HISTORICAL_SYNC_INTERVAL_HOURS, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            latestRatesRequest
        )
        workManager.enqueueUniquePeriodicWork(
            SyncHistoricalRatesWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            historicalRatesRequest
        )
    }

    fun cancelSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
        workManager.cancelUniqueWork(SyncHistoricalRatesWorker.WORK_NAME)
    }

    companion object {
        const val HISTORICAL_SYNC_INTERVAL_HOURS = 24L
        private const val BACKOFF_DELAY_SECONDS = 30L
    }
}