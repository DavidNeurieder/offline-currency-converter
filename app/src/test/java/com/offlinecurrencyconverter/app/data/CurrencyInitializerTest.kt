package com.offlinecurrencyconverter.app.data

import com.offlinecurrencyconverter.app.domain.model.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CurrencyInitializerTest {

    private lateinit var currencyRepository: MockCurrencyRepository
    private lateinit var preferencesManager: MockPreferencesManager
    private lateinit var initializer: TestableCurrencyInitializer

    @Before
    fun setup() {
        currencyRepository = MockCurrencyRepository()
        preferencesManager = MockPreferencesManager()
        initializer = TestableCurrencyInitializer(currencyRepository, preferencesManager)
    }

    @Test
    fun `initializeIfNeeded first time fetches currencies`() = runBlocking {
        preferencesManager.currenciesInitializedValue = false
        currencyRepository.hasCurrenciesValue = false
        currencyRepository.fetchResult = Result.success(Unit)

        val result = initializer.initializeIfNeeded()

        assertTrue(result.isSuccess)
        assertEquals(1, currencyRepository.fetchCallCount)
        assertTrue(preferencesManager.setCurrenciesInitializedCalled)
    }

    @Test
    fun `initializeIfNeeded already initialized returns success`() = runBlocking {
        preferencesManager.currenciesInitializedValue = true
        currencyRepository.hasCurrenciesValue = true

        val result = initializer.initializeIfNeeded()

        assertTrue(result.isSuccess)
        assertEquals(0, currencyRepository.fetchCallCount)
    }

    @Test
    fun `initializeIfNeeded no currencies in DB fetches currencies`() = runBlocking {
        preferencesManager.currenciesInitializedValue = true
        currencyRepository.hasCurrenciesValue = false
        currencyRepository.fetchResult = Result.success(Unit)

        val result = initializer.initializeIfNeeded()

        assertTrue(result.isSuccess)
        assertEquals(1, currencyRepository.fetchCallCount)
    }

    @Test
    fun `initializeIfNeeded fetch fails returns failure`() = runBlocking {
        preferencesManager.currenciesInitializedValue = false
        currencyRepository.hasCurrenciesValue = false
        currencyRepository.fetchResult = Result.failure(Exception("Network error"))

        val result = initializer.initializeIfNeeded()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    private class MockPreferencesManager {
        var currenciesInitializedValue: Boolean = false
        var setCurrenciesInitializedCalled = false

        val currenciesInitialized: Flow<Boolean> = flowOf(false)

        suspend fun setCurrenciesInitialized(initialized: Boolean) {
            setCurrenciesInitializedCalled = true
        }

        fun getInitializedValue(): Boolean = currenciesInitializedValue
    }

    private class MockCurrencyRepository {
        var hasCurrenciesValue: Boolean = false
        var fetchResult: Result<Unit> = Result.success(Unit)
        var fetchCallCount = 0

        fun getAllCurrencies(): Flow<List<Currency>> = flowOf(emptyList())

        fun getSelectedCurrencies(): Flow<List<Currency>> = flowOf(emptyList())

        suspend fun getCurrencyByCode(code: String): Currency? = null

        suspend fun updateCurrencySelection(code: String, selected: Boolean) {}

        suspend fun initializeCurrencies(currencies: List<Currency>) {}

        suspend fun clearAllCurrencies() {}

        suspend fun fetchAndSaveCurrenciesFromApi(): Result<Unit> {
            fetchCallCount++
            return fetchResult
        }

        suspend fun hasCurrencies(): Boolean = hasCurrenciesValue
    }

    private class TestableCurrencyInitializer(
        private val currencyRepository: MockCurrencyRepository,
        private val preferencesManager: MockPreferencesManager
    ) {
        suspend fun initializeIfNeeded(): Result<Unit> {
            val isInitialized = preferencesManager.getInitializedValue()
            val hasCurrencies = currencyRepository.hasCurrencies()

            if (!isInitialized || !hasCurrencies) {
                val result = currencyRepository.fetchAndSaveCurrenciesFromApi()
                if (result.isSuccess) {
                    preferencesManager.setCurrenciesInitialized(true)
                }
                return result
            }

            return Result.success(Unit)
        }
    }
}
