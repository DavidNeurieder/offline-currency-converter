package com.offlinecurrencyconverter.app.ui.convert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlinecurrencyconverter.app.R
import com.offlinecurrencyconverter.app.ui.components.ConversionCard
import com.offlinecurrencyconverter.app.ui.components.CurrencyPickerBottomSheet
import com.offlinecurrencyconverter.app.ui.components.RecentConversionItem

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    viewModel: ConvertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencies by viewModel.currencies.collectAsState()
    val recentConversions by viewModel.recentConversions.collectAsState()

    var showSourceCurrencyPicker by remember { mutableStateOf(false) }
    var showTargetCurrencyPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshLastSyncTime()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.currency_converter),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("currency_converter_header")
            )
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
                lastSyncTime = uiState.lastSyncTime
            )
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

    CurrencyPickerBottomSheet(
        isVisible = showSourceCurrencyPicker,
        currencies = currencies,
        selectedCurrency = uiState.sourceCurrency,
        recentCurrencies = uiState.recentCurrencies,
        onCurrencySelected = { currency ->
            viewModel.onSourceCurrencyChange(currency)
            showSourceCurrencyPicker = false
        },
        onDismiss = { showSourceCurrencyPicker = false }
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
        onDismiss = { showTargetCurrencyPicker = false }
    )
}
