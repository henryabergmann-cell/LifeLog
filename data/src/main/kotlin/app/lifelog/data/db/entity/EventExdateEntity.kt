package app.lifelog.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/** An occurrence of a series removed with "delete this event". */
@Entity(
    tableName = "event_exdates",
    primaryKeys = ["eventId", "originalStart"],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class EventExdateEntity(
    val eventId: Long,
    val originalStart: Long,
)
