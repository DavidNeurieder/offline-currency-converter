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
        assertFalse(currency.isFavorite)
    }

    @Test
    fun `currency with offline selection flag`() {
        val currency = Currency("EUR", "Euro", "€", isSelectedForOffline = true)
        assertTrue(currency.isSelectedForOffline)
    }

    @Test
    fun `currency with favorite flag`() {
        val currency = Currency("GBP", "British Pound", "£", isFavorite = true)
        assertTrue(currency.isFavorite)
    }

    @Test
    fun `currency defaults to not favorite`() {
        val currency = Currency("USD", "US Dollar", "$")
        assertFalse(currency.isFavorite)
    }

    @Test
    fun `currency copy preserves favorite flag`() {
        val original = Currency("USD", "US Dollar", "$", isFavorite = true)
        val copy = original.copy(name = "Dollar")

        assertEquals("Dollar", copy.name)
        assertTrue(copy.isFavorite)
    }

    @Test
    fun `currency copy can toggle favorite`() {
        val original = Currency("USD", "US Dollar", "$", isFavorite = false)
        val copy = original.copy(isFavorite = true)

        assertFalse(original.isFavorite)
        assertTrue(copy.isFavorite)
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
    fun `currency equality based on all fields`() {
        val currency1 = Currency("USD", "US Dollar", "$")
        val currency2 = Currency("USD", "US Dollar", "$")
        val currency3 = Currency("USD", "US Dollar", "$", isSelectedForOffline = true, isFavorite = true)
        val currency4 = Currency("EUR", "Euro", "€")

        assertEquals(currency1, currency2)
        assertFalse(currency1 == currency3)
        assertFalse(currency1 == currency4)
    }

    @Test
    fun `currency equality considers isFavorite`() {
        val notFavorite = Currency("USD", "US Dollar", "$", isFavorite = false)
        val isFavorite = Currency("USD", "US Dollar", "$", isFavorite = true)

        assertFalse(notFavorite == isFavorite)
    }

    @Test
    fun `currency hashCode based on all fields`() {
        val currency1 = Currency("USD", "US Dollar", "$")
        val currency2 = Currency("USD", "US Dollar", "$")
        val currency3 = Currency("USD", "US Dollar", "$", isFavorite = true)

        assertEquals(currency1.hashCode(), currency2.hashCode())
        assertFalse(currency1.hashCode() == currency3.hashCode())
    }
}
