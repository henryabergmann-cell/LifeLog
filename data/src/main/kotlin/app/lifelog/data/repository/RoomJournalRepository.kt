package app.lifelog.data.repository

import app.lifelog.data.db.dao.JournalDao
import app.lifelog.data.db.entity.JournalEntryEntity
import app.lifelog.domain.model.JournalEntry
import app.lifelog.domain.model.Mood
import app.lifelog.domain.repository.JournalRepository
import app.lifelog.domain.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

internal class RoomJournalRepository @Inject constructor(
    private val journalDao: JournalDao,
    private val timeSource: TimeSource,
) : JournalRepository {

    override val entries: Flow<List<JournalEntry>> =
        journalDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observe(date: LocalDate): Flow<JournalEntry?> =
        journalDao.observe(date.toEpochDay()).map { it?.toDomain() }

    override suspend fun save(entry: JournalEntry) {
        val now = timeSource.now().toEpochMilli()
        val createdAt = journalDao.get(entry.date.toEpochDay())?.createdAt ?: now
        journalDao.upsert(
            JournalEntryEntity(
                date = entry.date.toEpochDay(),
                mood = entry.mood?.score,
                note = entry.note,
                createdAt = createdAt,
                updatedAt = now,
            ),
        )
    }

    override suspend fun delete(date: LocalDate) = journalDao.delete(date.toEpochDay())

    private fun JournalEntryEntity.toDomain() = JournalEntry(
        date = LocalDate.ofEpochDay(date),
        mood = Mood.fromScore(mood),
        note = note,
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}
