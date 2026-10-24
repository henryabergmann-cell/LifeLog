package app.lifelog.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Alarms currently registered with AlarmManager. Deliberately without foreign keys: an alarm
 * must still be cancellable after its event is gone.
 */
@Entity(tableName = "scheduled_alarms", indices = [Index("eventId")])
internal data class ScheduledAlarmEntity(
    @PrimaryKey val requestCode: Int,
    val eventId: Long,
    val reminderId: Long,
    val occurrenceStart: Long,
    val triggerAtUtc: Long,
)
