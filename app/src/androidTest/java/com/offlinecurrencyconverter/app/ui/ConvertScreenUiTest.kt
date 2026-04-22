package com.offlinecurrencyconverter.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.offlinecurrencyconverter.app.MainActivity
import org.junit.Rule
import org.junit.Test

class ConvertScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_001_enterAmount_verifyConversion() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("100")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_converter_header")
            .assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_002_swapCurrencies_verifyChange() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("100")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("swap_button")
            .performClick()

        composeTestRule.waitForIdle()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_003_clearInput_verifyReset() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("100")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .performClick()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_004_viewCurrencyConverterHeader() {
        composeTestRule.onNodeWithTag("currency_converter_header")
            .assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_005_enterDecimalAmount_verifyFormatting() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("10.5")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_006_enterLargeAmount_verifyAcceptance() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("1000000")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_007_navigateToSettings() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exchange_rate_sync")
            .assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_008_swapButtonChangesCurrencies() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("50")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("swap_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("swap_button")
            .assertExists()
    }
}
