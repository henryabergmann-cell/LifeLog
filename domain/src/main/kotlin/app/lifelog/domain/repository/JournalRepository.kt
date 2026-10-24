package app.lifelog.domain.repository

import app.lifelog.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface JournalRepository {
    /** All entries, newest first (the timeline). */
    val entries: Flow<List<JournalEntry>>

    fun observe(date: LocalDate): Flow<JournalEntry?>

    /** Inserts or replaces the entry for its date. */
    suspend fun save(entry: JournalEntry)

    suspend fun delete(date: LocalDate)
}
