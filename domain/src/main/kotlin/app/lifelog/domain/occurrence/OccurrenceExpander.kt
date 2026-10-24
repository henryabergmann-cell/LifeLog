package app.lifelog.domain.occurrence

import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import app.lifelog.domain.model.Occurrence
import java.time.LocalDate
import java.time.ZoneId

/**
 * Turns stored events into occurrences in the display zone. M2 handles one-off events;
 * M3 adds recurrence expansion here.
 */
object OccurrenceExpander {

    private val ordering = compareBy<Occurrence>(
        { it.start },
        { if (it.isAllDay) 0 else 1 },
        { it.event.title.lowercase() },
        { it.event.id },
    )

    /** Occurrences overlapping the dates [from] (inclusive) to [toExclusive], sorted by start. */
    fun expand(events: List<Event>, from: LocalDate, toExclusive: LocalDate, zone: ZoneId): List<Occurrence> {
        val rangeStart = from.atStartOfDay()
        val rangeEnd = toExclusive.atStartOfDay()
        return events.asSequence()
            .map { occurrenceOf(it, zone) }
            .filter { it.start < rangeEnd && (it.end > rangeStart || (it.start == it.end && it.start >= rangeStart)) }
            .sortedWith(ordering)
            .toList()
    }

    /** The (single) occurrence of a one-off event, converted to [zone]. */
    fun occurrenceOf(event: Event, zone: ZoneId): Occurrence = when (val timing = event.timing) {
        is EventTiming.AllDay -> Occurrence(
            event = event,
            start = timing.startDate.atStartOfDay(),
            end = timing.endDate.plusDays(1).atStartOfDay(),
        )
        is EventTiming.Timed -> Occurrence(
            event = event,
            start = timing.start.atZone(timing.zone).withZoneSameInstant(zone).toLocalDateTime(),
            end = timing.end.atZone(timing.zone).withZoneSameInstant(zone).toLocalDateTime(),
        )
    }
}
