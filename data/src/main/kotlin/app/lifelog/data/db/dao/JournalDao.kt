package app.lifelog.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.lifelog.data.db.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface JournalDao {

    /** Timeline: newest first. */
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE date = :epochDay")
    fun observe(epochDay: Long): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries WHERE date = :epochDay")
    suspend fun get(epochDay: Long): JournalEntryEntity?

    @Upsert
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE date = :epochDay")
    suspend fun delete(epochDay: Long)
}
