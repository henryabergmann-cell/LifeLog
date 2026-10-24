package app.lifelog.domain.usecase

import app.lifelog.domain.model.Event
import app.lifelog.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deletes events with undo. The deletion is held as app-wide state so the "Undo" snackbar can
 * be shown on whichever screen the user returns to after the detail screen closes.
 */
@Singleton
class EventDeletion @Inject constructor(
    private val repository: EventRepository,
) {
    private val _lastDeleted = MutableStateFlow<DeletedEvent?>(null)
    val lastDeleted: StateFlow<DeletedEvent?> = _lastDeleted.asStateFlow()

    suspend fun delete(event: Event) {
        repository.softDelete(event.id)
        _lastDeleted.value = DeletedEvent(eventId = event.id, title = event.title)
    }

    suspend fun undo(deleted: DeletedEvent) {
        repository.restore(deleted.eventId)
        _lastDeleted.compareAndSet(deleted, null)
    }

    /** The undo window has passed; the purge worker removes the event later. */
    fun dismiss(deleted: DeletedEvent) {
        _lastDeleted.compareAndSet(deleted, null)
    }
}

data class DeletedEvent(val eventId: Long, val title: String)
