package app.lifelog.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Premium: a calendar from CalendarContract and how Life Log uses it. */
@Entity(tableName = "device_calendars")
internal data class DeviceCalendarEntity(
    @PrimaryKey val calendarId: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
    /** Show this calendar's events (read-only) in Life Log views. */
    val visible: Boolean,
    /** Life Log events are mirrored into this calendar (at most one). */
    val isPushTarget: Boolean,
)
