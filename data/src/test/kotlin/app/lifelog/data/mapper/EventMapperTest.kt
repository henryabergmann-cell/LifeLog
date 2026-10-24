package app.lifelog.data.mapper

import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class EventMapperTest {

    private val day = LocalDate.of(2026, 10, 6)

    @Test
    fun `timed event round-trips and derives UTC columns from its zone`() {
        val zone = ZoneId.of("Europe/Berlin")
        val event = Event(
            id = 3,
            uid = "u",
            title = "Dentist",
            location = "Main St",
            categoryId = 2,
            timing = EventTiming.Timed(day.atTime(9, 30), day.atTime(10, 15), zone),
            reminderMinutes = listOf(15),
            createdAt = Instant.ofEpochMilli(1),
            updatedAt = Instant.ofEpochMilli(2),
        )

        val entity = event.toEntity(uid = "u", createdAt = 1, updatedAt = 2)

        assertEquals(day.atTime(9, 30).atZone(zone).toInstant().toEpochMilli(), entity.startUtc) // 07:30 UTC (CEST)
        assertEquals(day.atTime(10, 15).atZone(zone).toInstant().toEpochMilli(), entity.endUtc)
        assertEquals("Europe/Berlin", entity.zoneId)
        assertEquals(event, entity.toDomain(listOf(15)))
    }

    @Test
    fun `all-day event is floating with UTC-midnight index columns`() {
        val event = Event(
            id = 1,
            uid = "a",
            title = "Holiday",
            timing = EventTiming.AllDay(day, day.plusDays(1)),
            createdAt = Instant.ofEpochMilli(0),
            updatedAt = Instant.ofEpochMilli(0),
        )

        val entity = event.toEntity(uid = "a", createdAt = 0, updatedAt = 0)

        assertNull(entity.zoneId)
        assertNull(entity.startTime)
        assertEquals(day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), entity.startUtc)
        assertEquals(day.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), entity.endUtc)
        assertEquals(event, entity.toDomain(emptyList()))
    }

    @Test
    fun `unknown zone IDs fall back to UTC instead of crashing`() {
        val entity = Event(
            title = "x",
            timing = EventTiming.Timed(day.atTime(9, 0), day.atTime(10, 0), ZoneOffset.UTC),
        ).toEntity(uid = "x", createdAt = 0, updatedAt = 0).copy(zoneId = "Mars/Olympus_Mons")

        assertEquals(ZoneOffset.UTC, (entity.toDomain(emptyList()).timing as EventTiming.Timed).zone)
    }
}
