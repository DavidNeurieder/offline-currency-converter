package com.offlinecurrencyconverter.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_conversions")
data class RecentConversionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceAmount: Double,
    val sourceCurrencyCode: String,
    val targetAmount: Double,
    val targetCurrencyCode: String,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis()
)
