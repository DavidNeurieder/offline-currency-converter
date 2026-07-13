package com.offlinecurrencyconverter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricalRateDao {
    @Query("SELECT * FROM historical_rates WHERE baseCurrency = :baseCurrency AND targetCurrency = :targetCurrency ORDER BY date ASC")
    fun getHistoricalRates(baseCurrency: String, targetCurrency: String): Flow<List<HistoricalRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<HistoricalRateEntity>)

    @Query("DELETE FROM historical_rates WHERE baseCurrency = :baseCurrency AND targetCurrency = :targetCurrency")
    suspend fun deleteRates(baseCurrency: String, targetCurrency: String)

    @Query("DELETE FROM historical_rates")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM historical_rates WHERE baseCurrency = :baseCurrency AND targetCurrency = :targetCurrency")
    suspend fun getCount(baseCurrency: String, targetCurrency: String): Int
}
