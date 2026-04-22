package com.offlinecurrencyconverter.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.entity.CurrencyEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrencyDaoTest {

    private lateinit var database: CurrencyDatabase
    private lateinit var currencyDao: CurrencyDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CurrencyDatabase::class.java
        ).build()
        currencyDao = database.currencyDao()
    }

    @Test
    fun insertCurrency_savesAndRetrieves() = runBlocking {
        val currency = CurrencyEntity(
            code = "USD",
            name = "US Dollar",
            symbol = "$"
        )

        currencyDao.insertCurrency(currency)
        val result = currencyDao.getCurrencyByCode("USD")

        assertNotNull(result)
        assertEquals("USD", result?.code)
        assertEquals("US Dollar", result?.name)
        assertEquals("$", result?.symbol)
    }

    @Test
    fun insertCurrencies_batchInsertWorks() = runBlocking {
        val currencies = listOf(
            CurrencyEntity("USD", "US Dollar", "$"),
            CurrencyEntity("EUR", "Euro", "€"),
            CurrencyEntity("GBP", "British Pound", "£")
        )

        currencyDao.insertCurrencies(currencies)

        val result = currencyDao.getAllCurrencies().first()
        assertEquals(3, result.size)
    }

    @Test
    fun getAllCurrencies_returnsAll() = runBlocking {
        currencyDao.insertCurrencies(listOf(
            CurrencyEntity("USD", "US Dollar", "$"),
            CurrencyEntity("EUR", "Euro", "€")
        ))

        val result = currencyDao.getAllCurrencies().first()

        assertEquals(2, result.size)
    }

    @Test
    fun getSelectedCurrencies_returnsFiltered() = runBlocking {
        currencyDao.insertCurrencies(listOf(
            CurrencyEntity("USD", "US Dollar", "$", isSelectedForOffline = true),
            CurrencyEntity("EUR", "Euro", "€", isSelectedForOffline = false),
            CurrencyEntity("GBP", "British Pound", "£", isSelectedForOffline = true)
        ))

        val result = currencyDao.getSelectedCurrencies().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isSelectedForOffline })
    }

    @Test
    fun getCurrencyByCode_returnsCorrect() = runBlocking {
        currencyDao.insertCurrency(CurrencyEntity("EUR", "Euro", "€"))

        val result = currencyDao.getCurrencyByCode("EUR")

        assertNotNull(result)
        assertEquals("EUR", result?.code)
    }

    @Test
    fun getCurrencyByCode_returnsNullForNonExistent() = runBlocking {
        val result = currencyDao.getCurrencyByCode("XYZ")

        assertNull(result)
    }

    @Test
    fun updateSelection_changesFlag() = runBlocking {
        currencyDao.insertCurrency(CurrencyEntity("USD", "US Dollar", "$", isSelectedForOffline = false))

        currencyDao.updateSelection("USD", true)

        val result = currencyDao.getCurrencyByCode("USD")
        assertTrue(result?.isSelectedForOffline == true)
    }

    @Test
    fun deleteAll_removesAllCurrencies() = runBlocking {
        currencyDao.insertCurrencies(listOf(
            CurrencyEntity("USD", "US Dollar", "$"),
            CurrencyEntity("EUR", "Euro", "€")
        ))

        currencyDao.deleteAll()

        val result = currencyDao.getAllCurrencies().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getCount_returnsCorrectCount() = runBlocking {
        currencyDao.insertCurrencies(listOf(
            CurrencyEntity("USD", "US Dollar", "$"),
            CurrencyEntity("EUR", "Euro", "€")
        ))

        val count = currencyDao.getCount()

        assertEquals(2, count)
    }
}
