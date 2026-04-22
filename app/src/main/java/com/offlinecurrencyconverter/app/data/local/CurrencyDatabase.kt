package com.offlinecurrencyconverter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.dao.ExchangeRateDao
import com.offlinecurrencyconverter.app.data.local.dao.RecentConversionDao
import com.offlinecurrencyconverter.app.data.local.entity.CurrencyEntity
import com.offlinecurrencyconverter.app.data.local.entity.ExchangeRateEntity
import com.offlinecurrencyconverter.app.data.local.entity.RecentConversionEntity

@Database(
    entities = [
        CurrencyEntity::class,
        ExchangeRateEntity::class,
        RecentConversionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CurrencyDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun recentConversionDao(): RecentConversionDao
}
