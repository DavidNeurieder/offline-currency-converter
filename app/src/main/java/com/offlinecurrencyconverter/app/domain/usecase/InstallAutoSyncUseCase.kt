package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.worker.SyncScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class InstallAutoSyncUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val syncScheduler: SyncScheduler
) {

    suspend operator fun invoke(currentVersionCode: Int) {
        val lastVersion = preferencesManager.lastInstalledVersion.first()
        val isFirstInstall = lastVersion == 0
        val isUpdate = !isFirstInstall && currentVersionCode > lastVersion

        if (!isFirstInstall && !isUpdate) return

        val syncIntervalHours = preferencesManager.syncInterval.first()
        if (syncIntervalHours > 0L) {
            // The sync itself is handed to WorkManager so it survives process
            // death and is retried with backoff instead of being attempted once.
            syncScheduler.enqueueInitialSync()
            syncScheduler.schedulePeriodicSync(syncIntervalHours)
        } else {
            syncScheduler.cancelSync()
        }
        preferencesManager.saveLastInstalledVersion(currentVersionCode)
    }
}
