package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.entity.CurrencyEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.data.remote.dto.CurrencyItem
import com.offlinecurrencyconverter.app.domain.model.Currency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class CurrencyRepositoryTest {

    private lateinit var currencyDao: CurrencyDao
    private lateinit var frankfurterApi: FrankfurterApi
    private lateinit var currencyRepository: CurrencyRepositoryImpl

    @Before
    fun setup() {
        currencyDao = mockk(relaxed = true)
        frankfurterApi = mockk(relaxed = true)
        currencyRepository = CurrencyRepositoryImpl(currencyDao, frankfurterApi)
    }

    @Test
    fun `getAllCurrencies returns flow of currencies`() = runTest {
        val entities = listOf(
            CurrencyEntity("USD", "US Dollar", "$", isSelectedForOffline = true),
            CurrencyEntity("EUR", "Euro", "€", isSelectedForOffline = true)
        )
        coEvery { currencyDao.getAllCurrencies() } returns flowOf(entities)

        val result = currencyRepository.getAllCurrencies().first()

        assertEquals(2, result.size)
        assertEquals("USD", result[0].code)
        assertEquals("EUR", result[1].code)
    }

    @Test
    fun `getSelectedCurrencies returns only selected currencies`() = runTest {
        val entities = listOf(
            CurrencyEntity("USD", "US Dollar", "$", isSelectedForOffline = true),
            CurrencyEntity("EUR", "Euro", "€", isSelectedForOffline = false)
        )
        coEvery { currencyDao.getSelectedCurrencies() } returns flowOf(listOf(entities[0]))

        val result = currencyRepository.getSelectedCurrencies().first()

        assertEquals(1, result.size)
        assertEquals("USD", result[0].code)
        assertTrue(result[0].isSelectedForOffline)
    }

    @Test
    fun `getCurrencyByCode returns currency when exists`() = runTest {
        coEvery { currencyDao.getCurrencyByCode("USD") } returns
                CurrencyEntity("USD", "US Dollar", "$", isSelectedForOffline = true)

        val result = currencyRepository.getCurrencyByCode("USD")

        assertEquals("USD", result?.code)
        assertEquals("US Dollar", result?.name)
        assertEquals("$", result?.symbol)
    }

    @Test
    fun `getCurrencyByCode returns null when not exists`() = runTest {
        coEvery { currencyDao.getCurrencyByCode("XXX") } returns null

        val result = currencyRepository.getCurrencyByCode("XXX")

        assertNull(result)
    }

    @Test
    fun `updateCurrencySelection updates selection status`() = runTest {
        currencyRepository.updateCurrencySelection("USD", true)

        coVerify { currencyDao.updateSelection("USD", true) }
    }

    @Test
    fun `initializeCurrencies inserts all currencies`() = runTest {
        val currencies = listOf(
            Currency("USD", "US Dollar", "$", true),
            Currency("EUR", "Euro", "€", false)
        )

        currencyRepository.initializeCurrencies(currencies)

        coVerify { currencyDao.insertCurrencies(any()) }
    }

    @Test
    fun `getAllCurrencies maps entity to domain correctly`() = runTest {
        val entity = CurrencyEntity("USD", "US Dollar", "$", isSelectedForOffline = true)
        coEvery { currencyDao.getAllCurrencies() } returns flowOf(listOf(entity))

        val result = currencyRepository.getAllCurrencies().first()

        assertEquals(entity.code, result[0].code)
        assertEquals(entity.name, result[0].name)
        assertEquals(entity.symbol, result[0].symbol)
        assertEquals(entity.isSelectedForOffline, result[0].isSelectedForOffline)
    }

    @Test
    fun `fetchAndSaveCurrenciesFromApi saves currencies to database`() = runTest {
        val currencyItems = listOf(
            CurrencyItem("USD", "840", "US Dollar", "$", null, null),
            CurrencyItem("EUR", "978", "Euro", "€", null, null)
        )
        coEvery { frankfurterApi.getCurrencies() } returns Response.success(currencyItems)

        val result = currencyRepository.fetchAndSaveCurrenciesFromApi()

        assertTrue(result.isSuccess)
        coVerify { currencyDao.deleteAll() }
        coVerify { currencyDao.insertCurrencies(any()) }
    }

    @Test
    fun `fetchAndSaveCurrenciesFromApi handles failure`() = runTest {
        coEvery { frankfurterApi.getCurrencies() } returns Response.error(500, mockk(relaxed = true))

        val result = currencyRepository.fetchAndSaveCurrenciesFromApi()

        assertTrue(result.isFailure)
    }
}
