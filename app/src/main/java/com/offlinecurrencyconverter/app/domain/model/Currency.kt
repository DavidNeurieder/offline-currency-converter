package com.offlinecurrencyconverter.app.domain.model

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val isSelectedForOffline: Boolean = false,
    val isFavorite: Boolean = false
)
