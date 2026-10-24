package app.lifelog.feature.calendar

import app.lifelog.domain.model.CalendarViewMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Half-open date range [start, endExclusive). */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && date.isBefore(endExclusive)
}

/**
 * Maps pager pages to calendar periods. Pages count from a fixed base date, so the mapping is
 * stateless and survives process death and rotation without extra bookkeeping.
 */
object CalendarPeriods {
    private val BASE: LocalDate = LocalDate.of(1900, 1, 1)
    private val LAST: LocalDate = LocalDate.of(2199, 12, 31)
    private const val MONTH_GRID_DAYS = 42

    fun periodStart(mode: CalendarViewMode, date: LocalDate, firstDay: DayOfWeek): LocalDate = when (mode) {
        CalendarViewMode.MONTH -> date.withDayOfMonth(1)
        CalendarViewMode.WEEK -> date.with(TemporalAdjusters.previousOrSame(firstDay))
        CalendarViewMode.DAY, CalendarViewMode.AGENDA -> date
    }

    fun pageFor(mode: CalendarViewMode, date: LocalDate, firstDay: DayOfWeek): Int {
        val clamped = date.coerceIn(BASE, LAST)
        return when (mode) {
            CalendarViewMode.MONTH -> ChronoUnit.MONTHS.between(YearMonth.from(BASE), YearMonth.from(clamped))
            CalendarViewMode.WEEK -> ChronoUnit.WEEKS.between(periodStart(mode, BASE, firstDay), periodStart(mode, clamped, firstDay))
            CalendarViewMode.DAY, CalendarViewMode.AGENDA -> ChronoUnit.DAYS.between(BASE, clamped)
        }.toInt()
    }

    /** First day of the period shown on [page]. */
    fun dateForPage(mode: CalendarViewMode, page: Int, firstDay: DayOfWeek): LocalDate = when (mode) {
        CalendarViewMode.MONTH -> BASE.plusMonths(page.toLong())
        CalendarViewMode.WEEK -> periodStart(mode, BASE, firstDay).plusWeeks(page.toLong())
        CalendarViewMode.DAY, CalendarViewMode.AGENDA -> BASE.plusDays(page.toLong())
    }

    fun pageCount(mode: CalendarViewMode, firstDay: DayOfWeek): Int = pageFor(mode, LAST, firstDay) + 1

    fun shift(mode: CalendarViewMode, date: LocalDate, periods: Long): LocalDate = when (mode) {
        CalendarViewMode.MONTH -> date.plusMonths(periods)
        CalendarViewMode.WEEK -> date.plusWeeks(periods)
        CalendarViewMode.DAY, CalendarViewMode.AGENDA -> date.plusDays(periods)
    }

    /** Six full weeks covering [month], starting on [firstDay]. */
    fun monthGrid(month: YearMonth, firstDay: DayOfWeek): List<LocalDate> {
        val start = periodStart(CalendarViewMode.WEEK, month.atDay(1), firstDay)
        return List(MONTH_GRID_DAYS) { start.plusDays(it.toLong()) }
    }

    fun weekDays(weekStart: LocalDate): List<LocalDate> = List(7) { weekStart.plusDays(it.toLong()) }

    /** Dates loaded for the pager: the period of [date] plus one period on each side. */
    fun loadWindow(mode: CalendarViewMode, date: LocalDate, firstDay: DayOfWeek): DateRange = when (mode) {
        CalendarViewMode.MONTH -> {
            val month = YearMonth.from(date)
            DateRange(
                monthGrid(month.minusMonths(1), firstDay).first(),
                monthGrid(month.plusMonths(1), firstDay).last().plusDays(1),
            )
        }
        CalendarViewMode.WEEK -> {
            val start = periodStart(mode, date, firstDay)
            DateRange(start.minusWeeks(1), start.plusWeeks(2))
        }
        CalendarViewMode.DAY -> DateRange(date.minusDays(1), date.plusDays(2))
        CalendarViewMode.AGENDA -> DateRange(date, date.plusDays(1))
    }
}
