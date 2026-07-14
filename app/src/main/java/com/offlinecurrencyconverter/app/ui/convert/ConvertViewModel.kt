package com.offlinecurrencyconverter.app.ui.convert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinecurrencyconverter.app.data.CurrencyInitializer
import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.model.ExchangeRate
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

data class MultiCurrencyResult(
    val currency: Currency,
    val rate: Double,
    val convertedAmount: Double
)

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
    val filteredHistoricalRates: List<HistoricalRateEntity> = emptyList(),
    val selectedDateRange: Int = 30,
    val multiCurrencyConversions: List<MultiCurrencyResult> = emptyList(),
    val multiCurrencyView: Boolean = false,
    val historicalRatesChart: Boolean = false
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
        loadInitialHistoricalRates()
        loadMultiCurrencyViewPreference()
        loadHistoricalRatesChartPreference()
        loadSavedAmount()
        loadSavedChartDateRange()
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

                    performConversion()

                    if (_uiState.value.multiCurrencyView) {
                        recomputeMultiCurrency()
                    }
                }
            }
        }
    }

    private fun loadInitialHistoricalRates() {
        viewModelScope.launch {
            uiState.first { it.sourceCurrency != null && it.targetCurrency != null }
            loadHistoricalRates()
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

    private fun loadSavedAmount() {
        viewModelScope.launch {
            val saved = preferencesManager.amount.first()
            if (saved.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(amount = saved)
                if (saved.toDoubleOrNull() != null) {
                    performConversion()
                }
            }
        }
    }

    private fun loadSavedChartDateRange() {
        viewModelScope.launch {
            val saved = preferencesManager.chartDateRange.first()
            _uiState.value = _uiState.value.copy(selectedDateRange = saved)
        }
    }

    private fun loadMultiCurrencyViewPreference() {
        viewModelScope.launch {
            preferencesManager.multiCurrencyView.collect { enabled ->
                _uiState.value = _uiState.value.copy(multiCurrencyView = enabled)
                if (!enabled) {
                    _uiState.value = _uiState.value.copy(multiCurrencyConversions = emptyList())
                } else {
                    recomputeMultiCurrency()
                }
            }
        }
        viewModelScope.launch {
            currencies.collect {
                if (_uiState.value.multiCurrencyView) {
                    recomputeMultiCurrency()
                }
            }
        }
    }

    private fun recomputeMultiCurrency() {
        val amount = _uiState.value.amount.toDoubleOrNull()
        val source = _uiState.value.sourceCurrency
        if (amount != null && source != null) {
            performMultiCurrencyConversion(amount, source)
        }
    }

    private fun loadHistoricalRatesChartPreference() {
        viewModelScope.launch {
            preferencesManager.historicalRatesChart.collect { enabled ->
                _uiState.value = _uiState.value.copy(historicalRatesChart = enabled)
                if (enabled) {
                    loadHistoricalRates()
                } else {
                    _uiState.value = _uiState.value.copy(historicalRates = emptyList())
                }
            }
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
            if (result.isSuccess) {
                exchangeRateRepository.fetchAndStoreHistoricalRates()
            }
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
        viewModelScope.launch {
            preferencesManager.saveAmount(filtered)
        }
        if (filtered.isNotEmpty() && filtered.toDoubleOrNull() != null) {
            performConversion()
        } else {
            _uiState.value = _uiState.value.copy(
                conversionResult = null,
                multiCurrencyConversions = emptyList()
            )
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
        if (!_uiState.value.historicalRatesChart) return
        val source = _uiState.value.sourceCurrency ?: return
        val target = _uiState.value.targetCurrency ?: return

        viewModelScope.launch {
            val rates = historicalRateRepository.getHistoricalRates(source.code, target.code)
            _uiState.value = _uiState.value.copy(historicalRates = rates)
            applyDateRangeFilter()
        }
    }

    fun onDateRangeChange(days: Int) {
        _uiState.value = _uiState.value.copy(selectedDateRange = days)
        applyDateRangeFilter()
        viewModelScope.launch {
            preferencesManager.saveChartDateRange(days)
        }
    }

    private fun applyDateRangeFilter() {
        val allRates = _uiState.value.historicalRates
        val days = _uiState.value.selectedDateRange
        val filtered = if (allRates.size <= days) {
            allRates
        } else {
            allRates.takeLast(days)
        }
        _uiState.value = _uiState.value.copy(filteredHistoricalRates = filtered)
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
            if (_uiState.value.multiCurrencyView) {
                performMultiCurrencyConversion(amount, source)
            }
        }
    }

    private fun performMultiCurrencyConversion(amount: Double, source: Currency) {
        viewModelScope.launch {
            val allRates = exchangeRateRepository.getAllRatesForCurrency(source.code)
            if (allRates.isEmpty()) {
                _uiState.value = _uiState.value.copy(multiCurrencyConversions = emptyList())
                return@launch
            }

            val favorites = currencies.value.filter { it.isFavorite && it.code != source.code }

            if (favorites.isEmpty()) {
                _uiState.value = _uiState.value.copy(multiCurrencyConversions = emptyList())
                return@launch
            }

            val conversions = favorites.mapNotNull { currency ->
                val rate = allRates.find { it.targetCurrency == currency.code }?.rate
                    ?: return@mapNotNull null
                MultiCurrencyResult(
                    currency = currency,
                    rate = rate,
                    convertedAmount = amount * rate
                )
            }

            _uiState.value = _uiState.value.copy(multiCurrencyConversions = conversions)
        }
    }

    fun saveConversion() {
        val result = _uiState.value.conversionResult ?: return
        viewModelScope.launch {
            recentConversionRepository.saveConversion(result)
        }
    }
}
