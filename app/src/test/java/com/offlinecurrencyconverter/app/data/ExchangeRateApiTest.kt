package com.offlinecurrencyconverter.app.data

import com.offlinecurrencyconverter.app.MockApiServer
import com.offlinecurrencyconverter.app.TestFixtures
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ExchangeRateApiTest : MockApiServer() {

    private lateinit var api: FrankfurterApi

    @Before
    fun setupApi() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        api = retrofit.create(FrankfurterApi::class.java)
    }

    @Test
    fun `fetch rates returns success`() = runTest {
        enqueueSuccess(TestFixtures.apiSuccessResponse)

        val response = api.getRates("EUR", null)

        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        val rates = response.body()!!
        assertTrue(rates.isNotEmpty())
        assertEquals("EUR", rates[0].base)
    }

    @Test
    fun `fetch rates parses rates correctly`() = runTest {
        enqueueSuccess(TestFixtures.apiSuccessResponse)

        val response = api.getRates("EUR", null)

        assertTrue(response.isSuccessful)
        val rates = response.body()!!
        assertTrue(rates.isNotEmpty())
        assertEquals("USD", rates[0].quote)
        assertEquals(1.0873, rates[0].rate, 0.0001)
        assertEquals("GBP", rates[1].quote)
        assertEquals(0.8562, rates[1].rate, 0.0001)
    }

    @Test
    fun `fetch rates with quotes filter`() = runTest {
        val filteredResponse = """
            [
                {"date": "2024-01-15", "base": "EUR", "quote": "USD", "rate": 1.0873},
                {"date": "2024-01-15", "base": "EUR", "quote": "GBP", "rate": 0.8562}
            ]
        """.trimIndent()
        enqueueSuccess(filteredResponse)

        val response = api.getRates("EUR", "USD,GBP")

        assertTrue(response.isSuccessful)
        val rates = response.body()!!
        assertEquals(2, rates.size)
    }

    @Test
    fun `fetch rates handles server error`() = runTest {
        enqueueError(500, "Internal Server Error")

        val response = api.getRates("EUR", null)

        assertTrue(!response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `fetch rates handles not found`() = runTest {
        enqueueError(404, "Not Found")

        val response = api.getRates("XXX", null)

        assertTrue(!response.isSuccessful)
        assertEquals(404, response.code())
    }

    @Test
    fun `fetch rates handles malformed response`() = runTest {
        enqueueMalformed()

        var threwException = false
        try {
            api.getRates("EUR", null)
        } catch (e: Exception) {
            threwException = true
        }
        assertTrue(threwException)
    }

    @Test
    fun `fetch rates handles empty response`() = runTest {
        enqueueSuccess("[]")

        val response = api.getRates("EUR", null)

        assertTrue(response.isSuccessful)
        val rates = response.body()
        assertTrue(rates?.isEmpty() == true)
    }

    @Test
    fun `fetch currencies returns currency list`() = runTest {
        enqueueSuccess(TestFixtures.apiCurrencyListResponse)

        val response = api.getCurrencies()

        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        val currencies = response.body()!!
        assertTrue(currencies.isNotEmpty())
        assertTrue(currencies.any { it.isoCode == "USD" })
        assertTrue(currencies.any { it.isoCode == "EUR" })
    }

    @Test
    fun `rates are updated for different base currencies`() = runTest {
        val responseWithUSD = """
            [
                {"date": "2024-01-15", "base": "USD", "quote": "EUR", "rate": 0.92},
                {"date": "2024-01-15", "base": "USD", "quote": "GBP", "rate": 0.79}
            ]
        """.trimIndent()
        enqueueSuccess(responseWithUSD)

        val response = api.getRates("USD", null)

        assertTrue(response.isSuccessful)
        val rates = response.body()!!
        assertEquals("USD", rates[0].base)
        assertEquals("EUR", rates[0].quote)
        assertEquals(0.92, rates[0].rate, 0.0)
    }
}
