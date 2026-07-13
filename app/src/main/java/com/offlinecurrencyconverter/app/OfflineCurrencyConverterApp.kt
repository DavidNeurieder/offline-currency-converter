package com.offlinecurrencyconverter.app

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.offlinecurrencyconverter.app.data.CurrencyInitializer
import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.usecase.SyncExchangeRatesUseCase
import com.offlinecurrencyconverter.app.widget.CurrencyWidgetProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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

    @Inject
    lateinit var preferencesManager: PreferencesManager

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
                val syncIntervalHours = preferencesManager.syncInterval.first()
                if (syncIntervalHours > 0L) {
                    syncExchangeRatesUseCase.forceSync()
                    updateWidget()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize currencies", e)
            }
        }
    }

    private fun updateWidget() {
        val intent = Intent(this, CurrencyWidgetProvider::class.java).apply {
            action = CurrencyWidgetProvider.ACTION_UPDATE_WIDGET
        }
        sendBroadcast(intent)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
