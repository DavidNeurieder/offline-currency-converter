package com.offlinecurrencyconverter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offlinecurrencyconverter.app.data.local.entity.RecentConversionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentConversionDao {
    @Query("SELECT * FROM recent_conversions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConversions(limit: Int = 10): Flow<List<RecentConversionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(conversion: RecentConversionEntity)

    @Query("DELETE FROM recent_conversions WHERE id NOT IN (SELECT id FROM recent_conversions ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimOldConversions(limit: Int = 10)

    @Query("DELETE FROM recent_conversions")
    suspend fun deleteAll()
}
