package com.offlinecurrencyconverter.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Intent
import com.offlinecurrencyconverter.app.widget.CurrencyWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "currency_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val SOURCE_CURRENCY = stringPreferencesKey("source_currency")
        val TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val SYNC_INTERVAL = longPreferencesKey("sync_interval")
        val CURRENCIES_INITIALIZED = booleanPreferencesKey("currencies_initialized")
        val RECENT_CURRENCIES = stringPreferencesKey("recent_currencies")
        val MULTI_CURRENCY_VIEW = booleanPreferencesKey("multi_currency_view")
        val HISTORICAL_RATES_CHART = booleanPreferencesKey("historical_rates_chart")
        val FAVORITES_INITIALIZED = booleanPreferencesKey("favorites_initialized")
        val AMOUNT = stringPreferencesKey("amount")
        val CHART_DATE_RANGE = intPreferencesKey("chart_date_range")
    }

    companion object {
        const val MAX_RECENT_CURRENCIES = 5
    }

    val sourceCurrency: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.SOURCE_CURRENCY]
    }

    val targetCurrency: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.TARGET_CURRENCY]
    }

    val syncInterval: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.SYNC_INTERVAL] ?: 24L
    }

    val currenciesInitialized: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENCIES_INITIALIZED] ?: false
    }

    val recentCurrencies: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[Keys.RECENT_CURRENCIES]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val multiCurrencyView: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.MULTI_CURRENCY_VIEW] ?: true
    }

    val historicalRatesChart: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.HISTORICAL_RATES_CHART] ?: true
    }

    val favoritesInitialized: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.FAVORITES_INITIALIZED] ?: false
    }

    val amount: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.AMOUNT] ?: "1"
    }

    val chartDateRange: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.CHART_DATE_RANGE] ?: 30
    }

    suspend fun saveSourceCurrency(code: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SOURCE_CURRENCY] = code
        }
        updateWidget()
    }

    suspend fun saveTargetCurrency(code: String) {
        dataStore.edit { prefs ->
            prefs[Keys.TARGET_CURRENCY] = code
        }
        updateWidget()
    }

    suspend fun saveSyncInterval(hours: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.SYNC_INTERVAL] = hours
        }
    }

    suspend fun setCurrenciesInitialized(initialized: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCIES_INITIALIZED] = initialized
        }
    }

    suspend fun addRecentCurrency(code: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_CURRENCIES]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            current.remove(code)
            current.add(0, code)
            val trimmed = current.take(MAX_RECENT_CURRENCIES)
            prefs[Keys.RECENT_CURRENCIES] = trimmed.joinToString(",")
        }
    }

    suspend fun saveMultiCurrencyView(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.MULTI_CURRENCY_VIEW] = enabled
        }
    }

    suspend fun saveHistoricalRatesChart(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HISTORICAL_RATES_CHART] = enabled
        }
    }

    suspend fun saveFavoritesInitialized(initialized: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.FAVORITES_INITIALIZED] = initialized
        }
    }

    suspend fun saveAmount(amount: String) {
        dataStore.edit { prefs ->
            prefs[Keys.AMOUNT] = amount
        }
    }

    suspend fun saveChartDateRange(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.CHART_DATE_RANGE] = days
        }
    }

    private fun updateWidget() {
        val intent = Intent(context, CurrencyWidgetProvider::class.java).apply {
            action = CurrencyWidgetProvider.ACTION_UPDATE_WIDGET
        }
        context.sendBroadcast(intent)
    }
}
