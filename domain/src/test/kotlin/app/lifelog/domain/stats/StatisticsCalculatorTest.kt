package app.lifelog.domain.stats

import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import app.lifelog.domain.model.Occurrence
import app.lifelog.domain.occurrence.OccurrenceExpander
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class StatisticsCalculatorTest {

    private val mon = LocalDate.of(2026, 10, 5) // a Monday

    private fun timed(start: LocalDateTime, minutes: Long, categoryId: Long? = null): Occurrence =
        OccurrenceExpander.occurrenceOf(
            Event(title = "t", categoryId = categoryId, timing = EventTiming.Timed(start, start.plusMinutes(minutes), ZoneOffset.UTC)),
            ZoneOffset.UTC,
        )

    private fun allDay(date: LocalDate, days: Long = 1, categoryId: Long? = null): Occurrence =
        OccurrenceExpander.occurrenceOf(
            Event(title = "a", categoryId = categoryId, timing = EventTiming.AllDay(date, date.plusDays(days - 1))),
            ZoneOffset.UTC,
        )

    @Test
    fun `counts appointments, hours, weeks, weekdays and categories`() {
        val occurrences = listOf(
            timed(mon.atTime(9, 0), 60, categoryId = 1),
            timed(mon.atTime(9, 30), 30, categoryId = 1),
            timed(mon.plusDays(2).atTime(14, 0), 90, categoryId = 2),
            allDay(mon.plusDays(8)), // next week, Tuesday
        )

        val stats = StatisticsCalculator.calculate(occurrences, mon, mon.plusWeeks(2), StatsBucket.WEEK, DayOfWeek.MONDAY)

        assertEquals(4, stats.appointmentCount)
        assertEquals(180L, stats.totalTimedMinutes) // all-day appointments add no hours
        assertEquals(listOf(BucketCount(mon, 3), BucketCount(mon.plusWeeks(1), 1)), stats.buckets)
        assertEquals(DayOfWeek.MONDAY to 2, stats.weekdayCounts.first())
        assertEquals(DayOfWeek.TUESDAY to 1, stats.weekdayCounts[1])
        assertEquals(9, stats.busiestHour)
        assertEquals(listOf(CategoryCount(1, 2), CategoryCount(2, 1), CategoryCount(null, 1)), stats.categoryCounts)
        assertEquals(mon, stats.busiestDate)
        assertEquals(2, stats.busiestDateCount)
    }

    @Test
    fun `occurrences are counted by start date only`() {
        val occurrences = listOf(
            allDay(mon.minusDays(1), days = 3), // starts before the range
            timed(mon.plusWeeks(1).atTime(8, 0), 30), // starts on the exclusive end
        )

        val stats = StatisticsCalculator.calculate(occurrences, mon, mon.plusWeeks(1), StatsBucket.WEEK, DayOfWeek.MONDAY)

        assertTrue(stats.isEmpty)
        assertNull(stats.busiestHour)
        assertEquals(listOf(BucketCount(mon, 0)), stats.buckets)
    }

    @Test
    fun `month buckets include empty months and weekdays follow the first day of week`() {
        val stats = StatisticsCalculator.calculate(
            listOf(timed(LocalDate.of(2026, 3, 10).atTime(10, 0), 60)),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 1),
            StatsBucket.MONTH,
            DayOfWeek.SUNDAY,
        )

        assertEquals(listOf(0, 0, 1), stats.buckets.map { it.count })
        assertEquals(LocalDate.of(2026, 3, 1), stats.buckets.last().start)
        assertEquals(DayOfWeek.SUNDAY, stats.weekdayCounts.first().first)
    }
}
