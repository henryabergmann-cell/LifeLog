package app.lifelog.feature.calendar

import app.lifelog.domain.model.CalendarViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarPeriodsTest {

    private val oct7 = LocalDate.of(2026, 10, 7) // a Wednesday

    @Test
    fun `pages map back to the period start for every mode`() {
        for (mode in listOf(CalendarViewMode.MONTH, CalendarViewMode.WEEK, CalendarViewMode.DAY)) {
            for (firstDay in listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)) {
                val page = CalendarPeriods.pageFor(mode, oct7, firstDay)
                assertEquals(
                    "$mode/$firstDay",
                    CalendarPeriods.periodStart(mode, oct7, firstDay),
                    CalendarPeriods.dateForPage(mode, page, firstDay),
                )
                assertTrue(page in 0 until CalendarPeriods.pageCount(mode, firstDay))
            }
        }
    }

    @Test
    fun `adjacent periods are adjacent pages`() {
        val monday = DayOfWeek.MONDAY
        assertEquals(
            CalendarPeriods.pageFor(CalendarViewMode.MONTH, oct7, monday) + 1,
            CalendarPeriods.pageFor(CalendarViewMode.MONTH, LocalDate.of(2026, 11, 30), monday),
        )
        assertEquals(
            CalendarPeriods.pageFor(CalendarViewMode.WEEK, oct7, monday) + 1,
            CalendarPeriods.pageFor(CalendarViewMode.WEEK, LocalDate.of(2026, 10, 12), monday),
        )
    }

    @Test
    fun `week start follows the first day of week`() {
        assertEquals(LocalDate.of(2026, 10, 5), CalendarPeriods.periodStart(CalendarViewMode.WEEK, oct7, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 4), CalendarPeriods.periodStart(CalendarViewMode.WEEK, oct7, DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 10, 3), CalendarPeriods.periodStart(CalendarViewMode.WEEK, oct7, DayOfWeek.SATURDAY))
    }

    @Test
    fun `month grid has six full weeks containing the whole month`() {
        val grid = CalendarPeriods.monthGrid(YearMonth.of(2026, 2), DayOfWeek.MONDAY)

        assertEquals(42, grid.size)
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        assertTrue(LocalDate.of(2026, 2, 1) in grid && LocalDate.of(2026, 2, 28) in grid)
    }

    @Test
    fun `load window covers the neighbouring periods`() {
        val window = CalendarPeriods.loadWindow(CalendarViewMode.MONTH, oct7, DayOfWeek.MONDAY)

        assertTrue(LocalDate.of(2026, 9, 1) in window)
        assertTrue(LocalDate.of(2026, 11, 30) in window)
        assertEquals(
            DateRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 19)),
            CalendarPeriods.loadWindow(CalendarViewMode.WEEK, oct7, DayOfWeek.MONDAY),
        )
    }
}
