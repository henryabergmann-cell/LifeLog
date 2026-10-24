package app.lifelog.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per one-off event, recurring-series master, or overridden occurrence of a series.
 *
 * Time model (PLAN.md §3.3): timed events store wall-clock dates/times in [zoneId]; all-day
 * events are floating ([zoneId] is null). [startUtc]/[endUtc]/[seriesEndUtc] are derived index
 * columns (epoch millis) recomputed on every save; all-day events derive them at UTC midnight.
 * "Original start" values (overrides, exdates) use the same UTC-millis convention.
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesMasterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uid"], unique = true),
        Index("startUtc"),
        Index("endUtc"),
        Index("seriesMasterId"),
        Index("categoryId"),
    ],
)
internal data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable identity across backup/restore and device-calendar sync. */
    val uid: String,
    val title: String,
    val location: String?,
    val notes: String?,
    val categoryId: Long?,
    val allDay: Boolean,
    /** Epoch day in the event's zone. */
    val startDate: Long,
    /** Epoch day in the event's zone; inclusive for all-day events. */
    val endDate: Long,
    /** Second of day; null for all-day events. */
    val startTime: Int?,
    val endTime: Int?,
    /** IANA zone ID for timed events; null = floating (all-day). */
    val zoneId: String?,
    val startUtc: Long,
    val endUtc: Long,
    /** RFC 5545 RRULE subset; null for one-off events and overrides. */
    val rrule: String?,
    /** End of the last occurrence; null = infinite series. */
    val seriesEndUtc: Long?,
    /** Set on override rows ("edit this event"): the series master. */
    val seriesMasterId: Long?,
    /** Set on override rows: start of the occurrence this row replaces. */
    val originalStart: Long?,
    /** Soft-delete marker for the undo snackbar; purged later. */
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
