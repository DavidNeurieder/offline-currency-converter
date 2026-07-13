package com.offlinecurrencyconverter.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.offlinecurrencyconverter.app.ui.convert.ConvertScreen
import com.offlinecurrencyconverter.app.ui.navigation.Screen
import com.offlinecurrencyconverter.app.ui.settings.SettingsScreen

@Composable
fun OfflineCurrencyConverterNavHost() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Convert.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Convert.route) {
                ConvertScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
