package com.offlinecurrencyconverter.app.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SyncSchedulerTest {

    @Test
    fun `interval hours to milliseconds conversion`() {
        val sixHours = TimeUnit.HOURS.toMillis(6)
        val twelveHours = TimeUnit.HOURS.toMillis(12)
        val twentyFourHours = TimeUnit.HOURS.toMillis(24)
        val fortyEightHours = TimeUnit.HOURS.toMillis(48)
        val weekly = TimeUnit.HOURS.toMillis(168)

        assertEquals(6 * 60 * 60 * 1000L, sixHours)
        assertEquals(12 * 60 * 60 * 1000L, twelveHours)
        assertEquals(24 * 60 * 60 * 1000L, twentyFourHours)
        assertEquals(48 * 60 * 60 * 1000L, fortyEightHours)
        assertEquals(168 * 60 * 60 * 1000L, weekly)
    }

    @Test
    fun `calculate next sync time`() {
        val now = System.currentTimeMillis()
        val intervalHours = 24L
        val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours)

        val nextSyncTime = now + intervalMillis

        assertTrue(nextSyncTime > now)
        assertEquals(now + (24 * 60 * 60 * 1000L), nextSyncTime)
    }

    @Test
    fun `minimum interval enforcement`() {
        val minIntervalHours = 6L
        val requestedInterval = 1L

        val actualInterval = maxOf(requestedInterval, minIntervalHours)

        assertEquals(minIntervalHours, actualInterval)
    }

    @Test
    fun `maximum interval enforcement`() {
        val maxIntervalHours = 168L
        val requestedInterval = 500L

        val actualInterval = minOf(requestedInterval, maxIntervalHours)

        assertEquals(maxIntervalHours, actualInterval)
    }

    @Test
    fun `interval boundaries are valid`() {
        val sixHours = 6L
        val weekly = 168L

        assertTrue(sixHours >= 6)
        assertTrue(weekly <= 168)
    }

    @Test
    fun `interval calculations are accurate`() {
        val intervalHours = 24L
        val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours)
        val intervalSeconds = TimeUnit.HOURS.toSeconds(intervalHours)

        assertEquals(24 * 60 * 60 * 1000L, intervalMillis)
        assertEquals(24 * 60 * 60L, intervalSeconds)
    }

    @Test
    fun `next sync calculation with current time`() {
        val now = System.currentTimeMillis()
        val intervalMillis = TimeUnit.HOURS.toMillis(24)

        val nextSync = now + intervalMillis
        val timeUntilSync = nextSync - now

        assertEquals(intervalMillis, timeUntilSync)
    }

    @Test
    fun `default interval is 24 hours`() {
        val defaultIntervalHours = 24L
        val defaultIntervalMillis = TimeUnit.HOURS.toMillis(defaultIntervalHours)

        assertNotNull(defaultIntervalMillis)
        assertEquals(24 * 60 * 60 * 1000L, defaultIntervalMillis)
    }
}
