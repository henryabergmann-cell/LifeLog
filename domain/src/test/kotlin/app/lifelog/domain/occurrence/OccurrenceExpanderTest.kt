package app.lifelog.domain.occurrence

import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class OccurrenceExpanderTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private val newYork = ZoneId.of("America/New_York")
    private val oct6 = LocalDate.of(2026, 10, 6)

    private fun timed(id: Long, start: LocalDateTime, end: LocalDateTime, zone: ZoneId = berlin, title: String = "E$id") =
        Event(id = id, title = title, timing = EventTiming.Timed(start, end, zone))

    private fun allDay(id: Long, start: LocalDate, end: LocalDate = start, title: String = "A$id") =
        Event(id = id, title = title, timing = EventTiming.AllDay(start, end))

    @Test
    fun `timed events are converted to the display zone`() {
        val event = timed(1, oct6.atTime(15, 0), oct6.atTime(16, 0), zone = berlin)

        val occurrence = OccurrenceExpander.occurrenceOf(event, newYork)

        assertEquals(oct6.atTime(9, 0), occurrence.start)
        assertEquals(oct6.atTime(10, 0), occurrence.end)
    }

    @Test
    fun `all-day events are floating and end exclusively after the last day`() {
        val event = allDay(1, oct6, oct6.plusDays(2))

        val occurrence = OccurrenceExpander.occurrenceOf(event, newYork)

        assertEquals(oct6.atStartOfDay(), occurrence.start)
        assertEquals(oct6.plusDays(3).atStartOfDay(), occurrence.end)
        assertTrue(occurrence.overlaps(oct6.plusDays(2)))
        assertFalse(occurrence.overlaps(oct6.plusDays(3)))
    }

    @Test
    fun `only occurrences overlapping the range are returned`() {
        val events = listOf(
            timed(1, oct6.minusDays(1).atTime(22, 0), oct6.atTime(1, 0)), // crosses into range
            timed(2, oct6.minusDays(1).atTime(9, 0), oct6.minusDays(1).atTime(10, 0)), // before
            timed(3, oct6.atTime(23, 0), oct6.plusDays(1).atTime(0, 0)), // ends at range end
            timed(4, oct6.plusDays(1).atTime(0, 0), oct6.plusDays(1).atTime(1, 0)), // after
        )

        val ids = OccurrenceExpander.expand(events, oct6, oct6.plusDays(1), berlin).map { it.event.id }

        assertEquals(listOf(1L, 3L), ids)
    }

    @Test
    fun `zero-length events are kept on their day`() {
        val events = listOf(timed(1, oct6.atTime(0, 0), oct6.atTime(0, 0)))

        assertEquals(1, OccurrenceExpander.expand(events, oct6, oct6.plusDays(1), berlin).size)
        assertEquals(0, OccurrenceExpander.expand(events, oct6.minusDays(1), oct6, berlin).size)
    }

    @Test
    fun `results are sorted by start with all-day events first`() {
        val events = listOf(
            timed(1, oct6.atTime(9, 0), oct6.atTime(10, 0), title = "b"),
            allDay(2, oct6, title = "z"),
            timed(3, oct6.atTime(0, 0), oct6.atTime(1, 0), title = "a"),
            timed(4, oct6.atTime(9, 0), oct6.atTime(9, 30), title = "a"),
        )

        val ids = OccurrenceExpander.expand(events, oct6, oct6.plusDays(1), berlin).map { it.event.id }

        assertEquals(listOf(2L, 3L, 4L, 1L), ids)
    }
}
