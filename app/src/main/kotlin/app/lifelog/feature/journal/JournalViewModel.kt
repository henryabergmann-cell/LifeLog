package app.lifelog.feature.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.model.JournalEntry
import app.lifelog.domain.model.Mood
import app.lifelog.domain.premium.PremiumManager
import app.lifelog.domain.repository.JournalRepository
import app.lifelog.domain.time.TimeSource
import app.lifelog.domain.usecase.SaveJournalEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class JournalUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
    val selectedDate: LocalDate = LocalDate.MIN,
    val mood: Mood? = null,
    val note: String = "",
    /** True while typed changes are waiting to be saved (autosave runs shortly after typing). */
    val hasUnsavedChanges: Boolean = false,
    val entries: List<JournalEntry> = emptyList(),
) {
    val canGoForward: Boolean get() = selectedDate.isBefore(today)

    /** After Premium lapses the journal stays readable but can't be edited (PLAN.md D2). */
    val isReadOnly: Boolean get() = !isPremium
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val saveEntry: SaveJournalEntryUseCase,
    premiumManager: PremiumManager,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())
    private val selectedDate = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_DATE)?.let(LocalDate::ofEpochDay) ?: today.value,
    )

    /** Local edits not yet written; null means "show what is stored". */
    private val edits = MutableStateFlow<JournalEntry?>(null)
    private var saveJob: Job? = null

    private val stored = selectedDate.flatMapLatest { date ->
        repository.observe(date).map { entry -> date to entry }
    }

    /**
     * The note text field's value. Kept as synchronous Compose state rather than derived from
     * [uiState]: a text field fed through an asynchronous flow can drop keystrokes while the
     * keyboard is composing a word.
     */
    var noteText by mutableStateOf("")
        private set

    init {
        // Load the stored note when the day changes, unless the user is mid-edit.
        viewModelScope.launch {
            stored.collect { (date, entry) ->
                if (edits.value?.date != date) noteText = entry?.note.orEmpty()
            }
        }
    }

    val uiState: StateFlow<JournalUiState> = combine(
        combine(selectedDate, today, ::Pair),
        stored,
        edits,
        repository.entries,
        premiumManager.isPremium,
    ) { (date, todayValue), (storedDate, storedEntry), edit, entries, premium ->
        val shown = edit?.takeIf { it.date == date }
            ?: storedEntry?.takeIf { storedDate == date }
            ?: JournalEntry(date)
        JournalUiState(
            isLoading = false,
            isPremium = premium,
            today = todayValue,
            selectedDate = date,
            mood = shown.mood,
            note = shown.note,
            hasUnsavedChanges = edit != null,
            entries = entries,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    /** Tapping the selected mood again clears it. */
    fun onMoodSelected(mood: Mood) = edit { it.copy(mood = if (it.mood == mood) null else mood) }

    fun onNoteChange(note: String) {
        if (uiState.value.isReadOnly) return
        noteText = note.take(JournalEntry.MAX_NOTE_LENGTH)
        edit { it.copy(note = noteText) }
    }

    fun onDateSelected(date: LocalDate) {
        val target = minOf(date, today.value)
        saveJob?.cancel()
        viewModelScope.launch {
            flush()
            selectedDate.value = target
            savedStateHandle[KEY_DATE] = target.toEpochDay()
        }
    }

    fun onPreviousDay() = onDateSelected(selectedDate.value.minusDays(1))

    fun onNextDay() = onDateSelected(selectedDate.value.plusDays(1))

    /** Saves pending edits right away (e.g. when the screen goes to the background). */
    fun onPause() {
        saveJob?.cancel()
        viewModelScope.launch { flush() }
    }

    fun refreshToday() {
        today.value = timeSource.today()
    }

    private fun edit(transform: (JournalEntry) -> JournalEntry) {
        val state = uiState.value
        if (state.isReadOnly) return
        val current = edits.value?.takeIf { it.date == state.selectedDate }
            ?: JournalEntry(state.selectedDate, state.mood, noteText)
        edits.value = transform(current)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            flush()
        }
    }

    /** Callers cancel [saveJob] first when flushing early; the autosave job calls this itself. */
    private suspend fun flush() {
        val pending = edits.value ?: return
        saveEntry(pending)
        // Keep newer edits made while saving; they get their own save.
        edits.compareAndSet(pending, null)
    }

    companion object {
        const val AUTOSAVE_DELAY_MS = 600L
        private const val KEY_DATE = "journalDate"
    }
}
