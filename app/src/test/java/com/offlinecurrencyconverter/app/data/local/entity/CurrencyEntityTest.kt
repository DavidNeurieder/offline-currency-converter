package com.offlinecurrencyconverter.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyEntityTest {

    @Test
    fun `default values are correct`() {
        val entity = CurrencyEntity(
            code = "USD",
            name = "US Dollar",
            symbol = "$"
        )

        assertEquals("USD", entity.code)
        assertEquals("US Dollar", entity.name)
        assertEquals("$", entity.symbol)
        assertFalse(entity.isSelectedForOffline)
        assertNull(entity.isoNumeric)
        assertNull(entity.startDate)
        assertNull(entity.endDate)
    }

    @Test
    fun `nullable fields can be null`() {
        val entity = CurrencyEntity(
            code = "EUR",
            name = "Euro",
            symbol = "€",
            isoNumeric = null,
            startDate = null,
            endDate = null,
            isSelectedForOffline = false
        )

        assertNull(entity.isoNumeric)
        assertNull(entity.startDate)
        assertNull(entity.endDate)
    }

    @Test
    fun `all fields initialized correctly`() {
        val entity = CurrencyEntity(
            code = "GBP",
            name = "British Pound",
            symbol = "£",
            isoNumeric = "826",
            startDate = "1971-01-01",
            endDate = null,
            isSelectedForOffline = true
        )

        assertEquals("GBP", entity.code)
        assertEquals("British Pound", entity.name)
        assertEquals("£", entity.symbol)
        assertEquals("826", entity.isoNumeric)
        assertEquals("1971-01-01", entity.startDate)
        assertNull(entity.endDate)
        assertTrue(entity.isSelectedForOffline)
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
