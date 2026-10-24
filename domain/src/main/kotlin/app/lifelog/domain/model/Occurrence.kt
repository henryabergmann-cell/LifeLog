package app.lifelog.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One appearance of an [event] on the calendar, in the display (device) time zone.
 * For all-day events [start] is the start of the first day and [end] the start of the day
 * after the last day; [end] is always exclusive.
 */
data class Occurrence(
    val event: Event,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    val isAllDay: Boolean get() = event.timing is EventTiming.AllDay

    /** Unique within a list of occurrences (recurring events share the event ID). */
    val key: String get() = "${event.id}@$start"

    fun overlaps(date: LocalDate): Boolean {
        val dayStart = date.atStartOfDay()
        val dayEnd = dayStart.plusDays(1)
        // A zero-length event belongs to the day it starts on.
        return start < dayEnd && (end > dayStart || (start == end && start >= dayStart))
    }
}
