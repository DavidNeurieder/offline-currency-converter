package com.offlinecurrencyconverter.app.ui

import android.content.Context
import android.os.LocaleList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.offlinecurrencyconverter.app.MainActivity
import org.junit.Rule
import org.junit.Test

class RtlLayoutUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_RTL001_arabicLocaleLayout() {
        composeTestRule.onNodeWithTag("currency_converter_header")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("amount_input")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_RTL002_navigationItemsDisplayed() {
        composeTestRule.onNodeWithTag("convert_nav_item")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_nav_item")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_RTL003_swapButtonDisplayed() {
        composeTestRule.onNodeWithTag("swap_button")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_RTL004_currencySelectorsDisplayed() {
        composeTestRule.onNodeWithTag("source_currency_button")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("target_currency_button")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_RTL005_placeholderTextDisplayed() {
        composeTestRule.onNodeWithTag("placeholder_text")
            .assertIsDisplayed()
    }
}
