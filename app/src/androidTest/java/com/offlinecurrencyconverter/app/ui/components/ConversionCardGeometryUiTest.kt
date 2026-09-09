package com.offlinecurrencyconverter.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.ui.theme.OfflineCurrencyConverterTheme
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversionCardGeometryUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val usd = Currency(code = "USD", name = "US Dollar", symbol = "$")
    private val eur = Currency(code = "EUR", name = "Euro", symbol = "€")

    private fun result(amount: Double) = ConversionResult(
        sourceAmount = amount,
        sourceCurrency = usd,
        targetAmount = amount * 0.85,
        targetCurrency = eur,
        rate = 0.85
    )

    @Test
    fun cardGeometry_staysStableAcrossStates() {
        assertGeometryStable(fontScale = 1f)
    }

    @Test
    fun cardGeometry_staysStableAcrossStates_largeFontScale() {
        assertGeometryStable(fontScale = 1.5f)
    }

    private fun assertGeometryStable(fontScale: Float) {
        var conversionResult by mutableStateOf<ConversionResult?>(null)
        var error by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                OfflineCurrencyConverterTheme {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ConversionCard(
                            amount = "100",
                            onAmountChange = {},
                            sourceCurrency = usd,
                            targetCurrency = eur,
                            onSourceCurrencyClick = {},
                            onTargetCurrencyClick = {},
                            onSwap = {},
                            conversionResult = conversionResult,
                            error = error,
                            lastSyncTime = null
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .testTag("below_card_marker")
                        )
                    }
                }
            }
        }

        fun belowMarkerTop(): Float =
            composeTestRule.onNodeWithTag("below_card_marker")
                .fetchSemanticsNode()
                .boundsInRoot
                .top

        fun assertStableFrom(initialY: Float) {
            assertTrue(
                "element below the card moved (expected stable at $initialY)",
                abs(belowMarkerTop() - initialY) < 0.5f
            )
        }

        composeTestRule.waitForIdle()
        val emptyY = belowMarkerTop()

        error = "Conversion failed"
        composeTestRule.waitForIdle()
        assertStableFrom(emptyY)

        error = null
        conversionResult = result(10.0)
        composeTestRule.waitForIdle()
        assertStableFrom(emptyY)

        conversionResult = result(1_234_567_890.12)
        composeTestRule.waitForIdle()
        assertStableFrom(emptyY)

        error = "Conversion failed"
        conversionResult = null
        composeTestRule.waitForIdle()
        assertStableFrom(emptyY)

        error = null
        conversionResult = result(123.45)
        composeTestRule.waitForIdle()
        assertStableFrom(emptyY)
    }
}