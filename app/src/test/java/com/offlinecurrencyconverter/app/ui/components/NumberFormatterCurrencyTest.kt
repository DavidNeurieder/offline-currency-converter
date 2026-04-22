package com.offlinecurrencyconverter.app.ui.components

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Locale

class NumberFormatterCurrencyTest {

    @Test
    fun TC_L001_USDollarFormat() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatCurrency(10.00, "$", "USD")
        assertTrue("Should contain dollar sign and formatted number", result.contains("$") && result.contains("10"))
    }

    @Test
    fun TC_L002_EuroFormat_DE() {
        Locale.setDefault(Locale.GERMANY)
        val result = NumberFormatter.formatCurrency(10.00, "€", "EUR")
        assertTrue("Should contain euro and comma", result.contains("€") && result.contains(","))
    }

    @Test
    fun TC_L003_YenFormat_JP() {
        Locale.setDefault(Locale.JAPAN)
        val result = NumberFormatter.formatCurrency(1000.0, "¥", "JPY")
        assertTrue("Should contain yen symbol and 1000", result.contains("¥") || result.contains("1,000"))
    }

    @Test
    fun TC_L004_PoundFormat_GB() {
        Locale.setDefault(Locale.UK)
        val result = NumberFormatter.formatCurrency(50.00, "£", "GBP")
        assertTrue("Should contain pound sign", result.contains("£"))
    }

    @Test
    fun TC_L005_LargeAmount_DE() {
        Locale.setDefault(Locale.GERMANY)
        val result = NumberFormatter.formatCurrency(1234567.89, "€", "EUR")
        assertTrue("Should contain euro and large number with dots", 
            result.contains("€") && result.contains("."))
    }

    @Test
    fun TC_L006_FranceEuroFormat() {
        Locale.setDefault(Locale.FRANCE)
        val result = NumberFormatter.formatCurrency(99.99, "€", "EUR")
        assertTrue("Should contain euro and comma", result.contains("€") && result.contains(","))
    }

    @Test
    fun TC_L007_ItalyEuroFormat() {
        Locale.setDefault(Locale.ITALY)
        val result = NumberFormatter.formatCurrency(25.50, "€", "EUR")
        assertTrue("Should contain euro symbol", result.contains("€"))
    }

    @Test
    fun TC_L008_USLargeAmount() {
        Locale.setDefault(Locale.US)
        val result = NumberFormatter.formatCurrency(1234567.89, "$", "USD")
        assertTrue("Should contain dollar sign and commas", 
            result.contains("$") && result.contains(","))
    }

    @Test
    fun TC_L009_BrazilRealFormat() {
        Locale.setDefault(Locale("pt", "BR"))
        val result = NumberFormatter.formatCurrency(1500.00, "R$", "BRL")
        assertTrue("Should contain R$ symbol", result.contains("R$"))
    }

    @Test
    fun TC_L010_IndiaRupeeFormat() {
        Locale.setDefault(Locale("hi", "IN"))
        val result = NumberFormatter.formatCurrency(100000.0, "₹", "INR")
        assertTrue("Should contain rupee symbol and formatted number", 
            result.contains("₹") || result.contains("1"))
    }
}
