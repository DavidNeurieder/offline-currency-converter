package com.offlinecurrencyconverter.app.data.remote.api

import com.offlinecurrencyconverter.app.data.remote.dto.CurrencyItem
import com.offlinecurrencyconverter.app.data.remote.dto.ExchangeRateItem
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FrankfurterApi {
    @GET("v2/rates")
    suspend fun getRates(
        @Query("base") baseCurrency: String,
        @Query("quotes") targetCurrencies: String? = null
    ): Response<List<ExchangeRateItem>>

    @GET("v2/rates")
    suspend fun getHistoricalRates(
        @Query("base") baseCurrency: String,
        @Query("quotes") targetCurrencies: String? = null,
        @Query("from") startDate: String,
        @Query("to") endDate: String
    ): Response<List<ExchangeRateItem>>

    @GET("v2/currencies")
    suspend fun getCurrencies(): Response<List<CurrencyItem>>
}
