package com.offlinecurrencyconverter.app.domain.model

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val isSelectedForOffline: Boolean = false
) {
    override fun equals(other: Any?): Boolean = other is Currency && code == other.code
    override fun hashCode(): Int = code.hashCode()
}
