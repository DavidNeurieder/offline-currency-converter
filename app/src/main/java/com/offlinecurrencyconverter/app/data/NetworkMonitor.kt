package com.offlinecurrencyconverter.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports whether the device currently has a usable internet connection.
 *
 * This is a *user experience* signal only, never a gate for correctness: cached
 * history keeps working offline, and the downloader still decides for itself
 * what to fetch. It exists so a chart range that has no cached history yet can
 * be disabled and explained instead of silently drawing a shorter series.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(ConnectivityManager::class.java)

    /** Emits the current state, then every change. */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                // Another network may still be up (e.g. wifi -> mobile handover).
                trySend(currentlyOnline())
            }
        }

        trySend(currentlyOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun currentlyOnline(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(active)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
