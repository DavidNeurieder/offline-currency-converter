package com.offlinecurrencyconverter.app.di

import com.offlinecurrencyconverter.app.data.repository.CurrencyRepositoryImpl
import com.offlinecurrencyconverter.app.data.repository.ExchangeRateRepositoryImpl
import com.offlinecurrencyconverter.app.data.repository.RecentConversionRepositoryImpl
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import com.offlinecurrencyconverter.app.domain.repository.ExchangeRateRepository
import com.offlinecurrencyconverter.app.domain.repository.RecentConversionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(
        impl: CurrencyRepositoryImpl
    ): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindExchangeRateRepository(
        impl: ExchangeRateRepositoryImpl
    ): ExchangeRateRepository

    @Binds
    @Singleton
    abstract fun bindRecentConversionRepository(
        impl: RecentConversionRepositoryImpl
    ): RecentConversionRepository
}
