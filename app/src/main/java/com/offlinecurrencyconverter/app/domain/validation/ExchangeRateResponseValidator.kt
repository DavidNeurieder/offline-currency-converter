package com.offlinecurrencyconverter.app.domain.validation

import com.offlinecurrencyconverter.app.data.remote.dto.ExchangeRateItem
import com.offlinecurrencyconverter.app.domain.model.SyncError
import com.offlinecurrencyconverter.app.domain.model.SyncErrorException
import com.offlinecurrencyconverter.app.domain.model.isValidExchangeRate
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Named

class ExchangeRateResponseValidator @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    @Named("minHistoricalRecords") private val minHistoricalRecords: Int
) {

    suspend fun validateLatest(
        rates: List<ExchangeRateItem>,
        requestedBase: String,
        requestedTargets: List<String>
    ): Result<Unit> {
        val supportedCurrencies = supportedCurrencies()
        if (supportedCurrencies.isEmpty()) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }
        if (requestedBase !in supportedCurrencies) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }
        if (rates.isEmpty()) {
            return Result.failure(SyncErrorException(SyncError.EmptyResponse))
        }

        val invalidItems = rates.filterNot { item ->
            item.base == requestedBase &&
                item.quote in supportedCurrencies &&
                item.rate.isValidExchangeRate()
        }
        if (invalidItems.isNotEmpty()) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }

        val duplicateKeys = rates.groupBy { it.base to it.quote }.any { it.value.size > 1 }
        if (duplicateKeys) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }

        if (requestedTargets.isNotEmpty()) {
            val returnedQuotes = rates.map { it.quote }.toSet()
            val requestedQuotes = requestedTargets.toSet()
            if (!returnedQuotes.containsAll(requestedQuotes)) {
                return Result.failure(SyncErrorException(SyncError.IncompleteResponse))
            }
        }

        return Result.success(Unit)
    }

    /**
     * Strict validation for a full historical window. Delegates to
     * [validateHistoricalRange] using the configured minimum record count and
     * treating an empty response as a failure.
     */
    suspend fun validateHistorical(
        rates: List<ExchangeRateItem>,
        requestedBase: String,
        startDate: String,
        endDate: String
    ): Result<Unit> = validateHistoricalRange(
        rates, requestedBase, startDate, endDate,
        minRecords = minHistoricalRecords,
        allowEmpty = false
    )

    /**
     * Validation for an arbitrary historical window, used by incremental
     * synchronization. Unlike [validateHistorical] the expected record count is
     * supplied by the caller (scaled to the window size, since a short window may
     * legitimately contain only a handful of observations) and an empty response
     * may be tolerated when [allowEmpty] is set — a weekend or holiday window can
     * contain no new publications and that is not an error.
     */
    suspend fun validateHistoricalRange(
        rates: List<ExchangeRateItem>,
        requestedBase: String,
        startDate: String,
        endDate: String,
        minRecords: Int,
        allowEmpty: Boolean
    ): Result<Unit> {
        val supportedCurrencies = supportedCurrencies()
        if (supportedCurrencies.isEmpty()) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }
        if (requestedBase !in supportedCurrencies) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }
        if (rates.isEmpty()) {
            if (allowEmpty) return Result.success(Unit)
            return Result.failure(SyncErrorException(SyncError.EmptyResponse))
        }

        val start: LocalDate
        val end: LocalDate
        try {
            start = LocalDate.parse(startDate)
            end = LocalDate.parse(endDate)
        } catch (e: DateTimeParseException) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }

        val validCount = rates.count { item ->
            item.base == requestedBase &&
                item.quote in supportedCurrencies &&
                item.rate.isValidExchangeRate() &&
                isWithinWindow(item.date, start, end)
        }

        if (validCount != rates.size) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }

        val duplicateKeys = rates
            .groupBy { Triple(it.base, it.quote, it.date) }
            .any { it.value.size > 1 }
        if (duplicateKeys) {
            return Result.failure(SyncErrorException(SyncError.InvalidResponse))
        }

        if (validCount < minRecords) {
            return Result.failure(SyncErrorException(SyncError.IncompleteResponse))
        }

        return Result.success(Unit)
    }

    private suspend fun supportedCurrencies(): Set<String> {
        return currencyRepository.getAllCurrencies().first().map { it.code }.toSet()
    }

    private fun isWithinWindow(date: String, start: LocalDate, end: LocalDate): Boolean {
        return try {
            val parsed = LocalDate.parse(date)
            !parsed.isBefore(start) && !parsed.isAfter(end)
        } catch (e: DateTimeParseException) {
            false
        }
    }

    companion object {
        const val DEFAULT_MIN_HISTORICAL_RECORDS = 30

        /**
         * Scales the configured minimum record count down for short windows.
         *
         * Central-bank reference rates are not published on weekends or holidays,
         * so a window of N calendar days yields roughly 4/5·N observations at most.
         * Applying the configured absolute minimum (30) to a short incremental
         * window would reject perfectly valid responses, so the expected count is
         * capped by the window size.
         */
        fun scaledMinRecords(windowDays: Long, configured: Int): Int {
            val expected = ((windowDays * 2 + 4) / 5).coerceAtLeast(1L)
            return minOf(configured, expected.toInt())
        }
    }
}