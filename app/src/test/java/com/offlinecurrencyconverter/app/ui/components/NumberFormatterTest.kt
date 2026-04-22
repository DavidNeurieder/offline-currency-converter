package com.offlinecurrencyconverter.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun `formatAmount with 2 decimal places`() {
        val result = NumberFormatter.formatAmount(1234.56)
        assertEquals("1,234.56", result)
    }

    @Test
    fun `formatAmount with 0 decimal places`() {
        val result = NumberFormatter.formatAmount(1234.0, 0)
        assertEquals("1,234", result)
    }

    @Test
    fun `formatAmount with 1 decimal place`() {
        val result = NumberFormatter.formatAmount(1234.5, 1)
        assertEquals("1,234.5", result)
    }

    @Test
    fun `formatAmount handles zero`() {
        val result = NumberFormatter.formatAmount(0.0)
        assertEquals("0.00", result)
    }

    @Test
    fun `formatAmount handles large numbers`() {
        val result = NumberFormatter.formatAmount(1_000_000.0)
        assertEquals("1,000,000.00", result)
    }

    @Test
    fun `formatAmount handles small decimals`() {
        val result = NumberFormatter.formatAmount(0.99)
        assertEquals("0.99", result)
    }

    @Test
    fun `formatAmount rounds correctly`() {
        val result = NumberFormatter.formatAmount(123.456, 2)
        assertEquals("123.46", result)
    }

    @Test
    fun `formatCurrency with symbol`() {
        val result = NumberFormatter.formatCurrency(19.99, "$", "USD")
        assertEquals("\$19.99", result)
    }

    @Test
    fun `formatCurrency with euro symbol`() {
        val result = NumberFormatter.formatCurrency(15.00, "€", "EUR")
        assertEquals("€15.00", result)
    }

    @Test
    fun `formatWithCode adds code before amount`() {
        val result = NumberFormatter.formatWithCode(19.99, "USD")
        assertEquals("USD 19.99", result)
    }

    @Test
    fun `formatRate formats with appropriate precision`() {
        val result = NumberFormatter.formatRate(0.923456789)
        assertEquals("0.923457", result)
    }

    @Test
    fun `formatRate handles whole numbers`() {
        val result = NumberFormatter.formatRate(1.0)
        assertEquals("1.0000", result)
    }

    @Test
    fun `formatRate handles small rates`() {
        val result = NumberFormatter.formatRate(0.0001)
        assertEquals("0.0001", result)
    }

    @Test
    fun `formatCompact handles millions`() {
        val result = NumberFormatter.formatCompact(1_500_000.0)
        assertEquals("1.5M", result)
    }

    @Test
    fun `formatCompact handles thousands`() {
        val result = NumberFormatter.formatCompact(2_500.0)
        assertEquals("2.5K", result)
    }

    @Test
    fun `formatCompact handles small numbers`() {
        val result = NumberFormatter.formatCompact(500.0)
        assertEquals("500.00", result)
    }

    @Test
    fun `parseInput parses normal number`() {
        val result = NumberFormatter.parseInput("1234.56")
        assertEquals(1234.56, result!!, 0.0)
    }

    @Test
    fun `parseInput parses with commas`() {
        val result = NumberFormatter.parseInput("1,234.56")
        assertEquals(1234.56, result!!, 0.0)
    }

    @Test
    fun `parseInput returns null for invalid input`() {
        assertNull(NumberFormatter.parseInput("abc"))
        assertNull(NumberFormatter.parseInput(""))
    }

    @Test
    fun `isValidInput returns true for valid numbers`() {
        assertTrue(NumberFormatter.isValidInput("123"))
        assertTrue(NumberFormatter.isValidInput("123.45"))
        assertTrue(NumberFormatter.isValidInput(""))
        assertTrue(NumberFormatter.isValidInput("0"))
    }

    @Test
    fun `isValidInput returns false for invalid input`() {
        assertFalse(NumberFormatter.isValidInput("abc"))
        assertFalse(NumberFormatter.isValidInput("12.34.56"))
    }
}
