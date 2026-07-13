package com.offlinecurrencyconverter.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoricalRateDaoTest {

    private lateinit var database: CurrencyDatabase
    private lateinit var historicalRateDao: HistoricalRateDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CurrencyDatabase::class.java
        ).build()
        historicalRateDao = database.historicalRateDao()
    }

    @Test
    fun insertRates_savesAndRetrieves() = runBlocking {
        val rates = listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2025-07-10"),
            HistoricalRateEntity("EUR", "USD", 1.12, "2025-07-11"),
            HistoricalRateEntity("EUR", "USD", 1.11, "2025-07-12")
        )

        historicalRateDao.insertRates(rates)
        val result = historicalRateDao.getHistoricalRates("EUR", "USD").first()

        assertEquals(3, result.size)
    }

    @Test
    fun insertRates_replaceOnConflict() = runBlocking {
        val rates = listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2025-07-10"),
            HistoricalRateEntity("EUR", "USD", 1.99, "2025-07-10")
        )

        historicalRateDao.insertRates(rates)
        val result = historicalRateDao.getHistoricalRates("EUR", "USD").first()

        assertEquals(1, result.size)
        assertEquals(1.99, result[0].rate, 0.001)
    }

    @Test
    fun getHistoricalRates_filtersByCurrencyPair() = runBlocking {
        historicalRateDao.insertRates(listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2025-07-10"),
            HistoricalRateEntity("GBP", "JPY", 190.5, "2025-07-10")
        ))

        val eurUsd = historicalRateDao.getHistoricalRates("EUR", "USD").first()
        val gbpJpy = historicalRateDao.getHistoricalRates("GBP", "JPY").first()

        assertEquals(1, eurUsd.size)
        assertEquals(1, gbpJpy.size)
        assertEquals("EUR", eurUsd[0].baseCurrency)
        assertEquals("GBP", gbpJpy[0].baseCurrency)
    }

    @Test
    fun getHistoricalRates_ordersByDateAsc() = runBlocking {
        historicalRateDao.insertRates(listOf(
            HistoricalRateEntity("EUR", "USD", 1.11, "2025-07-12"),
            HistoricalRateEntity("EUR", "USD", 1.10, "2025-07-10"),
            HistoricalRateEntity("EUR", "USD", 1.12, "2025-07-11")
        ))

        val result = historicalRateDao.getHistoricalRates("EUR", "USD").first()

        assertEquals("2025-07-10", result[0].date)
        assertEquals("2025-07-11", result[1].date)
        assertEquals("2025-07-12", result[2].date)
    }

    @Test
    fun deleteRates_removesMatchingPair() = runBlocking {
        historicalRateDao.insertRates(listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2025-07-10"),
            HistoricalRateEntity("GBP", "JPY", 190.5, "2025-07-10")
        ))

        historicalRateDao.deleteRates("EUR", "USD")

        val eurUsd = historicalRateDao.getHistoricalRates("EUR", "USD").first()
        val gbpJpy = historicalRateDao.getHistoricalRates("GBP", "JPY").first()

        assertEquals(0, eurUsd.size)
        assertEquals(1, gbpJpy.size)
    }

    @Test
    fun getCount_returnsCorrectCount() = runBlocking {
        historicalRateDao.insertRates(listOf(
            HistoricalRateEntity("EUR", "USD", 1.10, "2025-07-10"),
            HistoricalRateEntity("EUR", "USD", 1.12, "2025-07-11"),
            HistoricalRateEntity("EUR", "USD", 1.11, "2025-07-12")
        ))

        val count = historicalRateDao.getCount("EUR", "USD")

        assertEquals(3, count)
    }
}
