package com.offlinecurrencyconverter.app.di

import com.offlinecurrencyconverter.app.domain.validation.ExchangeRateResponseValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ValidationModule {

    @Provides
    @Singleton
    @Named("minHistoricalRecords")
    fun provideMinHistoricalRecords(): Int {
        return ExchangeRateResponseValidator.DEFAULT_MIN_HISTORICAL_RECORDS
    }
}