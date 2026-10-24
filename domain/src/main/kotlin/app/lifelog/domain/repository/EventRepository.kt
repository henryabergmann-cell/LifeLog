package app.lifelog.domain.repository

import app.lifelog.domain.model.Event
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface EventRepository {
    /**
     * Events that may produce occurrences between [start] and [end]: one-off events overlapping
     * the range and series whose recurrence overlaps it. Callers expand and filter precisely.
     */
    fun observeCandidates(start: Instant, end: Instant): Flow<List<Event>>

    /** Emits null when the event doesn't exist or is deleted. */
    fun observeEvent(id: Long): Flow<Event?>

    suspend fun getEvent(id: Long): Event?

    /** Inserts ([Event.id] == 0) or updates the event and its reminders. Returns the ID. */
    suspend fun save(event: Event): Long

    /** Hides the event; [restore] brings it back until it is purged. */
    suspend fun softDelete(id: Long)

    suspend fun restore(id: Long)

    /** Permanently removes events soft-deleted before [olderThan]. Returns the number removed. */
    suspend fun purgeDeleted(olderThan: Instant): Int

    /** Full-text search over title, location and notes; blank queries return nothing. */
    fun search(query: String): Flow<List<Event>>
}
