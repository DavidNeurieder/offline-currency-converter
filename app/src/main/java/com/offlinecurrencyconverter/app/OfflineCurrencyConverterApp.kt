package com.offlinecurrencyconverter.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.offlinecurrencyconverter.app.data.CurrencyInitializer
import com.offlinecurrencyconverter.app.domain.usecase.SyncExchangeRatesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class OfflineCurrencyConverterApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "OfflineCurrencyConverterApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var currencyInitializer: CurrencyInitializer

    @Inject
    lateinit var syncExchangeRatesUseCase: SyncExchangeRatesUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate started")
        
        initializeCurrencies()
        Log.d(TAG, "Application onCreate completed")
    }

    private fun initializeCurrencies() {
        applicationScope.launch {
            try {
                currencyInitializer.initializeIfNeeded()
                syncExchangeRatesUseCase.forceSync()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize currencies", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
