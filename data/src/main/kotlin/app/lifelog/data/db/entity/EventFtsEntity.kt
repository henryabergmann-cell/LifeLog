package app.lifelog.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/**
 * Full-text index over [EventEntity]. External content table: Room keeps it in sync with
 * triggers. unicode61 folds case and diacritics, so "muller" also finds "Müller".
 */
@Fts4(contentEntity = EventEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "events_fts")
internal data class EventFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    val title: String,
    val location: String?,
    val notes: String?,
)
