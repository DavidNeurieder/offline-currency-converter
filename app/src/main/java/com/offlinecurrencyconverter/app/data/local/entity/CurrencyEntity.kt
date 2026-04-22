package com.offlinecurrencyconverter.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey
    val code: String,
    val name: String,
    val symbol: String,
    val isoNumeric: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isSelectedForOffline: Boolean = false,
    val flagUrl: String? = null
)
