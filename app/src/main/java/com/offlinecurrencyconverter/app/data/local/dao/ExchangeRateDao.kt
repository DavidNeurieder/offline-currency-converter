package com.offlinecurrencyconverter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offlinecurrencyconverter.app.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE baseCurrency = :baseCurrency")
    fun getRatesForCurrency(baseCurrency: String): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE baseCurrency = :baseCurrency AND targetCurrency = :targetCurrency")
    suspend fun getRate(baseCurrency: String, targetCurrency: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE isOfflineAvailable = 1")
    fun getOfflineAvailableRates(): Flow<List<ExchangeRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates WHERE isOfflineAvailable = 0")
    suspend fun deleteNonOfflineRates()

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()

    @Query("SELECT MAX(lastUpdated) FROM exchange_rates")
    suspend fun getLastUpdateTime(): Long?
}
