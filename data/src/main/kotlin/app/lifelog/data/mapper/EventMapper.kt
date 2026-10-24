package app.lifelog.data.mapper

import app.lifelog.data.db.entity.EventEntity
import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

internal fun EventEntity.toDomain(reminderMinutes: List<Int>): Event {
    val timing = if (allDay) {
        EventTiming.AllDay(LocalDate.ofEpochDay(startDate), LocalDate.ofEpochDay(endDate))
    } else {
        EventTiming.Timed(
            start = LocalDateTime.of(LocalDate.ofEpochDay(startDate), LocalTime.ofSecondOfDay((startTime ?: 0).toLong())),
            end = LocalDateTime.of(LocalDate.ofEpochDay(endDate), LocalTime.ofSecondOfDay((endTime ?: 0).toLong())),
            zone = zoneOrUtc(zoneId),
        )
    }
    return Event(
        id = id,
        uid = uid,
        title = title,
        location = location,
        notes = notes,
        categoryId = categoryId,
        timing = timing,
        reminderMinutes = reminderMinutes,
        rrule = rrule,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}

/**
 * Maps to a row including the derived UTC index columns (PLAN.md §3.1). Series-related
 * columns that the domain model doesn't carry yet are left for the caller to preserve.
 */
internal fun Event.toEntity(uid: String, createdAt: Long, updatedAt: Long): EventEntity {
    val base = EventEntity(
        id = id,
        uid = uid,
        title = title,
        location = location,
        notes = notes,
        categoryId = categoryId,
        allDay = false,
        startDate = 0,
        endDate = 0,
        startTime = null,
        endTime = null,
        zoneId = null,
        startUtc = 0,
        endUtc = 0,
        rrule = rrule,
        seriesEndUtc = null,
        seriesMasterId = null,
        originalStart = null,
        deletedAt = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
    return when (val timing = timing) {
        is EventTiming.AllDay -> base.copy(
            allDay = true,
            startDate = timing.startDate.toEpochDay(),
            endDate = timing.endDate.toEpochDay(),
            startUtc = timing.startDate.utcMillis(),
            endUtc = timing.endDate.plusDays(1).utcMillis(),
        )
        is EventTiming.Timed -> base.copy(
            startDate = timing.start.toLocalDate().toEpochDay(),
            endDate = timing.end.toLocalDate().toEpochDay(),
            startTime = timing.start.toLocalTime().toSecondOfDay(),
            endTime = timing.end.toLocalTime().toSecondOfDay(),
            zoneId = timing.zone.id,
            startUtc = timing.start.atZone(timing.zone).toInstant().toEpochMilli(),
            endUtc = timing.end.atZone(timing.zone).toInstant().toEpochMilli(),
        )
    }
}

private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** A zone ID unknown to this device's tz database (e.g. from a newer backup) falls back to UTC. */
private fun zoneOrUtc(id: String?): ZoneId =
    try {
        id?.let(ZoneId::of) ?: ZoneOffset.UTC
    } catch (_: DateTimeException) {
        ZoneOffset.UTC
    }
