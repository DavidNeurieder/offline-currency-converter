package com.offlinecurrencyconverter.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyTest {

    @Test
    fun `currency code validation accepts valid codes`() {
        val currency = Currency("USD", "US Dollar", "$")
        assertEquals("USD", currency.code)
        assertEquals("US Dollar", currency.name)
        assertEquals("$", currency.symbol)
        assertFalse(currency.isSelectedForOffline)
    }

    @Test
    fun `currency with offline selection flag`() {
        val currency = Currency("EUR", "Euro", "€", isSelectedForOffline = true)
        assertTrue(currency.isSelectedForOffline)
    }

    @Test
    fun `currency copy creates new instance with updated values`() {
        val original = Currency("USD", "US Dollar", "$")
        val copy = original.copy(isSelectedForOffline = true)

        assertEquals("USD", copy.code)
        assertEquals("US Dollar", copy.name)
        assertEquals("$", copy.symbol)
        assertTrue(copy.isSelectedForOffline)
        assertFalse(original.isSelectedForOffline)
    }

    @Test
    fun `currency equality based on code`() {
        val currency1 = Currency("USD", "US Dollar", "$")
        val currency2 = Currency("USD", "US Dollar", "$", isSelectedForOffline = true)
        val currency3 = Currency("EUR", "Euro", "€")

        assertTrue(currency1 == currency2)
        assertFalse(currency1 == currency3)
    }

    @Test
    fun `currency hashCode based on code`() {
        val currency1 = Currency("USD", "US Dollar", "$")
        val currency2 = Currency("USD", "US Dollar", "$")

        assertEquals(currency1.hashCode(), currency2.hashCode())
    }
}
