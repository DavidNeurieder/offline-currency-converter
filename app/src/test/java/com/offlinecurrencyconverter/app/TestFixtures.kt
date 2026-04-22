package com.offlinecurrencyconverter.app

import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.model.ExchangeRate

object TestFixtures {
    val USD = Currency("USD", "US Dollar", "$", true)
    val EUR = Currency("EUR", "Euro", "€", true)
    val GBP = Currency("GBP", "British Pound", "£", true)
    val JPY = Currency("JPY", "Japanese Yen", "¥", false)
    val AUD = Currency("AUD", "Australian Dollar", "A$", false)

    val currencies = listOf(USD, EUR, GBP, JPY, AUD)

    val exchangeRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 149.50,
        "AUD" to 1.53
    )

    fun createExchangeRate(
        baseCurrency: String = "USD",
        targetCurrency: String = "EUR",
        rate: Double = 0.92,
        isOfflineAvailable: Boolean = true
    ) = ExchangeRate(
        baseCurrency = baseCurrency,
        targetCurrency = targetCurrency,
        rate = rate,
        lastUpdated = System.currentTimeMillis(),
        isOfflineAvailable = isOfflineAvailable
    )

    fun createConversionResult(
        sourceAmount: Double = 100.0,
        sourceCurrency: Currency = USD,
        targetAmount: Double = 92.0,
        targetCurrency: Currency = EUR,
        rate: Double = 0.92
    ) = ConversionResult(
        sourceAmount = sourceAmount,
        sourceCurrency = sourceCurrency,
        targetAmount = targetAmount,
        targetCurrency = targetCurrency,
        rate = rate,
        timestamp = System.currentTimeMillis()
    )

    val priceTagUS = "\$19.99"
    val priceTagEU = "€15,00"
    val priceTagUK = "£5.50"
    val priceTagJP = "¥1000"
    val priceTagIN = "₹500"
    val priceTagMultiple = "Total: \$25.00 + €10.00"
    val priceTagWithSpaces = "USD  1,234.56"
    val priceTagDecimalOnly = "99.99"
    val priceTagGarbage = "asdfghjkl"

    val apiSuccessResponse = """
        [
            {"date": "2024-01-15", "base": "EUR", "quote": "USD", "rate": 1.0873},
            {"date": "2024-01-15", "base": "EUR", "quote": "GBP", "rate": 0.8562},
            {"date": "2024-01-15", "base": "EUR", "quote": "JPY", "rate": 163.45}
        ]
    """.trimIndent()

    val apiCurrencyListResponse = """
        [
            {"iso_code": "AUD", "iso_numeric": "036", "name": "Australian Dollar", "symbol": "$"},
            {"iso_code": "EUR", "iso_numeric": "978", "name": "Euro", "symbol": "€"},
            {"iso_code": "GBP", "iso_numeric": "826", "name": "British Pound", "symbol": "£"},
            {"iso_code": "USD", "iso_numeric": "840", "name": "US Dollar", "symbol": "$"}
        ]
    """.trimIndent()
}
