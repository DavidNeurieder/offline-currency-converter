package com.offlinecurrencyconverter.app.ui.convert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinecurrencyconverter.app.data.CurrencyInitializer
import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import com.offlinecurrencyconverter.app.domain.repository.HistoricalRateRepository
import com.offlinecurrencyconverter.app.domain.repository.RecentConversionRepository
import com.offlinecurrencyconverter.app.domain.usecase.ConvertCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvertUiState(
    val amount: String = "",
    val sourceCurrency: Currency? = null,
    val targetCurrency: Currency? = null,
    val conversionResult: ConversionResult? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isCurrenciesLoading: Boolean = true,
    val currenciesError: String? = null,
    val isRefreshing: Boolean = false,
    val lastSyncTime: Long? = null,
    val recentCurrencies: List<Currency> = emptyList(),
    val historicalRates: List<HistoricalRateEntity> = emptyList(),
    val isHistoricalLoading: Boolean = false
)

@HiltViewModel
class ConvertViewModel @Inject constructor(
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val currencyRepository: CurrencyRepository,
    private val recentConversionRepository: RecentConversionRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val historicalRateRepository: HistoricalRateRepository,
    private val preferencesManager: PreferencesManager,
    private val currencyInitializer: CurrencyInitializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConvertUiState())
    val uiState: StateFlow<ConvertUiState> = _uiState.asStateFlow()

    val currencies: StateFlow<List<Currency>> = currencyRepository.getAllCurrencies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentConversions: StateFlow<List<ConversionResult>> = recentConversionRepository.getRecentConversions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recentCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val recentCurrencies: StateFlow<List<Currency>> = _recentCurrencies.asStateFlow()

    init {
        loadSavedCurrencies()
        loadLastSyncTime()
        loadRecentCurrencies()
        retryLoadingCurrencies()
    }

    private fun loadSavedCurrencies() {
        viewModelScope.launch {
            currencies.collect { list ->
                if (list.isNotEmpty()) {
                    val savedSource = preferencesManager.sourceCurrency.first()
                    val savedTarget = preferencesManager.targetCurrency.first()

                    val sourceCurrency = list.find { it.code == savedSource }
                        ?: list.find { it.code == "USD" }
                        ?: list.first()

                    val targetCurrency = list.find { it.code == savedTarget }
                        ?: list.find { it.code == "EUR" }
                        ?: list.getOrNull(1)
                        ?: list.first()

                    _uiState.value = _uiState.value.copy(
                        sourceCurrency = sourceCurrency,
                        targetCurrency = targetCurrency
                    )
                }
            }
        }
    }

    fun retryLoadingCurrencies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCurrenciesLoading = true,
                currenciesError = null
            )
            val result = currencyInitializer.initializeIfNeeded()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isCurrenciesLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isCurrenciesLoading = false,
                        currenciesError = "Internet connection is required on first launch to download currency data"
                    )
                }
            )
        }
    }

    private fun loadLastSyncTime() {
        viewModelScope.launch {
            val lastSync = exchangeRateRepository.getLastUpdateTime()
            _uiState.value = _uiState.value.copy(lastSyncTime = lastSync)
        }
    }

    private fun loadRecentCurrencies() {
        viewModelScope.launch {
            preferencesManager.recentCurrencies.collect { recentCodes ->
                val allCurrencies = currencies.value
                val recent = recentCodes.mapNotNull { code ->
                    allCurrencies.find { it.code == code }
                }
                _recentCurrencies.value = recent
                _uiState.value = _uiState.value.copy(recentCurrencies = recent)
            }
        }
    }

    fun refreshLastSyncTime() {
        loadLastSyncTime()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val result = exchangeRateRepository.fetchLatestRates(
                baseCurrency = "EUR",
                targetCurrencies = emptyList()
            )
            result.fold(
                onSuccess = {
                    val lastSync = exchangeRateRepository.getLastUpdateTime()
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        lastSyncTime = lastSync
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            )
        }
    }

    fun onAmountChange(newAmount: String) {
        val filtered = newAmount.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(amount = filtered, error = null)
        if (filtered.isNotEmpty() && filtered.toDoubleOrNull() != null) {
            performConversion()
        } else {
            _uiState.value = _uiState.value.copy(conversionResult = null)
        }
    }

    fun onSourceCurrencyChange(currency: Currency) {
        _uiState.value = _uiState.value.copy(sourceCurrency = currency, error = null)
        viewModelScope.launch {
            preferencesManager.saveSourceCurrency(currency.code)
            preferencesManager.addRecentCurrency(currency.code)
        }
        performConversion()
        loadHistoricalRates()
    }

    fun onTargetCurrencyChange(currency: Currency) {
        _uiState.value = _uiState.value.copy(targetCurrency = currency, error = null)
        viewModelScope.launch {
            preferencesManager.saveTargetCurrency(currency.code)
            preferencesManager.addRecentCurrency(currency.code)
        }
        performConversion()
        loadHistoricalRates()
    }

    fun onFavoriteToggle(currencyCode: String, isFavorite: Boolean) {
        viewModelScope.launch {
            currencyRepository.updateFavorite(currencyCode, isFavorite)
        }
    }

    fun swapCurrencies() {
        val current = _uiState.value
        val newSource = current.targetCurrency
        val newTarget = current.sourceCurrency

        _uiState.value = current.copy(
            sourceCurrency = newSource,
            targetCurrency = newTarget,
            error = null
        )

        viewModelScope.launch {
            newSource?.let { preferencesManager.saveSourceCurrency(it.code) }
            newTarget?.let { preferencesManager.saveTargetCurrency(it.code) }
        }

        performConversion()
        loadHistoricalRates()
    }

    private fun loadHistoricalRates() {
        val source = _uiState.value.sourceCurrency ?: return
        val target = _uiState.value.targetCurrency ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isHistoricalLoading = true)
            val result = historicalRateRepository.fetchAndStoreHistoricalRates(source.code, target.code, 30)
            result.fold(
                onSuccess = {
                    historicalRateRepository.getHistoricalRates(source.code, target.code).collect { rates ->
                        _uiState.value = _uiState.value.copy(
                            historicalRates = rates,
                            isHistoricalLoading = false
                        )
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isHistoricalLoading = false)
                }
            )
        }
    }

    private fun performConversion() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        val source = state.sourceCurrency
        val target = state.targetCurrency

        if (amount == null || source == null || target == null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = convertCurrencyUseCase(amount, source, target)
            result.fold(
                onSuccess = { conversion ->
                    _uiState.value = _uiState.value.copy(
                        conversionResult = conversion,
                        error = null,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        conversionResult = null,
                        error = error.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun saveConversion() {
        val result = _uiState.value.conversionResult ?: return
        viewModelScope.launch {
            recentConversionRepository.saveConversion(result)
        }
    }
}
