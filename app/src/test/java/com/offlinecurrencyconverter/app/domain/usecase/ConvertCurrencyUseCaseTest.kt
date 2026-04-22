package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.TestFixtures
import com.offlinecurrencyconverter.app.domain.model.ExchangeRate
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ConvertCurrencyUseCaseTest {

    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var convertCurrencyUseCase: ConvertCurrencyUseCase

    @Before
    fun setup() {
        exchangeRateRepository = mockk()
        convertCurrencyUseCase = ConvertCurrencyUseCase(exchangeRateRepository)
    }

    @Test
    fun `invoke with valid conversion returns correct result`() = runTest {
        val amount = 100.0
        val rate = 0.92
        coEvery {
            exchangeRateRepository.getRate("USD", "EUR")
        } returns ExchangeRate("USD", "EUR", rate, System.currentTimeMillis())

        val result = convertCurrencyUseCase(amount, TestFixtures.USD, TestFixtures.EUR)

        assertTrue(result.isSuccess)
        val conversion = result.getOrThrow()
        assertEquals(amount, conversion.sourceAmount, 0.0)
        assertEquals(TestFixtures.USD, conversion.sourceCurrency)
        assertEquals(TestFixtures.EUR, conversion.targetCurrency)
        assertEquals(rate, conversion.rate, 0.0)
        assertEquals(amount * rate, conversion.targetAmount, 0.01)
    }

    @Test
    fun `invoke with same currency returns 1-to-1 rate`() = runTest {
        val amount = 50.0

        val result = convertCurrencyUseCase(amount, TestFixtures.USD, TestFixtures.USD)

        assertTrue(result.isSuccess)
        val conversion = result.getOrThrow()
        assertEquals(amount, conversion.targetAmount, 0.0)
        assertEquals(1.0, conversion.rate, 0.0)
    }

    @Test
    fun `invoke with zero amount returns zero`() = runTest {
        coEvery {
            exchangeRateRepository.getRate("USD", "EUR")
        } returns ExchangeRate("USD", "EUR", 0.92, System.currentTimeMillis())

        val result = convertCurrencyUseCase(0.0, TestFixtures.USD, TestFixtures.EUR)

        assertTrue(result.isSuccess)
        assertEquals(0.0, result.getOrThrow().targetAmount, 0.0)
    }

    @Test
    fun `invoke with negative amount returns failure`() = runTest {
        val result = convertCurrencyUseCase(-100.0, TestFixtures.USD, TestFixtures.EUR)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Amount cannot be negative", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke with missing rate returns failure`() = runTest {
        coEvery {
            exchangeRateRepository.getRate("USD", "EUR")
        } returns null

        val result = convertCurrencyUseCase(100.0, TestFixtures.USD, TestFixtures.EUR)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not available") == true)
    }

    @Test
    fun `invoke with large amount handles precision correctly`() = runTest {
        val amount = 1_000_000.0
        val rate = 0.923456
        coEvery {
            exchangeRateRepository.getRate("USD", "EUR")
        } returns ExchangeRate("USD", "EUR", rate, System.currentTimeMillis())

        val result = convertCurrencyUseCase(amount, TestFixtures.USD, TestFixtures.EUR)

        assertTrue(result.isSuccess)
        assertEquals(amount * rate, result.getOrThrow().targetAmount, 0.01)
    }

    @Test
    fun `invoke with small rate handles precision correctly`() = runTest {
        val amount = 100.0
        val rate = 0.0001
        coEvery {
            exchangeRateRepository.getRate("USD", "JPY")
        } returns ExchangeRate("USD", "JPY", rate, System.currentTimeMillis())

        val result = convertCurrencyUseCase(amount, TestFixtures.USD, TestFixtures.JPY)

        assertTrue(result.isSuccess)
        assertEquals(amount * rate, result.getOrThrow().targetAmount, 0.0001)
    }
}
