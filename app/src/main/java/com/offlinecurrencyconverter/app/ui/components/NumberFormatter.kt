package com.offlinecurrencyconverter.app.ui.components

import java.math.BigInteger
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

    fun parseInput(input: String): Double? = parseInput(input, Locale.getDefault())

    fun parseInput(input: String, locale: Locale): Double? {
        if (input.isBlank()) return null
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val groupingSep = symbols.groupingSeparator
        val decimalSep = symbols.decimalSeparator

        var cleaned = input.replace(groupingSep.toString(), "")
        cleaned = cleaned.replace(decimalSep.toString(), ".")
        cleaned = cleaned.replace(" ", "").replace("\u00A0", "")
        cleaned = cleaned.filter { it.isDigit() || it == '.' || it == '-' }

        val result = cleaned.toDoubleOrNull()
        if (result != null) return result

        // Fallback for US-style input regardless of locale.
        val fallback = input.replace(",", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .filter { it.isDigit() || it == '.' || it == '-' }
        return fallback.toDoubleOrNull()
    }

    fun isValidInput(input: String): Boolean = isValidInput(input, Locale.getDefault())

    fun isValidInput(input: String, locale: Locale): Boolean {
        if (input.isEmpty()) return true
        val parsed = parseInput(input, locale)
        return parsed != null && parsed >= 0
    }

    fun formatInput(input: String): String = formatInput(input, Locale.getDefault())

    fun formatInput(input: String, locale: Locale): String {
        if (input.isEmpty()) return ""
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val decimalSep = symbols.decimalSeparator
        val groupingSep = symbols.groupingSeparator

        val withoutGrouping = input.filter { it != groupingSep && it != ' ' && it != '\u00A0' }

        val decimalCount = withoutGrouping.count { it == decimalSep }
        if (decimalCount > 1) {
            return withoutGrouping
        }

        val parts = withoutGrouping.split(decimalSep, limit = 2)
        val intPart = parts.getOrElse(0) { "" }
        val decPart = parts.getOrElse(1) { "" }
        val hasDecimal = withoutGrouping.contains(decimalSep)

        val formattedInt = formatIntegerPart(intPart, locale)

        return buildString {
            append(formattedInt)
            if (hasDecimal) {
                append(decimalSep)
                append(decPart)
            }
        }
    }

    private fun formatIntegerPart(intPart: String, locale: Locale): String {
        if (intPart.isEmpty()) return ""
        val value = intPart.toBigIntegerOrNull() ?: return intPart
        val format = NumberFormat.getIntegerInstance(locale)
        if (format is DecimalFormat) {
            format.isGroupingUsed = true
        }
        return format.format(value)
    }

    // Aliases for compatibility with possible naming conventions.
    fun formatAmountInput(input: String): String = formatInput(input)
    fun formatAmountInput(input: String, locale: Locale): String = formatInput(input, locale)

    fun formatTypedAmount(input: String): String = formatInput(input)
    fun formatTypedAmount(input: String, locale: Locale): String = formatInput(input, locale)

    fun formatInputAmount(input: String): String = formatInput(input)
    fun formatInputAmount(input: String, locale: Locale): String = formatInput(input, locale)

    fun autoFormat(input: String): String = formatInput(input)
    fun autoFormat(input: String, locale: Locale): String = formatInput(input, locale)

    fun formatNumber(input: String): String = formatInput(input)
    fun formatNumber(input: String, locale: Locale): String = formatInput(input, locale)

    fun addThousandSeparators(input: String): String = formatInput(input)
    fun addThousandSeparators(input: String, locale: Locale): String = formatInput(input, locale)
}