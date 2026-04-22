package com.offlinecurrencyconverter.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionResultTest {

    private val usd = Currency("USD", "US Dollar", "$")
    private val eur = Currency("EUR", "Euro", "€")

    @Test
    fun `conversion result stores all values correctly`() {
        val timestamp = System.currentTimeMillis()
        val result = ConversionResult(
            sourceAmount = 100.0,
            sourceCurrency = usd,
            targetAmount = 92.0,
            targetCurrency = eur,
            rate = 0.92,
            timestamp = timestamp
        )

        assertEquals(100.0, result.sourceAmount, 0.0)
        assertEquals(usd, result.sourceCurrency)
        assertEquals(92.0, result.targetAmount, 0.0)
        assertEquals(eur, result.targetCurrency)
        assertEquals(0.92, result.rate, 0.0)
        assertEquals(timestamp, result.timestamp)
    }

    @Test
    fun `conversion result uses current timestamp by default`() {
        val before = System.currentTimeMillis()
        val result = ConversionResult(
            sourceAmount = 100.0,
            sourceCurrency = usd,
            targetAmount = 92.0,
            targetCurrency = eur,
            rate = 0.92
        )
        val after = System.currentTimeMillis()

        assertTrue(result.timestamp in before..after)
    }

    @Test
    fun `conversion result with zero amounts`() {
        val result = ConversionResult(
            sourceAmount = 0.0,
            sourceCurrency = usd,
            targetAmount = 0.0,
            targetCurrency = eur,
            rate = 0.92
        )

        assertEquals(0.0, result.sourceAmount, 0.0)
        assertEquals(0.0, result.targetAmount, 0.0)
    }

    @Test
    fun `conversion result with same currency has rate of 1`() {
        val result = ConversionResult(
            sourceAmount = 100.0,
            sourceCurrency = usd,
            targetAmount = 100.0,
            targetCurrency = usd,
            rate = 1.0
        )

        assertEquals(1.0, result.rate, 0.0)
        assertEquals(result.sourceAmount, result.targetAmount, 0.0)
    }

    @Test
    fun `conversion result copy preserves values`() {
        val original = ConversionResult(
            sourceAmount = 100.0,
            sourceCurrency = usd,
            targetAmount = 92.0,
            targetCurrency = eur,
            rate = 0.92
        )
        val copy = original.copy(targetAmount = 93.0)

        assertEquals(100.0, copy.sourceAmount, 0.0)
        assertEquals(93.0, copy.targetAmount, 0.0)
        assertEquals(original.sourceCurrency, copy.sourceCurrency)
        assertEquals(original.rate, copy.rate, 0.0)
    }

    @Test
    fun `conversion result toString contains key info`() {
        val result = ConversionResult(
            sourceAmount = 100.0,
            sourceCurrency = usd,
            targetAmount = 92.0,
            targetCurrency = eur,
            rate = 0.92
        )

        val str = result.toString()
        assertTrue(str.contains("100.0"))
        assertTrue(str.contains("USD"))
        assertTrue(str.contains("92.0"))
        assertTrue(str.contains("EUR"))
    }
}
