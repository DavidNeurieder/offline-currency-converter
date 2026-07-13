package com.offlinecurrencyconverter.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.offlinecurrencyconverter.app.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_N001_appStartsOnConvertScreen() {
        composeTestRule.onNodeWithTag("currency_converter_header")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("open_settings")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_N002_navigateToSettings() {
        composeTestRule.onNodeWithTag("open_settings")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exchange_rate_sync")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_N003_navigateBackToConvert() {
        composeTestRule.onNodeWithTag("open_settings")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_back")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_converter_header")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("amount_input")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_N004_settingsIconDisplayedOnConvertScreen() {
        composeTestRule.onNodeWithTag("open_settings")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("open_favorites_picker")
            .assertIsDisplayed()
    }
}
