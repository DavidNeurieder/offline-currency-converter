package com.offlinecurrencyconverter.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.testDataStore: DataStore<Preferences> by preferencesDataStore(name = "test_currency_prefs")

class PreferencesManagerTestHelper(context: Context) {

    private val dataStore: DataStore<Preferences> = context.testDataStore

    private object Keys {
        val SOURCE_CURRENCY = stringPreferencesKey("source_currency")
        val TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val SYNC_INTERVAL = longPreferencesKey("sync_interval")
        val CURRENCIES_INITIALIZED = booleanPreferencesKey("currencies_initialized")
        val RECENT_CURRENCIES = stringPreferencesKey("recent_currencies")
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

    suspend fun saveSourceCurrency(code: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SOURCE_CURRENCY] = code
        }
    }

    suspend fun saveTargetCurrency(code: String) {
        dataStore.edit { prefs ->
            prefs[Keys.TARGET_CURRENCY] = code
        }
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
}
