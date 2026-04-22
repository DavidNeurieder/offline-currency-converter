package com.offlinecurrencyconverter.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExchangeRateItem(
    @SerializedName("date")
    val date: String,
    @SerializedName("base")
    val base: String,
    @SerializedName("quote")
    val quote: String,
    @SerializedName("rate")
    val rate: Double
)

data class CurrencyItem(
    @SerializedName("iso_code")
    val isoCode: String,
    @SerializedName("iso_numeric")
    val isoNumeric: String?,
    @SerializedName("name")
    val name: String,
    @SerializedName("symbol")
    val symbol: String?,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("end_date")
    val endDate: String?
)

@Deprecated("Use v2 format with ExchangeRateItem")
data class ExchangeRatesResponse(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("base")
    val base: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("rates")
    val rates: Map<String, Double>
)

@Deprecated("Use v2 format with List<CurrencyItem>")
typealias CurrenciesResponse = Map<String, String>

@Deprecated("Use v2 format with List<CurrencyItem>")
typealias CurrencyListResponse = Map<String, String>
