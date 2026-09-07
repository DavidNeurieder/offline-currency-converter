package com.offlinecurrencyconverter.app.domain.usecase

import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend operator fun invoke(
        amount: Double,
        sourceCurrency: Currency,
        targetCurrency: Currency
    ): Result<ConversionResult> {
        if (!amount.isFinite() || amount < 0.0) {
            return Result.failure(
                IllegalArgumentException("Amount must be a finite, non-negative number")
            )
        }

        if (sourceCurrency.code == targetCurrency.code) {
            return Result.success(
                ConversionResult(
                    sourceAmount = amount,
                    sourceCurrency = sourceCurrency,
                    targetAmount = amount,
                    targetCurrency = targetCurrency,
                    rate = 1.0
                )
            )
        }

        val exchangeRate = exchangeRateRepository.getRate(
            baseCurrency = sourceCurrency.code,
            targetCurrency = targetCurrency.code
        )

        return exchangeRate?.let {
            if (!it.rate.isFinite() || it.rate <= 0.0) {
                return Result.failure(
                    IllegalArgumentException("Exchange rate is invalid")
                )
            }

            val convertedAmount = amount * it.rate

            if (!convertedAmount.isFinite()) {
                return Result.failure(
                    IllegalArgumentException("Conversion result is out of range")
                )
            }

            Result.success(
                ConversionResult(
                    sourceAmount = amount,
                    sourceCurrency = sourceCurrency,
                    targetAmount = convertedAmount,
                    targetCurrency = targetCurrency,
                    rate = it.rate
                )
            )
        } ?: Result.failure(Exception("Exchange rate not available for ${sourceCurrency.code} to ${targetCurrency.code}. Please sync rates in Settings."))
    }
}
