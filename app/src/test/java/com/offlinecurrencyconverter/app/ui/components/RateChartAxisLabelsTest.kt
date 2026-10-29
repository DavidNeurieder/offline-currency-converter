package com.offlinecurrencyconverter.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale

class RateChartAxisLabelsTest {

    private val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun `five year range labels every year where it starts`() {
        val points = dailyRange("2021-10-21", "2026-10-20")

        val labels = buildAxisLabels(points, parser)

        assertEquals(
            (2022..2026).map { yearOf("$it-01-01") },
            labels.map { it.text }
        )
        // Every label sits on the first day of the year it names.
        labels.forEach { label -> assertTrue(points[label.index].first.endsWith("-01-01")) }
    }

    @Test
    fun `leading year is dropped when it would collide with the next one`() {
        // Only three months of 2021 precede the first year boundary.
        val points = dailyRange("2021-10-21", "2026-10-20")

        val labels = buildAxisLabels(points, parser)

        assertEquals(yearOf("2022-01-01"), labels.first().text)
        assertTrue(labels.none { it.text == yearOf("2021-01-01") })
    }

    @Test
    fun `leading year is kept when there is room before the next one`() {
        val points = dailyRange("2022-06-01", "2026-10-20")

        val labels = buildAxisLabels(points, parser)

        assertEquals(0, labels.first().index)
        assertEquals(yearOf("2022-06-01"), labels.first().text)
        assertEquals(
            (2023..2026).map { yearOf("$it-01-01") },
            labels.drop(1).map { it.text }
        )
    }

    @Test
    fun `one year range labels months with their year`() {
        val points = dailyRange("2025-10-19", "2026-10-19")

        val labels = buildAxisLabels(points, parser)

        assertEquals(5, labels.size)
        assertEquals(0, labels.first().index)
        assertEquals(points.size - 1, labels.last().index)
        assertEquals(labels.size, labels.map { it.text }.distinct().size)
        labels.forEach { label ->
            assertTrue(label.text.contains("2025") || label.text.contains("2026"))
        }
    }

    @Test
    fun `range shorter than a year never repeats a tick`() {
        // Two of the five evenly spaced ticks fall in September here.
        val points = dailyRange("2026-07-22", "2026-10-20")

        val labels = buildAxisLabels(points, parser)

        assertEquals(4, labels.size)
        assertEquals(0, labels.first().index)
        assertEquals(points.size - 1, labels.last().index)
        assertEquals(labels.size, labels.map { it.text }.distinct().size)
    }

    @Test
    fun `range shorter than two months labels the day`() {
        val points = dailyRange("2026-10-01", "2026-10-30")

        val labels = buildAxisLabels(points, parser)

        assertEquals(5, labels.size)
        assertEquals(0, labels.first().index)
        assertEquals(points.size - 1, labels.last().index)
        assertEquals(labels.size, labels.map { it.text }.distinct().size)
        // Short ranges label a day and month, no year.
        assertEquals(monthDayOf(points[labels.first().index].first), labels.first().text)
    }

    @Test
    fun `single point gets no labels`() {
        assertTrue(buildAxisLabels(listOf("2026-10-20" to 1.0), parser).isEmpty())
    }

    @Test
    fun `unparsable dates get no labels`() {
        val points = listOf("not a date" to 1.0, "2026-10-20" to 1.0)

        assertTrue(buildAxisLabels(points, parser).isEmpty())
    }

    private fun dailyRange(from: String, to: String): List<Pair<String, Double>> {
        val start = LocalDate.parse(from)
        val end = LocalDate.parse(to)
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.toString() to 1.0 }
            .toList()
    }

    /** Formats a date the same way the chart does, so expectations follow the locale. */
    private fun yearOf(isoDate: String): String =
        SimpleDateFormat("yyyy", Locale.getDefault()).format(parser.parse(isoDate))

    private fun monthDayOf(isoDate: String): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).format(parser.parse(isoDate))
}
