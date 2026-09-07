package com.offlinecurrencyconverter.app.domain.validation

import com.offlinecurrencyconverter.app.data.remote.dto.ExchangeRateItem
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.model.SyncError
import com.offlinecurrencyconverter.app.domain.model.SyncErrorException
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExchangeRateResponseValidatorTest {

    private lateinit var currencyRepository: CurrencyRepository
    private val supportedCurrencies = listOf(
        Currency("EUR", "Euro", "€"),
        Currency("USD", "US Dollar", "$"),
        Currency("GBP", "British Pound", "£"),
        Currency("JPY", "Japanese Yen", "¥")
    )

    @Before
    fun setup() {
        currencyRepository = mockk()
        coEvery { currencyRepository.getAllCurrencies() } returns flowOf(supportedCurrencies)
    }

    private fun validator(minHistoricalRecords: Int = 30): ExchangeRateResponseValidator {
        return ExchangeRateResponseValidator(currencyRepository, minHistoricalRecords)
    }

    private fun latestItem(
        base: String = "EUR",
        quote: String = "USD",
        rate: Double = 1.09
    ): ExchangeRateItem = ExchangeRateItem("2024-01-15", base, quote, rate)

    private fun historicalItems(count: Int = 31): List<ExchangeRateItem> {
        val start = LocalDate.parse("2024-01-01")
        return (0 until count).map { index ->
            ExchangeRateItem(
                date = start.plusDays(index.toLong()).toString(),
                base = "EUR",
                quote = "USD",
                rate = 1.0 + index * 0.001
            )
        }
    }

    @Test
    fun `validateLatest accepts fully valid response`() = runTest {
        val rates = listOf(latestItem("EUR", "USD"), latestItem("EUR", "GBP"), latestItem("EUR", "JPY"))

        val result = validator().validateLatest(rates, "EUR", emptyList())

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateLatest accepts all requested targets`() = runTest {
        val rates = listOf(latestItem("EUR", "USD"), latestItem("EUR", "GBP"))

        val result = validator().validateLatest(rates, "EUR", listOf("USD", "GBP"))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateLatest rejects empty response`() = runTest {
        val result = validator().validateLatest(emptyList(), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.EmptyResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects empty supported currencies`() = runTest {
        coEvery { currencyRepository.getAllCurrencies() } returns flowOf(emptyList())

        val result = validator().validateLatest(listOf(latestItem()), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects NaN rate`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(rate = Double.NaN)), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects zero rate`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(rate = 0.0)), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects negative rate`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(rate = -1.0)), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects infinite rate`() = runTest {
        val result = validator().validateLatest(
            listOf(latestItem(rate = Double.POSITIVE_INFINITY)),
            "EUR",
            emptyList()
        )

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects unsupported quote code`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(quote = "FOO")), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects an unsupported code like EURO`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(quote = "EURO")), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects blank quote code`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(quote = "")), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects self pair`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(quote = "EUR")), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects wrong base currency`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(base = "USD")), "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects unsupported requested base`() = runTest {
        val result = validator().validateLatest(listOf(latestItem(base = "XXX")), "XXX", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects duplicate quote`() = runTest {
        val rates = listOf(latestItem("EUR", "USD"), latestItem("EUR", "USD"))

        val result = validator().validateLatest(rates, "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects extra unsupported currency amid valid ones`() = runTest {
        val rates = listOf(
            latestItem("EUR", "USD"),
            latestItem("EUR", "GBP"),
            latestItem("EUR", "FOO")
        )

        val result = validator().validateLatest(rates, "EUR", emptyList())

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateLatest rejects missing requested target`() = runTest {
        val rates = listOf(latestItem("EUR", "USD"))

        val result = validator().validateLatest(rates, "EUR", listOf("USD", "GBP"))

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.IncompleteResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical accepts fully valid response`() = runTest {
        val result = validator().validateHistorical(
            historicalItems(),
            "EUR",
            "2024-01-01",
            "2024-01-31"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateHistorical accepts exactly minimum records`() = runTest {
        val result = validator(minHistoricalRecords = 2).validateHistorical(
            historicalItems(count = 2),
            "EUR",
            "2024-01-01",
            "2024-01-31"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateHistorical rejects empty response`() = runTest {
        val result = validator().validateHistorical(emptyList(), "EUR", "2024-01-01", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.EmptyResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects malformed item date`() = runTest {
        val items = historicalItems().toMutableList()
        items[0] = ExchangeRateItem("not-a-date", "EUR", "USD", 1.1)

        val result = validator().validateHistorical(items, "EUR", "2024-01-01", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects blank item date`() = runTest {
        val items = historicalItems().toMutableList()
        items[0] = ExchangeRateItem("", "EUR", "USD", 1.1)

        val result = validator().validateHistorical(items, "EUR", "2024-01-01", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects date outside requested window`() = runTest {
        val items = historicalItems().toMutableList()
        items[0] = ExchangeRateItem("1999-01-01", "EUR", "USD", 1.1)

        val result = validator().validateHistorical(items, "EUR", "2024-01-01", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects invalid rate`() = runTest {
        val items = historicalItems().toMutableList()
        items[0] = ExchangeRateItem("2024-01-02", "EUR", "USD", Double.NaN)

        val result = validator().validateHistorical(items, "EUR", "2024-01-01", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects unsupported quote`() = runTest {
        val items = historicalItems().toMutableList()
        items[0] = ExchangeRateItem("2024-01-02", "EUR", "FOO", 1.1)

        val result = validator().validateHistorical(items, "EUR", "2024-01-01", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects malformed window dates`() = runTest {
        val result = validator().validateHistorical(historicalItems(), "EUR", "oops", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects response below minimum records`() = runTest {
        val result = validator().validateHistorical(
            historicalItems(count = 5),
            "EUR",
            "2024-01-01",
            "2024-01-31"
        )

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.IncompleteResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }

    @Test
    fun `validateHistorical rejects duplicate base quote date key`() = runTest {
        val items = historicalItems().toMutableList()
        items.add(ExchangeRateItem("2024-01-01", "EUR", "USD", 1.1))

        val result = validator().validateHistorical(items, "EUR", "2024-01-01", "2024-01-31")

        assertTrue(result.isFailure)
        assertEquals(
            SyncError.InvalidResponse,
            (result.exceptionOrNull() as SyncErrorException).syncError
        )
    }
}