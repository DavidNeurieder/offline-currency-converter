package com.offlinecurrencyconverter.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.offlinecurrencyconverter.app.R
import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import com.offlinecurrencyconverter.app.domain.model.Currency
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    sourceCurrency: Currency?,
    targetCurrency: Currency?,
    currencies: List<Currency>,
    recentCurrencies: List<Currency>,
    onSourceCurrencyClick: () -> Unit,
    onTargetCurrencyClick: () -> Unit,
    onSwap: () -> Unit,
    conversionResult: ConversionResult?,
    error: String?,
    isLoading: Boolean,
    lastSyncTime: Long?,
    modifier: Modifier = Modifier,
    detectionInfo: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (detectionInfo != null) {
                Text(
                    text = detectionInfo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                text = formatLastSyncTimeComposable(lastSyncTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("last_sync_info")
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("amount_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CurrencySelectorButton(
                    currency = sourceCurrency,
                    label = stringResource(R.string.from),
                    onClick = onSourceCurrencyClick,
                    modifier = Modifier.weight(1f).testTag("source_currency_button")
                )

                IconButton(onClick = onSwap, modifier = Modifier.testTag("swap_button")) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.swap_currencies),
                        modifier = Modifier.rotate(90f)
                    )
                }

                CurrencySelectorButton(
                    currency = targetCurrency,
                    label = stringResource(R.string.to),
                    onClick = onTargetCurrencyClick,
                    modifier = Modifier.weight(1f).testTag("target_currency_button")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Text(
                    text = stringResource(R.string.converting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else if (conversionResult != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.result),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${conversionResult.targetCurrency.symbol}${
                            DecimalFormat("#,##0.00").format(conversionResult.targetAmount)
                        }",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1 ${conversionResult.sourceCurrency.code} = ${
                            DecimalFormat("#,##0.####").format(conversionResult.rate)
                        } ${conversionResult.targetCurrency.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.enter_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("placeholder_text")
                )
            }
        }
    }
}

@Composable
fun CurrencySelectorButton(
    currency: Currency?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        if (currency != null) {
            currency.flagUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "${currency.name} flag",
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = currency.code,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun formatLastSyncTimeComposable(timestamp: Long?): String {
    return when {
        timestamp == null -> stringResource(R.string.never_synced)
        else -> {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            when {
                diff < 60_000 -> stringResource(R.string.last_synced_just_now)
                diff < 3600_000 -> stringResource(R.string.last_synced_minutes, (diff / 60_000).toInt())
                else -> {
                    val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                    stringResource(R.string.last_synced_date, sdf.format(java.util.Date(timestamp)))
                }
            }
        }
    }
}

@Composable
fun RecentConversionItem(conversion: ConversionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${conversion.sourceCurrency.symbol}${
                        DecimalFormat("#,##0.00").format(conversion.sourceAmount)
                    } ${conversion.sourceCurrency.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "→ ${conversion.targetCurrency.symbol}${
                        DecimalFormat("#,##0.00").format(conversion.targetAmount)
                    } ${conversion.targetCurrency.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = conversion.targetCurrency.code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
