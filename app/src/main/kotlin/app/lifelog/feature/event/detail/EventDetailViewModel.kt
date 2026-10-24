package app.lifelog.feature.event.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import app.lifelog.domain.model.Occurrence
import app.lifelog.domain.occurrence.OccurrenceExpander
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.EventRepository
import app.lifelog.domain.time.TimeSource
import app.lifelog.domain.usecase.EventDeletion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState

    /** The event doesn't exist (any more), or it was just deleted from this screen. */
    data class Gone(val deletedHere: Boolean) : EventDetailUiState

    data class Ready(
        val event: Event,
        val occurrence: Occurrence,
        val category: Category?,
        /** The event's own zone when it differs from the device zone (PLAN.md D3). */
        val foreignZone: ZoneId?,
    ) : EventDetailUiState
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: EventRepository,
    categoryRepository: CategoryRepository,
    private val eventDeletion: EventDeletion,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val eventId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_EVENT_ID))
    private val deletedHere = MutableStateFlow(false)

    val uiState: StateFlow<EventDetailUiState> =
        combine(repository.observeEvent(eventId), categoryRepository.categories, deletedHere) { event, categories, deleted ->
            when {
                event == null -> EventDetailUiState.Gone(deletedHere = deleted)
                else -> {
                    val zone = timeSource.zone()
                    EventDetailUiState.Ready(
                        event = event,
                        occurrence = OccurrenceExpander.occurrenceOf(event, zone),
                        category = categories.firstOrNull { it.id == event.categoryId },
                        foreignZone = (event.timing as? EventTiming.Timed)?.zone?.takeIf { it.normalized() != zone.normalized() },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailUiState.Loading)

    fun onDelete() {
        val event = (uiState.value as? EventDetailUiState.Ready)?.event ?: return
        viewModelScope.launch {
            deletedHere.value = true
            eventDeletion.delete(event)
        }
    }

    companion object {
        /** Navigation argument name; must match the property in EventDetailRoute. */
        const val ARG_EVENT_ID = "eventId"
    }
}
