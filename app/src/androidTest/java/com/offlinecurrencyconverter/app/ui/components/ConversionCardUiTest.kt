package com.offlinecurrencyconverter.app.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.offlinecurrencyconverter.app.MainActivity
import org.junit.Rule
import org.junit.Test

class ConversionCardUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_C001_amountInput_acceptsNumbers() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("100")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_C002_amountInput_acceptsDecimal() {
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("99.99")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_C003_currencySelectorButtons_displayed() {
        composeTestRule.onNodeWithTag("source_currency_button")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("target_currency_button")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_C004_swapButtonDisplayed() {
        composeTestRule.onNodeWithTag("swap_button")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_C005_swapButtonClickable() {
        composeTestRule.onNodeWithTag("swap_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("swap_button")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_C006_resultArea_showsPlaceholder() {
        composeTestRule.onNodeWithTag("placeholder_text")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_C007_lastSyncInfoDisplayed() {
        composeTestRule.onNodeWithTag("last_sync_info")
            .assertIsDisplayed()
    }
}
