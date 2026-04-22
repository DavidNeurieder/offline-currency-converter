package com.offlinecurrencyconverter.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.offlinecurrencyconverter.app.R

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Convert : Screen(
        route = "convert",
        titleResId = R.string.nav_convert,
        selectedIcon = Icons.Filled.SwapHoriz,
        unselectedIcon = Icons.Outlined.SwapHoriz
    )

    data object Settings : Screen(
        route = "settings",
        titleResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavItems = listOf(
    Screen.Convert,
    Screen.Settings
)
