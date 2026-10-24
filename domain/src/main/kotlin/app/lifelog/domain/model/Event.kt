package app.lifelog.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** An appointment as stored: a one-off event or (from M3) the master of a recurring series. */
data class Event(
    val id: Long = 0,
    /** Stable identity across backup/restore and sync; assigned on first save when blank. */
    val uid: String = "",
    val title: String,
    val location: String? = null,
    val notes: String? = null,
    val categoryId: Long? = null,
    val timing: EventTiming,
    /** Reminder offsets in minutes before the start. Free tier: one; Premium: several. */
    val reminderMinutes: List<Int> = emptyList(),
    /** RFC 5545 RRULE subset; null for one-off events. */
    val rrule: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

sealed interface EventTiming {
    /** Floating dates, the same everywhere (PLAN.md D3). [endDate] is inclusive. */
    data class AllDay(val startDate: LocalDate, val endDate: LocalDate) : EventTiming

    /** Wall-clock times anchored to [zone] (PLAN.md D3). */
    data class Timed(val start: LocalDateTime, val end: LocalDateTime, val zone: ZoneId) : EventTiming
}
