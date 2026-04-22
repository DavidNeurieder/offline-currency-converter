package com.offlinecurrencyconverter.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlinecurrencyconverter.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.syncSuccess) {
        if (uiState.syncSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.sync_completed))
            viewModel.clearSyncStatus()
        }
    }

    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let { error ->
            snackbarHostState.showSnackbar(context.getString(R.string.sync_failed, error))
            viewModel.clearSyncStatus()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbarHostState)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SyncSection(
                    syncInterval = uiState.syncInterval,
                    onSyncIntervalChange = viewModel::onSyncIntervalChange,
                    lastSyncTime = uiState.lastSyncTime,
                    isSyncing = uiState.isSyncing,
                    onSyncNow = viewModel::syncNow
                )
            }

            item {
                AboutSection()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncSection(
    syncInterval: SyncInterval,
    onSyncIntervalChange: (SyncInterval) -> Unit,
    lastSyncTime: Long?,
    isSyncing: Boolean,
    onSyncNow: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = stringResource(R.string.exchange_rate_sync),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("exchange_rate_sync")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = getSyncIntervalDisplayName(syncInterval),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.sync_interval)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("sync_interval_dropdown")
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SyncInterval.entries.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(getSyncIntervalDisplayName(interval)) },
                            onClick = {
                                onSyncIntervalChange(interval)
                                expanded = false
                            },
                            modifier = Modifier.testTag("dropdown_item_${interval.name.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val syncStatus = formatSyncStatus(lastSyncTime)
            Text(
                text = syncStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSyncNow,
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth().testTag("sync_now_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(if (isSyncing) stringResource(R.string.syncing) else stringResource(R.string.sync_now))
            }
        }
    }
}

@Composable
private fun formatSyncStatus(lastSyncTime: Long?): String {
    return when {
        lastSyncTime == null -> stringResource(R.string.never_synced)
        else -> {
            val now = System.currentTimeMillis()
            val diff = now - lastSyncTime
            val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastSyncTime))
            
            when {
                diff < 60_000 -> stringResource(R.string.last_synced_just_now)
                diff < 3600_000 -> stringResource(R.string.last_synced_minutes, (diff / 60_000).toInt())
                else -> stringResource(R.string.last_synced_date, dateStr)
            }
        }
    }
}

@Composable
private fun getSyncIntervalDisplayName(interval: SyncInterval): String {
    return when (interval) {
        SyncInterval.SIX_HOURS -> stringResource(R.string.sync_interval_6_hours)
        SyncInterval.TWELVE_HOURS -> stringResource(R.string.sync_interval_12_hours)
        SyncInterval.TWENTY_FOUR_HOURS -> stringResource(R.string.sync_interval_24_hours)
        SyncInterval.FORTY_EIGHT_HOURS -> stringResource(R.string.sync_interval_48_hours)
        SyncInterval.WEEKLY -> stringResource(R.string.sync_interval_weekly)
    }
}

@Composable
private fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("about_section")
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.version_format, "1.0.0"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
