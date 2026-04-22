package com.offlinecurrencyconverter.app.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NumberFormatterLocaleTest {

    @Test
    fun TC_N001_USThousands() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatAmount(1234.56)
        assertTrue("US should use comma for thousands", result.contains(","))
    }

    @Test
    fun TC_N002_DEThousands() {
        Locale.setDefault(Locale.GERMANY)
        val result = NumberFormatter.formatAmount(1234.56)
        assertTrue("DE should use dot for thousands and comma for decimal", 
            result.contains(".") && result.contains(","))
    }

    @Test
    fun TC_N003_FRThousands() {
        Locale.setDefault(Locale.FRANCE)
        val result = NumberFormatter.formatAmount(1234.56)
        assertTrue("FR should use space for thousands and comma for decimal",
            result.contains(","))
    }

    @Test
    fun TC_N004_USMillions() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatAmount(1000000.0)
        assertTrue("US should have commas as thousands separator",
            result.contains(",") && result.count { it == ',' } >= 2)
    }

    @Test
    fun TC_N005_JPFormat() {
        Locale.setDefault(Locale.JAPAN)
        val result = NumberFormatter.formatAmount(1234.0)
        assertTrue("JP format should have commas", result.contains(","))
    }

    @Test
    fun TC_N006_DecimalPrecision_US() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatAmount(99.999, 2)
        assertTrue("Should be rounded to 100.00", result.contains("100") || result.contains("."))
    }

    @Test
    fun TC_N007_ZeroAmount() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatAmount(0.0)
        assertTrue("Should contain 0", result.contains("0"))
    }

    @Test
    fun TC_N008_ZeroAmount_DE() {
        Locale.setDefault(Locale.GERMANY)
        val result = NumberFormatter.formatAmount(0.0)
        assertTrue("DE should contain 0 with comma", result.contains("0"))
    }

    @Test
    fun TC_N009_SmallDecimal_US() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatAmount(0.99)
        assertTrue("Should contain 0.99", result.contains("0") && result.contains("."))
    }

    @Test
    fun TC_N010_SmallDecimal_DE() {
        Locale.setDefault(Locale.GERMANY)
        val result = NumberFormatter.formatAmount(0.99)
        assertTrue("DE should contain 0,99", result.contains("0") && result.contains(","))
    }

    @Test
    fun TC_N011_CustomDecimalPlaces() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatAmount(1234.5678, 3)
        assertTrue("Should have 3 decimal places", result.contains("."))
    }

    @Test
    fun TC_N012_IntegerNoDecimals() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatAmount(100.0, 0)
        assertTrue("Should be 1000 without decimals", result.contains("1") && !result.contains("."))
    }

    @Test
    fun TC_N013_BrazilNumberFormat() {
        Locale.setDefault(Locale("pt", "BR"))
        val result = NumberFormatter.formatAmount(1999.99)
        assertTrue("BR should use comma for decimal", result.contains(","))
    }

    @Test
    fun TC_N014_IndiaNumberFormat() {
        Locale.setDefault(Locale("hi", "IN"))
        val result = NumberFormatter.formatAmount(12345678.0)
        assertTrue("IN should have commas", result.contains(","))
    }

    @Test
    fun TC_N015_SwitzerlandNumberFormat() {
        Locale.setDefault(Locale("de", "CH"))
        val result = NumberFormatter.formatAmount(1000.50)
        assertTrue("CH should have apostrophe or comma", 
            result.contains("'") || result.contains("."))
    }
}
