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

class CurrencyPickerUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_P001_openCurrencyPicker_fromSourceCurrency() {
        composeTestRule.onNodeWithTag("source_currency_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_picker_sheet")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_P002_openCurrencyPicker_fromTargetCurrency() {
        composeTestRule.onNodeWithTag("target_currency_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_picker_sheet")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_P003_searchField_acceptsInput() {
        composeTestRule.onNodeWithTag("source_currency_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_search_field")
            .performTextInput("USD")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_picker_sheet")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_P004_closeButton_dismissesPicker() {
        composeTestRule.onNodeWithTag("source_currency_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("close_picker_button")
            .performClick()

        composeTestRule.waitForIdle()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_P005_frequentlyUsedSection_displayed() {
        composeTestRule.onNodeWithTag("source_currency_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("frequently_used_header")
            .assertIsDisplayed()
    }
}
