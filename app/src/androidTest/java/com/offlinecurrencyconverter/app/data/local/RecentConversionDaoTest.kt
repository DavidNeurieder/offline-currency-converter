package com.offlinecurrencyconverter.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.offlinecurrencyconverter.app.data.local.dao.RecentConversionDao
import com.offlinecurrencyconverter.app.data.local.entity.RecentConversionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentConversionDaoTest {

    private lateinit var database: CurrencyDatabase
    private lateinit var recentConversionDao: RecentConversionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CurrencyDatabase::class.java
        ).build()
        recentConversionDao = database.recentConversionDao()
    }

    @Test
    fun insertConversion_savesWithTimestamp() = runBlocking {
        val conversion = RecentConversionEntity(
            sourceAmount = 100.0,
            sourceCurrencyCode = "USD",
            targetAmount = 92.0,
            targetCurrencyCode = "EUR",
            rate = 0.92
        )

        recentConversionDao.insertConversion(conversion)

        val result = recentConversionDao.getRecentConversions(10).first()
        assertEquals(1, result.size)
        assertTrue(result[0].timestamp > 0)
    }

    @Test
    fun getRecentConversions_returnsLimitedResults() = runBlocking {
        val now = System.currentTimeMillis()
        for (i in 1..15) {
            recentConversionDao.insertConversion(
                RecentConversionEntity(
                    sourceAmount = i.toDouble(),
                    sourceCurrencyCode = "USD",
                    targetAmount = i * 0.92,
                    targetCurrencyCode = "EUR",
                    rate = 0.92,
                    timestamp = now - i * 1000
                )
            )
        }

        val result = recentConversionDao.getRecentConversions(10).first()

        assertEquals(10, result.size)
    }

    @Test
    fun getRecentConversions_returnsOrderedByTimestamp() = runBlocking {
        val now = System.currentTimeMillis()
        recentConversionDao.insertConversion(
            RecentConversionEntity(
                sourceAmount = 100.0,
                sourceCurrencyCode = "USD",
                targetAmount = 92.0,
                targetCurrencyCode = "EUR",
                rate = 0.92,
                timestamp = now - 1000
            )
        )
        recentConversionDao.insertConversion(
            RecentConversionEntity(
                sourceAmount = 200.0,
                sourceCurrencyCode = "USD",
                targetAmount = 184.0,
                targetCurrencyCode = "EUR",
                rate = 0.92,
                timestamp = now
            )
        )

        val result = recentConversionDao.getRecentConversions(10).first()

        assertEquals(200.0, result[0].sourceAmount, 0.001)
        assertEquals(100.0, result[1].sourceAmount, 0.001)
    }

    @Test
    fun trimOldConversions_removesOldEntries() = runBlocking {
        val now = System.currentTimeMillis()
        for (i in 1..15) {
            recentConversionDao.insertConversion(
                RecentConversionEntity(
                    sourceAmount = i.toDouble(),
                    sourceCurrencyCode = "USD",
                    targetAmount = i * 0.92,
                    targetCurrencyCode = "EUR",
                    rate = 0.92,
                    timestamp = now - i * 1000
                )
            )
        }

        recentConversionDao.trimOldConversions(5)

        val result = recentConversionDao.getRecentConversions(10).first()
        assertEquals(5, result.size)
    }

    @Test
    fun deleteAll_removesAllConversions() = runBlocking {
        recentConversionDao.insertConversion(
            RecentConversionEntity(
                sourceAmount = 100.0,
                sourceCurrencyCode = "USD",
                targetAmount = 92.0,
                targetCurrencyCode = "EUR",
                rate = 0.92
            )
        )
        recentConversionDao.insertConversion(
            RecentConversionEntity(
                sourceAmount = 200.0,
                sourceCurrencyCode = "USD",
                targetAmount = 184.0,
                targetCurrencyCode = "EUR",
                rate = 0.92
            )
        )

        recentConversionDao.deleteAll()

        val result = recentConversionDao.getRecentConversions(10).first()
        assertTrue(result.isEmpty())
    }
}
