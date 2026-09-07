package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class InstallAutoSyncUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val syncHistoricalRatesUseCase: SyncHistoricalRatesUseCase
) {

    suspend operator fun invoke(
        currentVersionCode: Int,
        onSyncAttempt: () -> Unit
    ) {
        val lastVersion = preferencesManager.lastInstalledVersion.first()
        val isFirstInstall = lastVersion == 0
        val isUpdate = !isFirstInstall && currentVersionCode > lastVersion

        if (!isFirstInstall && !isUpdate) return

        val syncIntervalHours = preferencesManager.syncInterval.first()
        if (syncIntervalHours > 0L) {
            val latestSync = syncExchangeRatesUseCase.forceSync()
            if (latestSync.isSuccess) {
                syncHistoricalRatesUseCase()
            }
            onSyncAttempt()
        }
        preferencesManager.saveLastInstalledVersion(currentVersionCode)
    }
}