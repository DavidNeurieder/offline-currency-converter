package com.offlinecurrencyconverter.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.offlinecurrencyconverter.app.R
import com.offlinecurrencyconverter.app.domain.model.Currency

private val FREQUENTLY_USED_CODES = listOf("USD", "EUR", "GBP")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
    isVisible: Boolean,
    currencies: List<Currency>,
    selectedCurrency: Currency?,
    recentCurrencies: List<Currency>,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (isVisible) {
        var searchQuery by remember { mutableStateOf("") }

        val filteredCurrencies = remember(searchQuery, currencies) {
            if (searchQuery.isBlank()) null
            else currencies.filter {
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.symbol.contains(searchQuery, ignoreCase = true)
            }
        }

        val frequentlyUsed = remember(currencies) {
            currencies.filter { it.code in FREQUENTLY_USED_CODES }
                .sortedBy { FREQUENTLY_USED_CODES.indexOf(it.code) }
        }

        val recentFiltered = remember(recentCurrencies, currencies, selectedCurrency) {
            recentCurrencies.filter { it.code != selectedCurrency?.code }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = Modifier.height(500.dp).testTag("currency_picker_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.select_currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).testTag("select_currency_title")
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_picker_button")) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_currencies)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_currencies)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("currency_search_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (searchQuery.isBlank()) {
                        if (recentFiltered.isNotEmpty()) {
                            item {
                                SectionHeader(stringResource(R.string.recently_used))
                            }
                            items(recentFiltered, key = { "recent_${it.code}" }) { currency ->
                                CurrencyListItem(
                                    currency = currency,
                                    isSelected = currency.code == selectedCurrency?.code,
                                    onClick = { onCurrencySelected(currency) }
                                )
                            }
                        }

                        item {
                            SectionHeader(
                                stringResource(R.string.frequently_used),
                                modifier = Modifier.testTag("frequently_used_header")
                            )
                        }
                        items(frequentlyUsed, key = { "freq_${it.code}" }) { currency ->
                            CurrencyListItem(
                                currency = currency,
                                isSelected = currency.code == selectedCurrency?.code,
                                onClick = { onCurrencySelected(currency) }
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            if (searchQuery.isBlank()) stringResource(R.string.all_currencies) 
                            else stringResource(R.string.search_results)
                        )
                    }

                    val displayCurrencies = if (searchQuery.isBlank()) {
                        currencies.filter { it.code !in FREQUENTLY_USED_CODES }
                    } else {
                        filteredCurrencies ?: emptyList()
                    }

                    items(displayCurrencies, key = { "all_${it.code}" }) { currency ->
                        CurrencyListItem(
                            currency = currency,
                            isSelected = currency.code == selectedCurrency?.code,
                            onClick = { onCurrencySelected(currency) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun CurrencyListItem(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                currency.flagUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "${currency.name} flag",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = currency.code,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currency.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingContent = {
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isSelected) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
