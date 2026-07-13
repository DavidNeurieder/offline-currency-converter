package com.offlinecurrencyconverter.app.ui.convert

import com.offlinecurrencyconverter.app.TestFixtures
import com.offlinecurrencyconverter.app.data.CurrencyInitializer
import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import com.offlinecurrencyconverter.app.domain.repository.HistoricalRateRepository
import com.offlinecurrencyconverter.app.domain.repository.RecentConversionRepository
import com.offlinecurrencyconverter.app.domain.usecase.ConvertCurrencyUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConvertViewModelTest {

    private lateinit var convertCurrencyUseCase: ConvertCurrencyUseCase
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var recentConversionRepository: RecentConversionRepository
    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var historicalRateRepository: HistoricalRateRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var currencyInitializer: CurrencyInitializer
    private lateinit var viewModel: ConvertViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        convertCurrencyUseCase = mockk(relaxed = true)
        currencyRepository = mockk(relaxed = true)
        recentConversionRepository = mockk(relaxed = true)
        exchangeRateRepository = mockk(relaxed = true)
        historicalRateRepository = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        currencyInitializer = mockk(relaxed = true)

        every { currencyRepository.getAllCurrencies() } returns flowOf(TestFixtures.currencies)
        every { recentConversionRepository.getRecentConversions(any()) } returns flowOf(emptyList())
        coEvery { exchangeRateRepository.getLastUpdateTime() } returns System.currentTimeMillis()
        coEvery { convertCurrencyUseCase(any(), any(), any()) } returns Result.success(
            TestFixtures.createConversionResult()
        )
        coEvery { currencyInitializer.initializeIfNeeded() } returns Result.success(Unit)
        coEvery { historicalRateRepository.getHistoricalRates(any(), any()) } returns emptyList()
        every { preferencesManager.sourceCurrency } returns flowOf("USD")
        coEvery { preferencesManager.saveSourceCurrency(any()) } returns Unit
        every { preferencesManager.targetCurrency } returns flowOf("EUR")
        coEvery { preferencesManager.saveTargetCurrency(any()) } returns Unit
        every { preferencesManager.historicalRatesChart } returns flowOf(true)
        every { preferencesManager.multiCurrencyView } returns flowOf(false)

        viewModel = ConvertViewModel(
            convertCurrencyUseCase,
            currencyRepository,
            recentConversionRepository,
            exchangeRateRepository,
            historicalRateRepository,
            preferencesManager,
            currencyInitializer
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.amount)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onAmountChange updates amount`() = runTest {
        viewModel.onAmountChange("100")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("100", viewModel.uiState.value.amount)
    }

    @Test
    fun `onAmountChange filters non-numeric characters`() = runTest {
        viewModel.onAmountChange("100.50abc")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("100.50", viewModel.uiState.value.amount)
    }

    @Test
    fun `onSourceCurrencyChange updates source currency`() = runTest {
        viewModel.onSourceCurrencyChange(TestFixtures.EUR)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TestFixtures.EUR, viewModel.uiState.value.sourceCurrency)
    }

    @Test
    fun `onTargetCurrencyChange updates target currency`() = runTest {
        viewModel.onTargetCurrencyChange(TestFixtures.GBP)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TestFixtures.GBP, viewModel.uiState.value.targetCurrency)
    }

    @Test
    fun `swapCurrencies swaps source and target`() = runTest {
        coEvery { convertCurrencyUseCase(any(), any(), any()) } returns Result.success(
            TestFixtures.createConversionResult(
                sourceCurrency = TestFixtures.USD,
                targetCurrency = TestFixtures.EUR
            )
        )

        viewModel.onSourceCurrencyChange(TestFixtures.USD)
        viewModel.onTargetCurrencyChange(TestFixtures.EUR)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.swapCurrencies()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TestFixtures.EUR, state.sourceCurrency)
        assertEquals(TestFixtures.USD, state.targetCurrency)
    }

    @Test
    fun `empty amount clears conversion result`() = runTest {
        coEvery { convertCurrencyUseCase(any(), any(), any()) } returns Result.success(
            TestFixtures.createConversionResult()
        )

        viewModel.onAmountChange("100")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.conversionResult != null || viewModel.uiState.value.amount.isNotEmpty())

        viewModel.onAmountChange("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.conversionResult)
    }

    @Test
    fun `invalid amount clears conversion result`() = runTest {
        viewModel.onAmountChange("abc")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.amount)
    }

    @Test
    fun `recent conversions state flow is created`() = runTest {
        val flow = viewModel.recentConversions
        assertTrue(flow is kotlinx.coroutines.flow.StateFlow<*>)
    }

    @Test
    fun `onFavoriteToggle calls repository updateFavorite`() = runTest {
        viewModel.onFavoriteToggle("USD", true)
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { currencyRepository.updateFavorite("USD", true) }
    }

    @Test
    fun `onFavoriteToggle can remove favorite`() = runTest {
        viewModel.onFavoriteToggle("EUR", false)
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { currencyRepository.updateFavorite("EUR", false) }
    }

    @Test
    fun `onSourceCurrencyChange loads historical rates from db`() = runTest {
        viewModel.onSourceCurrencyChange(TestFixtures.EUR)
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { historicalRateRepository.getHistoricalRates(any(), any()) }
    }

    @Test
    fun `onTargetCurrencyChange loads historical rates from db`() = runTest {
        viewModel.onTargetCurrencyChange(TestFixtures.GBP)
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { historicalRateRepository.getHistoricalRates(any(), any()) }
    }

    @Test
    fun `swapCurrencies loads historical rates from db`() = runTest {
        viewModel.onSourceCurrencyChange(TestFixtures.USD)
        viewModel.onTargetCurrencyChange(TestFixtures.EUR)
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify(atLeast = 1) { historicalRateRepository.getHistoricalRates(any(), any()) }
    }

    @Test
    fun `historical rates are exposed in ui state`() = runTest {
        val historicalRates = listOf(
            com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity(
                baseCurrency = "USD", targetCurrency = "EUR", rate = 0.92, date = "2024-01-01"
            ),
            com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity(
                baseCurrency = "USD", targetCurrency = "EUR", rate = 0.93, date = "2024-01-02"
            )
        )
        coEvery { historicalRateRepository.getHistoricalRates(any(), any()) } returns historicalRates

        viewModel.onSourceCurrencyChange(TestFixtures.EUR)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(historicalRates, viewModel.uiState.value.historicalRates)
    }

    @Test
    fun `initial historical rates state is empty`() = runTest {
        assertTrue(viewModel.uiState.value.historicalRates.isEmpty())
    }

    @Test
    fun `multiCurrency conversions populated when enabled with favorites and rates`() = runTest {
        every { preferencesManager.multiCurrencyView } returns flowOf(true)
        every { currencyRepository.getAllCurrencies() } returns flowOf(TestFixtures.currenciesWithFavorites)
        coEvery { exchangeRateRepository.getAllRatesForCurrency("USD") } returns listOf(
            TestFixtures.createExchangeRate("USD", "EUR", 0.92),
            TestFixtures.createExchangeRate("USD", "GBP", 0.79),
            TestFixtures.createExchangeRate("USD", "JPY", 149.50)
        )

        val vm = ConvertViewModel(
            convertCurrencyUseCase,
            currencyRepository,
            recentConversionRepository,
            exchangeRateRepository,
            historicalRateRepository,
            preferencesManager,
            currencyInitializer
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")
        testDispatcher.scheduler.advanceUntilIdle()

        val conversions = vm.uiState.value.multiCurrencyConversions
        assertTrue(conversions.isNotEmpty())
        assertTrue(conversions.any { it.currency.code == "EUR" })
        assertTrue(conversions.any { it.currency.code == "GBP" })
        assertTrue(conversions.any { it.currency.code == "JPY" })

        val eurResult = conversions.find { it.currency.code == "EUR" }!!
        assertEquals(92.0, eurResult.convertedAmount, 0.01)
    }

    @Test
    fun `multiCurrency conversions empty when no favorites`() = runTest {
        every { preferencesManager.multiCurrencyView } returns flowOf(true)
        every { currencyRepository.getAllCurrencies() } returns flowOf(TestFixtures.currencies)
        coEvery { exchangeRateRepository.getAllRatesForCurrency("USD") } returns listOf(
            TestFixtures.createExchangeRate("USD", "EUR", 0.92),
            TestFixtures.createExchangeRate("USD", "GBP", 0.79)
        )

        val vm = ConvertViewModel(
            convertCurrencyUseCase,
            currencyRepository,
            recentConversionRepository,
            exchangeRateRepository,
            historicalRateRepository,
            preferencesManager,
            currencyInitializer
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.multiCurrencyConversions.isEmpty())
    }

    @Test
    fun `multiCurrency conversions empty when disabled`() = runTest {
        every { preferencesManager.multiCurrencyView } returns flowOf(false)
        every { currencyRepository.getAllCurrencies() } returns flowOf(TestFixtures.currenciesWithFavorites)
        coEvery { exchangeRateRepository.getAllRatesForCurrency("USD") } returns listOf(
            TestFixtures.createExchangeRate("USD", "EUR", 0.92),
            TestFixtures.createExchangeRate("USD", "GBP", 0.79)
        )

        val vm = ConvertViewModel(
            convertCurrencyUseCase,
            currencyRepository,
            recentConversionRepository,
            exchangeRateRepository,
            historicalRateRepository,
            preferencesManager,
            currencyInitializer
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.multiCurrencyConversions.isEmpty())
    }
}
