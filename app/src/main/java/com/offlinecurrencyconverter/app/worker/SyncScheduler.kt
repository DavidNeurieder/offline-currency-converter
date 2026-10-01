package com.offlinecurrencyconverter.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /**
     * One-shot bootstrap sync used on first install and after an app update.
     * Unlike an in-process coroutine this survives process death, and it is
     * retried with backoff until it succeeds.
     */
    fun enqueueInitialSync() {
        val constraints = networkConstraints()
        val workManager = WorkManager.getInstance(context)

        val latestRatesRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        val historicalRatesRequest = OneTimeWorkRequestBuilder<SyncHistoricalRatesWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            INITIAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            latestRatesRequest
        )
        workManager.enqueueUniqueWork(
            INITIAL_HISTORICAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            historicalRatesRequest
        )
    }

    fun schedulePeriodicSync(intervalHours: Long) {
        val constraints = networkConstraints()

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
        const val INITIAL_SYNC_WORK_NAME = "initial_exchange_rate_sync"
        const val INITIAL_HISTORICAL_SYNC_WORK_NAME = "initial_historical_rates_sync"
        private const val BACKOFF_DELAY_SECONDS = 30L
    }
}