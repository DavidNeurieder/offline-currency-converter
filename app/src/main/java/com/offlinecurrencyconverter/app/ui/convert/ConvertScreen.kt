package com.offlinecurrencyconverter.app.ui.convert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlinecurrencyconverter.app.R
import com.offlinecurrencyconverter.app.ui.components.ConversionCard
import com.offlinecurrencyconverter.app.ui.components.CurrencyPickerBottomSheet
import com.offlinecurrencyconverter.app.ui.components.RateChart
import com.offlinecurrencyconverter.app.ui.components.RateChartSummary
import com.offlinecurrencyconverter.app.ui.components.RecentConversionItem

@OptIn(ExperimentalMaterialApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: ConvertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencies by viewModel.currencies.collectAsState()
    val recentConversions by viewModel.recentConversions.collectAsState()

    var showSourceCurrencyPicker by remember { mutableStateOf(false) }
    var showTargetCurrencyPicker by remember { mutableStateOf(false) }
    var showFavoritesPicker by remember { mutableStateOf(false) }

    val hasFavorites = remember(currencies) { currencies.any { it.isFavorite } }

    LaunchedEffect(Unit) {
        viewModel.refreshLastSyncTime()
    }

    if (uiState.currenciesError != null && currencies.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.currenciesError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("currencies_error")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = viewModel::retryLoadingCurrencies,
                    modifier = Modifier.testTag("retry_button")
                ) {
                    Text("Retry")
                }
            }
        }
    } else if (currencies.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
        }
    } else {
        val pullRefreshState = rememberPullRefreshState(
            refreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.currency_converter),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("currency_converter_header")
                        )
                        IconButton(
                            onClick = { showFavoritesPicker = true },
                            modifier = Modifier.testTag("open_favorites_picker")
                        ) {
                            Icon(
                                imageVector = if (hasFavorites) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = stringResource(R.string.favorite_currencies),
                                tint = if (hasFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("open_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings)
                            )
                        }
                    }
                }

                item {
                    ConversionCard(
                        amount = uiState.amount,
                        onAmountChange = viewModel::onAmountChange,
                        sourceCurrency = uiState.sourceCurrency,
                        targetCurrency = uiState.targetCurrency,
                        currencies = currencies,
                        recentCurrencies = uiState.recentCurrencies,
                        onSourceCurrencyClick = { showSourceCurrencyPicker = true },
                        onTargetCurrencyClick = { showTargetCurrencyPicker = true },
                        onSwap = viewModel::swapCurrencies,
                        conversionResult = uiState.conversionResult,
                        error = uiState.error,
                        isLoading = uiState.isLoading,
                        lastSyncTime = uiState.lastSyncTime,
                        multiCurrencyConversions = uiState.multiCurrencyConversions,
                        onMultiCurrencyTargetClick = { currency ->
                            viewModel.onTargetCurrencyChange(currency)
                        }
                    )
                }

                if (uiState.historicalRatesChart) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rate_chart"),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.rate_trend,
                                        uiState.sourceCurrency?.code ?: "",
                                        uiState.targetCurrency?.code ?: ""
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(7, 30, 90).forEach { days ->
                                        FilterChip(
                                            selected = uiState.selectedDateRange == days,
                                            onClick = { viewModel.onDateRangeChange(days) },
                                            label = {
                                                Text(
                                                    text = stringResource(
                                                        R.string.chart_range_days,
                                                        days
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                when {
                                    uiState.filteredHistoricalRates.size >= 2 -> {
                                        RateChart(
                                            dataPoints = uiState.filteredHistoricalRates.map { it.date to it.rate }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        RateChartSummary(
                                            dataPoints = uiState.filteredHistoricalRates.map { it.date to it.rate }
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = stringResource(R.string.historical_rates_unavailable),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (recentConversions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.recent_conversions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("recent_conversions_header")
                        )
                    }

                    items(recentConversions.take(5)) { conversion ->
                        RecentConversionItem(conversion = conversion)
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    CurrencyPickerBottomSheet(
        isVisible = showSourceCurrencyPicker,
        currencies = currencies,
        selectedCurrency = uiState.sourceCurrency,
        recentCurrencies = uiState.recentCurrencies,
        onCurrencySelected = { currency ->
            viewModel.onSourceCurrencyChange(currency)
            showSourceCurrencyPicker = false
        },
        onDismiss = { showSourceCurrencyPicker = false },
        onFavoriteToggle = viewModel::onFavoriteToggle
    )

    CurrencyPickerBottomSheet(
        isVisible = showTargetCurrencyPicker,
        currencies = currencies,
        selectedCurrency = uiState.targetCurrency,
        recentCurrencies = uiState.recentCurrencies,
        onCurrencySelected = { currency ->
            viewModel.onTargetCurrencyChange(currency)
            showTargetCurrencyPicker = false
        },
        onDismiss = { showTargetCurrencyPicker = false },
        onFavoriteToggle = viewModel::onFavoriteToggle
    )

    CurrencyPickerBottomSheet(
        isVisible = showFavoritesPicker,
        currencies = currencies,
        selectedCurrency = uiState.sourceCurrency,
        recentCurrencies = emptyList(),
        onCurrencySelected = { currency ->
            viewModel.onSourceCurrencyChange(currency)
            showFavoritesPicker = false
        },
        onDismiss = { showFavoritesPicker = false },
        onFavoriteToggle = viewModel::onFavoriteToggle
    )
}
