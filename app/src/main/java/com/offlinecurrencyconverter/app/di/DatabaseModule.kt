package com.offlinecurrencyconverter.app.di

import android.content.Context
import androidx.room.Room
import com.offlinecurrencyconverter.app.data.local.CurrencyDatabase
import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.dao.ExchangeRateDao
import com.offlinecurrencyconverter.app.data.local.dao.RecentConversionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CurrencyDatabase {
        return Room.databaseBuilder(
            context,
            CurrencyDatabase::class.java,
            "offline_currency_converter_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCurrencyDao(database: CurrencyDatabase): CurrencyDao {
        return database.currencyDao()
    }

    @Provides
    fun provideExchangeRateDao(database: CurrencyDatabase): ExchangeRateDao {
        return database.exchangeRateDao()
    }

    @Provides
    fun provideRecentConversionDao(database: CurrencyDatabase): RecentConversionDao {
        return database.recentConversionDao()
    }
}
