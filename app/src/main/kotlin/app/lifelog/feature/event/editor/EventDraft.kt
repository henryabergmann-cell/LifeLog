package app.lifelog.feature.event.editor

import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Editor state that survives process death (stored as JSON in SavedStateHandle).
 * Times are kept even while "all day" is on, so toggling it back restores them.
 */
@Serializable
data class EventDraft(
    val eventId: Long = 0,
    val uid: String = "",
    val title: String = "",
    val allDay: Boolean = false,
    val startEpochDay: Long,
    val startMinute: Int,
    val endEpochDay: Long,
    val endMinute: Int,
    val zoneId: String,
    val location: String = "",
    val notes: String = "",
    val categoryId: Long? = null,
    val reminderMinutes: List<Int> = emptyList(),
    val rrule: String? = null,
) {
    val startDate: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)
    val endDate: LocalDate get() = LocalDate.ofEpochDay(endEpochDay)
    val start: LocalDateTime get() = startDate.atStartOfDay().plusMinutes(startMinute.toLong())
    val end: LocalDateTime get() = endDate.atStartOfDay().plusMinutes(endMinute.toLong())
    val zone: ZoneId get() = ZoneId.of(zoneId)

    fun toEvent(): Event = Event(
        id = eventId,
        uid = uid,
        title = title,
        location = location,
        notes = notes,
        categoryId = categoryId,
        timing = if (allDay) EventTiming.AllDay(startDate, endDate) else EventTiming.Timed(start, end, zone),
        reminderMinutes = reminderMinutes,
        rrule = rrule,
    )

    fun withTimes(newStart: LocalDateTime, newEnd: LocalDateTime): EventDraft = copy(
        startEpochDay = newStart.toLocalDate().toEpochDay(),
        startMinute = newStart.toLocalTime().toSecondOfDay() / 60,
        endEpochDay = newEnd.toLocalDate().toEpochDay(),
        endMinute = newEnd.toLocalTime().toSecondOfDay() / 60,
    )

    /** Moves the start and shifts the end by the same amount, keeping the duration. */
    fun withStartKeepingDuration(newStart: LocalDateTime): EventDraft =
        withTimes(newStart, newStart.plus(Duration.between(start, end)))

    companion object {
        fun from(event: Event): EventDraft {
            val base = EventDraft(
                eventId = event.id,
                uid = event.uid,
                title = event.title,
                startEpochDay = 0,
                startMinute = DEFAULT_START_MINUTE,
                endEpochDay = 0,
                endMinute = DEFAULT_START_MINUTE + 60,
                zoneId = ZoneId.systemDefault().id,
                location = event.location.orEmpty(),
                notes = event.notes.orEmpty(),
                categoryId = event.categoryId,
                reminderMinutes = event.reminderMinutes,
                rrule = event.rrule,
            )
            return when (val timing = event.timing) {
                is EventTiming.AllDay -> base.copy(
                    allDay = true,
                    startEpochDay = timing.startDate.toEpochDay(),
                    endEpochDay = timing.endDate.toEpochDay(),
                )
                is EventTiming.Timed -> base.copy(zoneId = timing.zone.id).withTimes(timing.start, timing.end)
            }
        }

        const val DEFAULT_START_MINUTE = 9 * 60
    }
}
