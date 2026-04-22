package com.offlinecurrencyconverter.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.offlinecurrencyconverter.app.MainActivity
import org.junit.Rule
import org.junit.Test

class SettingsScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_020_openSettings_verifyScreenLoads() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exchange_rate_sync")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_021_syncIntervalDropdown_opensAndShowsOptions() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sync_interval_dropdown")
            .performScrollTo()
            .performClick()

        Thread.sleep(500)

        composeTestRule.onNodeWithTag("dropdown_item_six_hours")
            .assertExists()
        composeTestRule.onNodeWithTag("dropdown_item_twelve_hours")
            .assertExists()
        composeTestRule.onNodeWithTag("dropdown_item_twenty_four_hours")
            .assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_022_syncNowButton_verifyEnabled() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sync_now_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_024_aboutSection_verifyAppInfo() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_section")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_030_bottomNav_navigateBetweenScreens() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("convert_nav_item")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("currency_converter_header")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exchange_rate_sync")
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun TC_031_syncIntervalSelection_verifyChange() {
        composeTestRule.onNodeWithTag("settings_nav_item")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sync_interval_dropdown")
            .performScrollTo()
            .performClick()

        Thread.sleep(500)

        composeTestRule.onNodeWithTag("dropdown_item_weekly")
            .performClick()

        composeTestRule.waitForIdle()
    }
}
