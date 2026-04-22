package com.offlinecurrencyconverter.app.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object NumberFormatter {

    private val noDecimalFormat = DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US))

    fun formatAmount(amount: Double, decimalPlaces: Int = 2): String {
        val locale = Locale.getDefault()
        val format = NumberFormat.getNumberInstance(locale)
        if (format is DecimalFormat) {
            format.minimumFractionDigits = decimalPlaces
            format.maximumFractionDigits = decimalPlaces
        }
        return format.format(amount)
    }

    fun formatCurrency(amount: Double, symbol: String, code: String): String {
        val locale = Locale.getDefault()
        return try {
            val currency = Currency.getInstance(code)
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = currency
            format.format(amount)
        } catch (e: Exception) {
            val symbolFirst = isSymbolFirstInLocale(locale)
            val formattedAmount = formatAmount(amount)
            if (symbolFirst) {
                "$symbol$formattedAmount"
            } else {
                "$formattedAmount $symbol"
            }
        }
    }

    private fun isSymbolFirstInLocale(locale: Locale): Boolean {
        return try {
            val tempCurrency = Currency.getInstance("USD")
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = tempCurrency
            val sampleFormatted = format.format(10.00)
            val currencySymbol = tempCurrency.getSymbol(locale)
            sampleFormatted.indexOf(currencySymbol) < sampleFormatted.indexOf("10")
        } catch (e: Exception) {
            true
        }
    }

    fun formatWithCode(amount: Double, code: String): String {
        val formattedAmount = formatAmount(amount)
        return "$code $formattedAmount"
    }

    fun formatRate(rate: Double): String {
        val locale = Locale.getDefault()
        val format = NumberFormat.getNumberInstance(locale)
        if (format is DecimalFormat) {
            format.minimumFractionDigits = 4
            format.maximumFractionDigits = 6
        }
        return format.format(rate)
    }

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", amount / 1_000_000)
            amount >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1_000)
            else -> formatAmount(amount)
        }
    }

    fun parseInput(input: String): Double? {
        return try {
            input
                .replace(",", "")
                .replace(" ", "")
                .toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun isValidInput(input: String): Boolean {
        if (input.isEmpty()) return true
        val cleaned = input.replace(",", "").replace(" ", "")
        return cleaned.toDoubleOrNull() != null && cleaned.toDoubleOrNull()!! >= 0
    }
}
