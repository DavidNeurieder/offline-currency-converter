package com.offlinecurrencyconverter.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.offlinecurrencyconverter.app.data.local.dao.ExchangeRateDao
import com.offlinecurrencyconverter.app.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExchangeRateDaoTest {

    private lateinit var database: CurrencyDatabase
    private lateinit var exchangeRateDao: ExchangeRateDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CurrencyDatabase::class.java
        ).build()
        exchangeRateDao = database.exchangeRateDao()
    }

    @Test
    fun insertRate_savesAndRetrieves() = runBlocking {
        val rate = ExchangeRateEntity(
            baseCurrency = "USD",
            targetCurrency = "EUR",
            rate = 0.92,
            lastUpdated = System.currentTimeMillis(),
            isOfflineAvailable = true
        )

        exchangeRateDao.insertRate(rate)

        val result = exchangeRateDao.getRate("USD", "EUR")
        assertNotNull(result)
        assertEquals("USD", result!!.baseCurrency)
        assertEquals("EUR", result.targetCurrency)
        assertEquals(0.92, result.rate, 0.001)
    }

    @Test
    fun insertRates_batchInsertWorks() = runBlocking {
        val rates = listOf(
            ExchangeRateEntity("USD", "EUR", 0.92, System.currentTimeMillis()),
            ExchangeRateEntity("USD", "GBP", 0.79, System.currentTimeMillis()),
            ExchangeRateEntity("USD", "JPY", 149.50, System.currentTimeMillis())
        )

        exchangeRateDao.insertRates(rates)

        val result = exchangeRateDao.getRatesForCurrency("USD").first()
        assertEquals(3, result.size)
    }

    @Test
    fun getRatesForCurrency_filtersCorrectly() = runBlocking {
        exchangeRateDao.insertRates(listOf(
            ExchangeRateEntity("USD", "EUR", 0.92, System.currentTimeMillis()),
            ExchangeRateEntity("USD", "GBP", 0.79, System.currentTimeMillis()),
            ExchangeRateEntity("EUR", "USD", 1.09, System.currentTimeMillis())
        ))

        val result = exchangeRateDao.getRatesForCurrency("USD").first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.baseCurrency == "USD" })
    }

    @Test
    fun getOfflineAvailableRates_returnsOnlyOfflineAvailable() = runBlocking {
        exchangeRateDao.insertRates(listOf(
            ExchangeRateEntity("USD", "EUR", 0.92, System.currentTimeMillis(), isOfflineAvailable = true),
            ExchangeRateEntity("USD", "GBP", 0.79, System.currentTimeMillis(), isOfflineAvailable = false),
            ExchangeRateEntity("USD", "JPY", 149.50, System.currentTimeMillis(), isOfflineAvailable = true)
        ))

        val result = exchangeRateDao.getOfflineAvailableRates().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isOfflineAvailable })
    }

    @Test
    fun updateRate_changesValue() = runBlocking {
        exchangeRateDao.insertRate(
            ExchangeRateEntity("USD", "EUR", 0.92, System.currentTimeMillis())
        )

        exchangeRateDao.insertRate(
            ExchangeRateEntity("USD", "EUR", 0.95, System.currentTimeMillis())
        )

        val result = exchangeRateDao.getRate("USD", "EUR")
        assertNotNull(result)
        assertEquals(0.95, result!!.rate, 0.001)
    }

    @Test
    fun deleteNonOfflineRates_removesNonOffline() = runBlocking {
        exchangeRateDao.insertRates(listOf(
            ExchangeRateEntity("USD", "EUR", 0.92, System.currentTimeMillis(), isOfflineAvailable = true),
            ExchangeRateEntity("USD", "GBP", 0.79, System.currentTimeMillis(), isOfflineAvailable = false),
            ExchangeRateEntity("USD", "JPY", 149.50, System.currentTimeMillis(), isOfflineAvailable = false)
        ))

        exchangeRateDao.deleteNonOfflineRates()

        val result = exchangeRateDao.getRatesForCurrency("USD").first()
        assertEquals(1, result.size)
        assertTrue(result[0].isOfflineAvailable)
    }

    @Test
    fun deleteAll_removesAllRates() = runBlocking {
        exchangeRateDao.insertRates(listOf(
            ExchangeRateEntity("USD", "EUR", 0.92, System.currentTimeMillis()),
            ExchangeRateEntity("USD", "GBP", 0.79, System.currentTimeMillis())
        ))

        exchangeRateDao.deleteAll()

        val result = exchangeRateDao.getRatesForCurrency("USD").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getLastUpdateTime_returnsMaxTimestamp() = runBlocking {
        val timestamp1 = System.currentTimeMillis() - 1000
        val timestamp2 = System.currentTimeMillis()
        exchangeRateDao.insertRates(listOf(
            ExchangeRateEntity("USD", "EUR", 0.92, timestamp1),
            ExchangeRateEntity("USD", "GBP", 0.79, timestamp2)
        ))

        val result = exchangeRateDao.getLastUpdateTime()

        assertNotNull(result)
        assertEquals(timestamp2, result)
    }
}
