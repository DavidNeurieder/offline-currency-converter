package com.offlinecurrencyconverter.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import com.offlinecurrencyconverter.app.domain.usecase.SyncExchangeRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SyncInterval(val hours: Long) {
    SIX_HOURS(6),
    TWELVE_HOURS(12),
    TWENTY_FOUR_HOURS(24),
    FORTY_EIGHT_HOURS(48),
    WEEKLY(168);

    val millis: Long get() = hours * 60 * 60 * 1000
}

data class SettingsUiState(
    val syncInterval: SyncInterval = SyncInterval.TWENTY_FOUR_HOURS,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val syncSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadLastSyncTime()
        loadSyncInterval()
    }

    private fun loadLastSyncTime() {
        viewModelScope.launch {
            val lastUpdate = exchangeRateRepository.getLastUpdateTime()
            _uiState.value = _uiState.value.copy(lastSyncTime = lastUpdate)
        }
    }

    private fun loadSyncInterval() {
        viewModelScope.launch {
            preferencesManager.syncInterval.collect { hours ->
                val interval = SyncInterval.entries.find { it.hours == hours } ?: SyncInterval.TWENTY_FOUR_HOURS
                _uiState.value = _uiState.value.copy(syncInterval = interval)
            }
        }
    }

    fun onSyncIntervalChange(interval: SyncInterval) {
        _uiState.value = _uiState.value.copy(syncInterval = interval)
        viewModelScope.launch {
            preferencesManager.saveSyncInterval(interval.hours)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null, syncSuccess = false)
            val result = syncExchangeRatesUseCase.forceSync()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncSuccess = true,
                        lastSyncTime = System.currentTimeMillis()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = error.message ?: "Sync failed"
                    )
                }
            )
        }
    }

    fun clearSyncStatus() {
        _uiState.value = _uiState.value.copy(syncError = null, syncSuccess = false)
    }
}
