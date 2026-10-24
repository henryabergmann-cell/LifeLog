package app.lifelog.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Premium: one journal entry per calendar day. */
@Entity(tableName = "journal_entries")
internal data class JournalEntryEntity(
    /** Epoch day. */
    @PrimaryKey val date: Long,
    /** 1 (worst) to 5 (best); null when no mood was picked. */
    val mood: Int?,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
