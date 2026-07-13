package com.offlinecurrencyconverter.app.ui.navigation

import com.offlinecurrencyconverter.app.R

sealed class Screen(
    val route: String,
    val titleResId: Int
) {
    data object Convert : Screen(
        route = "convert",
        titleResId = R.string.nav_convert
    )

    data object Settings : Screen(
        route = "settings",
        titleResId = R.string.nav_settings
    )
}
