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

        composeTestRule.onNodeWithTag("convert_nav_item")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_nav_item")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_N002_navigateToSettings() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exchange_rate_sync")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_nav_item")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_N003_navigateBackToConvert() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("convert_nav_item")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_converter_header")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("amount_input")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_N004_navigationPersistsState() {
        composeTestRule.onNodeWithTag("amount_input")
            .performClick()

        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("convert_nav_item")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .assertIsDisplayed()
    }
}
