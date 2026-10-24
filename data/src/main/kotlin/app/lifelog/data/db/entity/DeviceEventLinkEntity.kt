package app.lifelog.data.db.entity

import androidx.room.Entity
import androidx.room.Index

/** Premium: maps a Life Log event to its mirrored copy in a device calendar. */
@Entity(
    tableName = "device_event_links",
    primaryKeys = ["eventUid", "deviceCalendarId"],
    indices = [Index("deviceEventId")],
)
internal data class DeviceEventLinkEntity(
    val eventUid: String,
    val deviceCalendarId: Long,
    val deviceEventId: Long,
    /** Hash of the pushed fields; skips writes when nothing changed. */
    val contentHash: String,
)
