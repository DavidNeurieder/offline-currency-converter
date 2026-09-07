package com.offlinecurrencyconverter.app.domain.model

import java.time.Duration

data class CachePolicy(
    val latestRateMaxAge: Duration,
    val historicalMaxAge: Duration
) {
    companion object {
        val DEFAULT = CachePolicy(
            latestRateMaxAge = Duration.ofHours(6),
            historicalMaxAge = Duration.ofHours(24)
        )
    }
}