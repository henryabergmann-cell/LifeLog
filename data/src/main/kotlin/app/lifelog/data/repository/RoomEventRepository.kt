package app.lifelog.data.repository

import androidx.room.withTransaction
import app.lifelog.data.db.LifeLogDatabase
import app.lifelog.data.db.dao.EventDao
import app.lifelog.data.db.dao.ReminderDao
import app.lifelog.data.db.entity.EventEntity
import app.lifelog.data.db.entity.ReminderEntity
import app.lifelog.data.mapper.toDomain
import app.lifelog.data.mapper.toEntity
import app.lifelog.data.search.FtsQuery
import app.lifelog.domain.model.Event
import app.lifelog.domain.repository.EventRepository
import app.lifelog.domain.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

internal class RoomEventRepository @Inject constructor(
    private val db: LifeLogDatabase,
    private val eventDao: EventDao,
    private val reminderDao: ReminderDao,
    private val timeSource: TimeSource,
) : EventRepository {

    override fun observeCandidates(start: Instant, end: Instant): Flow<List<Event>> =
        eventDao.observeInRange(start.toEpochMilli(), end.toEpochMilli()).map { withReminders(it) }

    override fun observeEvent(id: Long): Flow<Event?> =
        eventDao.observeById(id).map { entity ->
            entity?.takeIf { it.deletedAt == null }?.toDomain(reminderMinutes(id))
        }

    override suspend fun getEvent(id: Long): Event? =
        eventDao.getById(id)?.takeIf { it.deletedAt == null }?.toDomain(reminderMinutes(id))

    override suspend fun save(event: Event): Long = db.withTransaction {
        val now = timeSource.now().toEpochMilli()
        val existing = if (event.id != 0L) eventDao.getById(event.id) else null
        val entity = event.toEntity(
            uid = existing?.uid ?: event.uid.ifBlank { UUID.randomUUID().toString() },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val id = if (existing != null) {
            eventDao.update(entity.preservingSeriesColumnsOf(existing))
            existing.id
        } else {
            eventDao.insert(entity.copy(id = 0))
        }
        reminderDao.deleteForEvent(id)
        if (event.reminderMinutes.isNotEmpty()) {
            reminderDao.insertAll(event.reminderMinutes.map { ReminderEntity(eventId = id, minutesBefore = it) })
        }
        id
    }

    override suspend fun softDelete(id: Long) = eventDao.softDelete(id, timeSource.now().toEpochMilli())

    override suspend fun restore(id: Long) = eventDao.restore(id)

    override suspend fun purgeDeleted(olderThan: Instant): Int = eventDao.purgeDeletedBefore(olderThan.toEpochMilli())

    override fun search(query: String): Flow<List<Event>> {
        val ftsQuery = FtsQuery.from(query) ?: return flowOf(emptyList())
        return eventDao.search(ftsQuery).map { withReminders(it) }
    }

    private suspend fun reminderMinutes(eventId: Long): List<Int> =
        reminderDao.getForEvent(eventId).map { it.minutesBefore }

    private suspend fun withReminders(entities: List<EventEntity>): List<Event> {
        if (entities.isEmpty()) return emptyList()
        // Chunked to stay well below SQLite's bound-parameter limit.
        val reminders = entities.map { it.id }.chunked(500)
            .flatMap { reminderDao.getForEvents(it) }
            .groupBy({ it.eventId }, { it.minutesBefore })
        return entities.map { it.toDomain(reminders[it.id].orEmpty()) }
    }

    private fun EventEntity.preservingSeriesColumnsOf(existing: EventEntity) = copy(
        id = existing.id,
        seriesEndUtc = if (rrule != null) existing.seriesEndUtc else null,
        seriesMasterId = existing.seriesMasterId,
        originalStart = existing.originalStart,
        deletedAt = existing.deletedAt,
    )
}
