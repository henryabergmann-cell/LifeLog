package app.lifelog.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.lifelog.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EventDao {

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeById(id: Long): Flow<EventEntity?>

    /**
     * Candidate rows for the half-open range [startUtc, endUtc): one-off events and overrides
     * that overlap it, plus series masters whose series overlaps it. Callers pad the range by a
     * day and filter precisely after expanding recurrences (floating all-day events).
     */
    @Query(
        """
        SELECT * FROM events
        WHERE deletedAt IS NULL AND startUtc < :endUtc AND (
            (rrule IS NULL AND endUtc > :startUtc) OR
            (rrule IS NOT NULL AND (seriesEndUtc IS NULL OR seriesEndUtc > :startUtc))
        )
        ORDER BY startUtc, id
        """,
    )
    fun observeInRange(startUtc: Long, endUtc: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE seriesMasterId = :masterId AND deletedAt IS NULL")
    suspend fun getOverrides(masterId: Long): List<EventEntity>

    /** [ftsQuery] must already be a sanitized FTS4 MATCH expression. */
    @Query(
        """
        SELECT events.* FROM events
        JOIN events_fts ON events.id = events_fts.rowid
        WHERE events_fts MATCH :ftsQuery AND events.deletedAt IS NULL
        ORDER BY events.startUtc DESC
        """,
    )
    fun search(ftsQuery: String): Flow<List<EventEntity>>

    /** Soft-deletes an event together with its overrides (undo restores both). */
    @Query("UPDATE events SET deletedAt = :deletedAt WHERE id = :id OR seriesMasterId = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("UPDATE events SET deletedAt = NULL WHERE id = :id OR seriesMasterId = :id")
    suspend fun restore(id: Long)

    /** Permanently removes rows soft-deleted before [before]; overrides cascade. */
    @Query("DELETE FROM events WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeletedBefore(before: Long): Int
}
