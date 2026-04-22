package com.offlinecurrencyconverter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinecurrencyconverter.app.data.local.entity.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY code ASC")
    fun getAllCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isSelectedForOffline = 1 ORDER BY code ASC")
    fun getSelectedCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE code = :code")
    suspend fun getCurrencyByCode(code: String): CurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(currency: CurrencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Update
    suspend fun updateCurrency(currency: CurrencyEntity)

    @Query("UPDATE currencies SET isSelectedForOffline = :selected WHERE code = :code")
    suspend fun updateSelection(code: String, selected: Boolean)

    @Query("DELETE FROM currencies")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM currencies")
    suspend fun getCount(): Int
}
