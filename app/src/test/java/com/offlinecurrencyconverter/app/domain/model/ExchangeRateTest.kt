package com.offlinecurrencyconverter.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateTest {

    @Test
    fun `exchange rate stores all values correctly`() {
        val lastUpdated = System.currentTimeMillis()
        val rate = ExchangeRate(
            baseCurrency = "USD",
            targetCurrency = "EUR",
            rate = 0.92,
            lastUpdated = lastUpdated,
            isOfflineAvailable = true
        )

        assertEquals("USD", rate.baseCurrency)
        assertEquals("EUR", rate.targetCurrency)
        assertEquals(0.92, rate.rate, 0.0)
        assertEquals(lastUpdated, rate.lastUpdated)
        assertTrue(rate.isOfflineAvailable)
    }

    @Test
    fun `exchange rate offline availability defaults to false`() {
        val rate = ExchangeRate(
            baseCurrency = "USD",
            targetCurrency = "EUR",
            rate = 0.92,
            lastUpdated = System.currentTimeMillis()
        )

        assertFalse(rate.isOfflineAvailable)
    }

    @Test
    fun `exchange rate with zero rate`() {
        val rate = ExchangeRate(
            baseCurrency = "USD",
            targetCurrency = "XXX",
            rate = 0.0,
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals(0.0, rate.rate, 0.0)
    }

    @Test
    fun `exchange rate with very large rate`() {
        val rate = ExchangeRate(
            baseCurrency = "USD",
            targetCurrency = "JPY",
            rate = 150.5,
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals(150.5, rate.rate, 0.0)
    }

    @Test
    fun `exchange rate with very small rate`() {
        val rate = ExchangeRate(
            baseCurrency = "USD",
            targetCurrency = "XXX",
            rate = 0.0001,
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals(0.0001, rate.rate, 0.00001)
    }

    @Test
    fun `exchange rate equality based on currencies`() {
        val rate1 = ExchangeRate(
            baseCurrency = "USD",
            targetCurrency = "EUR",
            rate = 0.92,
            lastUpdated = System.currentTimeMillis()
        )
        val rate2 = ExchangeRate(
            baseCurrency = "USD",
            targetCurrency = "EUR",
            rate = 0.95,
            lastUpdated = System.currentTimeMillis()
        )

        assertTrue(rate1 == rate2)
    }

    @Test
    fun `exchange rate different bases are not equal`() {
        val rate1 = ExchangeRate("USD", "EUR", 0.92, System.currentTimeMillis())
        val rate2 = ExchangeRate("GBP", "EUR", 1.15, System.currentTimeMillis())

        assertFalse(rate1 == rate2)
    }
}
