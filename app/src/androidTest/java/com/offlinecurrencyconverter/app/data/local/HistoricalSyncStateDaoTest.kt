package com.offlinecurrencyconverter.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.offlinecurrencyconverter.app.data.local.dao.HistoricalSyncStateDao
import com.offlinecurrencyconverter.app.data.local.entity.HistoricalSyncStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoricalSyncStateDaoTest {

    private lateinit var database: CurrencyDatabase
    private lateinit var dao: HistoricalSyncStateDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CurrencyDatabase::class.java
        ).build()
        dao = database.historicalSyncStateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun state(
        target: String,
        start: String?,
        end: String?,
        sync: Long = 0L
    ) = HistoricalSyncStateEntity("EUR", target, start, end, sync)

    @Test
    fun get_returnsNullWhenAbsent() = runBlocking {
        assertNull(dao.get("EUR", "USD"))
    }

    @Test
    fun upsert_insertsAndRetrieves() = runBlocking {
        dao.upsert(state("USD", "2024-01-01", "2024-03-31", 42L))

        val result = dao.get("EUR", "USD")

        assertEquals("USD", result?.targetCurrency)
        assertEquals("2024-01-01", result?.coveredStartDate)
        assertEquals("2024-03-31", result?.coveredEndDate)
        assertEquals(42L, result?.lastSuccessfulSync)
    }

    @Test
    fun upsert_updatesExistingRowWithoutDuplicating() = runBlocking {
        dao.upsert(state("USD", "2024-01-01", "2024-03-31", 1L))
        dao.upsert(state("USD", "2023-01-01", "2024-06-30", 2L))

        val result = dao.get("EUR", "USD")

        assertEquals("2023-01-01", result?.coveredStartDate)
        assertEquals("2024-06-30", result?.coveredEndDate)
        assertEquals(2L, result?.lastSuccessfulSync)
    }

    @Test
    fun getCoverageAggregate_returnsMinStartAndMaxEnd() = runBlocking {
        dao.upsert(state("USD", "2023-01-01", "2024-06-30"))
        dao.upsert(state("GBP", "2024-01-01", "2024-12-31"))

        val aggregate = dao.getCoverageAggregate("EUR")

        assertEquals("2023-01-01", aggregate?.minStart)
        assertEquals("2024-12-31", aggregate?.maxEnd)
    }

    @Test
    fun getCoverageAggregate_returnsNullFieldsWhenEmpty() = runBlocking {
        val aggregate = dao.getCoverageAggregate("EUR")

        // A SQL aggregate returns one row even with no data; its fields are null.
        assertNull(aggregate?.minStart)
        assertNull(aggregate?.maxEnd)
    }

    @Test
    fun deleteAll_clearsCoverage() = runBlocking {
        dao.upsert(state("USD", "2024-01-01", "2024-03-31"))

        dao.deleteAll()

        assertNull(dao.get("EUR", "USD"))
        val aggregate = dao.getCoverageAggregate("EUR")
        assertNull(aggregate?.minStart)
        assertNull(aggregate?.maxEnd)
    }
}
