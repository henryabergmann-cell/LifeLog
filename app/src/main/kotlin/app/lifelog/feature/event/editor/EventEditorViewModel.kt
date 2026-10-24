package app.lifelog.feature.event.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.model.Category
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.EventRepository
import app.lifelog.domain.repository.SettingsRepository
import app.lifelog.domain.time.TimeSource
import app.lifelog.domain.usecase.EventValidationError
import app.lifelog.domain.usecase.SaveEventResult
import app.lifelog.domain.usecase.SaveEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class EventEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val notFound: Boolean = false,
    val draft: EventDraft? = null,
    val categories: List<Category> = emptyList(),
    /** Only set after the user tried to save, so errors don't show while typing. */
    val validationError: EventValidationError? = null,
    val isSaving: Boolean = false,
    /** Set once saved; the screen then closes. */
    val savedEventId: Long? = null,
    val isDirty: Boolean = false,
    val maxReminders: Int = 1,
    /** The event's zone when it differs from the device zone (PLAN.md D3). */
    val foreignZone: ZoneId? = null,
)

@HiltViewModel
class EventEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: EventRepository,
    private val saveEvent: SaveEventUseCase,
    private val settingsRepository: SettingsRepository,
    private val timeSource: TimeSource,
    categoryRepository: CategoryRepository,
    featureGate: FeatureGate,
) : ViewModel() {

    private data class Editor(
        val draft: EventDraft? = null,
        val initial: EventDraft? = null,
        val showErrors: Boolean = false,
        val isSaving: Boolean = false,
        val savedEventId: Long? = null,
        val notFound: Boolean = false,
    )

    private val eventId = savedStateHandle.get<Long>(ARG_EVENT_ID) ?: NO_ID
    private val duplicateOfId = savedStateHandle.get<Long>(ARG_DUPLICATE_OF_ID) ?: NO_ID
    private val initialDate = savedStateHandle.get<Long>(ARG_DATE_EPOCH_DAY)?.takeIf { it != NO_DATE }?.let(LocalDate::ofEpochDay)
    private val initialMinute = savedStateHandle.get<Int>(ARG_START_MINUTE)?.takeIf { it >= 0 }

    private val editor = MutableStateFlow(
        Editor(draft = restore(KEY_DRAFT), initial = restore(KEY_INITIAL)),
    )

    val uiState: StateFlow<EventEditorUiState> =
        combine(editor, categoryRepository.categories, featureGate.limits) { editor, categories, limits ->
            val draft = editor.draft
            EventEditorUiState(
                isLoading = draft == null && !editor.notFound,
                isNew = eventId == NO_ID,
                notFound = editor.notFound,
                draft = draft,
                categories = categories,
                validationError = draft?.takeIf { editor.showErrors }?.let { SaveEventUseCase.validate(it.toEvent()) },
                isSaving = editor.isSaving,
                savedEventId = editor.savedEventId,
                isDirty = draft != null && draft != editor.initial,
                maxReminders = limits.maxRemindersPerEvent,
                foreignZone = draft?.takeIf { !it.allDay }?.zone?.takeIf { it.normalized() != timeSource.zone().normalized() },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventEditorUiState())

    init {
        if (editor.value.draft == null) viewModelScope.launch { loadInitialDraft() }
    }

    fun onTitleChange(title: String) = updateDraft { it.copy(title = title) }

    fun onLocationChange(location: String) = updateDraft { it.copy(location = location) }

    fun onNotesChange(notes: String) = updateDraft { it.copy(notes = notes) }

    fun onAllDayChange(allDay: Boolean) = updateDraft { draft ->
        if (!allDay && !draft.end.isAfter(draft.start)) {
            // Coming back from all-day with a collapsed range: restore a sensible end time.
            draft.copy(allDay = false).withTimes(draft.start, draft.start.plusHours(1))
        } else {
            draft.copy(allDay = allDay)
        }
    }

    fun onStartDateChange(date: LocalDate) = updateDraft { it.withStartKeepingDuration(date.atTime(it.start.toLocalTime())) }

    fun onStartTimeChange(time: LocalTime) = updateDraft { it.withStartKeepingDuration(it.startDate.atTime(time)) }

    fun onEndDateChange(date: LocalDate) = updateDraft { it.withTimes(it.start, date.atTime(it.end.toLocalTime())) }

    fun onEndTimeChange(time: LocalTime) = updateDraft { it.withTimes(it.start, it.endDate.atTime(time)) }

    fun onCategoryChange(categoryId: Long?) = updateDraft { it.copy(categoryId = categoryId) }

    /** Sets the first reminder; further reminders (Premium, or kept after a downgrade) stay. */
    fun onReminderChange(minutesBefore: Int?) = updateDraft { draft ->
        val others = draft.reminderMinutes.drop(1)
        draft.copy(reminderMinutes = listOfNotNull(minutesBefore) + others)
    }

    fun onSave() {
        val draft = editor.value.draft ?: return
        if (editor.value.isSaving) return
        editor.update { it.copy(showErrors = true) }
        if (SaveEventUseCase.validate(draft.toEvent()) != null) return

        editor.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveEvent(draft.toEvent())) {
                is SaveEventResult.Saved -> editor.update { it.copy(isSaving = false, savedEventId = result.eventId) }
                is SaveEventResult.Invalid -> editor.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun loadInitialDraft() {
        val sourceId = if (eventId != NO_ID) eventId else duplicateOfId
        val draft = if (sourceId != NO_ID) {
            val event = repository.getEvent(sourceId)
            if (event == null) {
                editor.update { it.copy(notFound = true) }
                return
            }
            EventDraft.from(event).let { if (eventId == NO_ID) it.copy(eventId = 0, uid = "") else it }
        } else {
            newDraft()
        }
        // A duplicate starts out "dirty": leaving without saving should ask first.
        val initial = if (duplicateOfId != NO_ID) null else draft
        editor.update { it.copy(draft = draft, initial = initial) }
        persist(KEY_DRAFT, draft)
        persist(KEY_INITIAL, initial)
    }

    private suspend fun newDraft(): EventDraft {
        val settings = settingsRepository.settings.first()
        val zone = timeSource.zone()
        val now = timeSource.now().atZone(zone).toLocalDateTime()
        val date = initialDate ?: now.toLocalDate()
        val startMinute = initialMinute ?: if (date == now.toLocalDate()) {
            ((now.hour + 1) * 60).coerceAtMost(23 * 60)
        } else {
            EventDraft.DEFAULT_START_MINUTE
        }
        val start = date.atStartOfDay().plusMinutes(startMinute.toLong())
        return EventDraft(
            startEpochDay = 0,
            startMinute = 0,
            endEpochDay = 0,
            endMinute = 0,
            zoneId = zone.id,
            reminderMinutes = listOfNotNull(settings.defaultReminderMinutes),
        ).withTimes(start, start.plusMinutes(settings.defaultEventDurationMinutes.toLong()))
    }

    private fun updateDraft(transform: (EventDraft) -> EventDraft) {
        val updated = editor.value.draft?.let(transform) ?: return
        editor.update { it.copy(draft = updated) }
        persist(KEY_DRAFT, updated)
    }

    private fun restore(key: String): EventDraft? =
        savedStateHandle.get<String>(key)?.let { runCatching { json.decodeFromString<EventDraft>(it) }.getOrNull() }

    private fun persist(key: String, draft: EventDraft?) {
        savedStateHandle[key] = draft?.let { json.encodeToString(EventDraft.serializer(), it) }
    }

    companion object {
        /** Navigation argument names; must match the properties of EventEditorRoute. */
        const val ARG_EVENT_ID = "eventId"
        const val ARG_DUPLICATE_OF_ID = "duplicateOfId"
        const val ARG_DATE_EPOCH_DAY = "dateEpochDay"
        const val ARG_START_MINUTE = "startMinute"

        const val NO_ID = -1L
        const val NO_DATE = Long.MIN_VALUE

        private const val KEY_DRAFT = "draft"
        private const val KEY_INITIAL = "initialDraft"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
