package com.offlinecurrencyconverter.app.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    private val localeDisplayFormat = DecimalFormat("#,##0.00")

    fun format(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = currency
            format.format(amount)
        } catch (e: Exception) {
            formatAmount(amount, currencyCode)
        }
    }

    fun formatAmount(amount: Double, currencyCode: String): String {
        return "$currencyCode ${localeDisplayFormat.format(amount)}"
    }

    fun formatNumber(amount: Double, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getNumberInstance(locale)
        if (format is DecimalFormat) {
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
        }
        return format.format(amount)
    }

    fun formatRate(rate: Double, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getNumberInstance(locale)
        if (format is DecimalFormat) {
            format.maximumFractionDigits = 6
            format.minimumFractionDigits = 4
        }
        return format.format(rate)
    }
}
