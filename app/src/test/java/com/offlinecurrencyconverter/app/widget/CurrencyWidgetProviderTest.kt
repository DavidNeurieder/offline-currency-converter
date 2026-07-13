package com.offlinecurrencyconverter.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CurrencyWidgetProviderTest {

    @Test
    fun `widget provider class exists`() {
        val provider = CurrencyWidgetProvider()
        assertNotNull(provider)
    }

    @Test
    fun `ACTION_UPDATE_WIDGET constant has correct value`() {
        assertEquals(
            "com.offlinecurrencyconverter.app.ACTION_UPDATE_WIDGET",
            CurrencyWidgetProvider.ACTION_UPDATE_WIDGET
        )
    }

    @Test
    fun `widget provider can be instantiated`() {
        val provider = CurrencyWidgetProvider()
        assertNotNull(provider)
    }
}
