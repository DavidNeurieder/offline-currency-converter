package com.offlinecurrencyconverter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.dao.ExchangeRateDao
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalRateDao
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalSyncStateDao
import com.offlinecurrencyconverter.app.data.local.dao.RecentConversionDao
import com.offlinecurrencyconverter.app.data.local.entity.CurrencyEntity
import com.offlinecurrencyconverter.app.data.local.entity.ExchangeRateEntity
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalSyncStateEntity
import com.offlinecurrencyconverter.app.data.local.entity.RecentConversionEntity

@Database(
    entities = [
        CurrencyEntity::class,
        ExchangeRateEntity::class,
        RecentConversionEntity::class,
        HistoricalRateEntity::class,
        HistoricalSyncStateEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class CurrencyDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun recentConversionDao(): RecentConversionDao
    abstract fun historicalRateDao(): HistoricalRateDao
    abstract fun historicalSyncStateDao(): HistoricalSyncStateDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE currencies ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS historical_rates (
                        baseCurrency TEXT NOT NULL,
                        targetCurrency TEXT NOT NULL,
                        rate REAL NOT NULL,
                        date TEXT NOT NULL,
                        PRIMARY KEY(baseCurrency, targetCurrency, date)
                    )
                """)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS historical_sync_state (
                        baseCurrency TEXT NOT NULL,
                        targetCurrency TEXT NOT NULL,
                        coveredStartDate TEXT,
                        coveredEndDate TEXT,
                        lastSuccessfulSync INTEGER NOT NULL,
                        PRIMARY KEY(baseCurrency, targetCurrency)
                    )
                """)
            }
        }
    }
}
