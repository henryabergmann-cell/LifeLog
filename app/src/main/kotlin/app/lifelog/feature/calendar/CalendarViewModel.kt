package app.lifelog.feature.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.model.CalendarViewMode
import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Occurrence
import app.lifelog.domain.premium.PremiumManager
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.SettingsRepository
import app.lifelog.domain.time.TimeSource
import app.lifelog.domain.usecase.GetOccurrencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

sealed interface CalendarUiState {
    data object Loading : CalendarUiState

    data class Ready(
        val viewMode: CalendarViewMode,
        val selectedDate: LocalDate,
        val today: LocalDate,
        val firstDayOfWeek: DayOfWeek,
        /** Dates covered by [occurrences]. */
        val loadedRange: DateRange,
        val occurrences: List<Occurrence>,
        val categories: Map<Long, Category>,
        /** Free users only, unless dismissed in the last 14 days. */
        val showUpgradeBanner: Boolean = false,
    ) : CalendarUiState {
        /** Days that have occurrences, for the agenda; multi-day events appear on each day. */
        val agendaDays: List<AgendaDay> by lazy {
            if (viewMode != CalendarViewMode.AGENDA) return@lazy emptyList()
            generateSequence(loadedRange.start) { it.plusDays(1) }
                .takeWhile { it.isBefore(loadedRange.endExclusive) }
                .mapNotNull { date ->
                    occurrences.filter { it.overlaps(date) }.takeIf { it.isNotEmpty() }?.let { AgendaDay(date, it) }
                }
                .toList()
        }
    }
}

data class AgendaDay(val date: LocalDate, val occurrences: List<Occurrence>)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val getOccurrences: GetOccurrencesUseCase,
    categoryRepository: CategoryRepository,
    premiumManager: PremiumManager,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    private val selectedDate = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_SELECTED)?.let(LocalDate::ofEpochDay) ?: today.value,
    )

    private val agendaRange = MutableStateFlow(
        savedStateHandle.get<LongArray>(KEY_AGENDA)?.let { DateRange(LocalDate.ofEpochDay(it[0]), LocalDate.ofEpochDay(it[1])) }
            ?: initialAgendaRange(selectedDate.value),
    )

    private data class Query(
        val mode: CalendarViewMode,
        val firstDay: DayOfWeek,
        val selected: LocalDate,
        val today: LocalDate,
        val range: DateRange,
    )

    private val showUpgradeBanner = combine(premiumManager.isPremium, settingsRepository.settings) { premium, settings ->
        !premium && settings.upgradeBannerDismissedUntil?.isAfter(timeSource.now()) != true
    }.distinctUntilChanged()

    val uiState: StateFlow<CalendarUiState> =
        combine(settingsRepository.settings, selectedDate, agendaRange, today) { settings, selected, agenda, today ->
            val mode = settings.lastCalendarView
            val firstDay = settings.firstDayOfWeek.resolve(Locale.getDefault())
            val range = if (mode == CalendarViewMode.AGENDA) agenda else CalendarPeriods.loadWindow(mode, selected, firstDay)
            Query(mode, firstDay, selected, today, range)
        }
            .distinctUntilChanged()
            .flatMapLatest { query ->
                combine(
                    getOccurrences(query.range.start, query.range.endExclusive, timeSource.zone()),
                    categoryRepository.categories.map { list -> list.associateBy { it.id } },
                ) { occurrences, categories ->
                    CalendarUiState.Ready(
                        viewMode = query.mode,
                        selectedDate = query.selected,
                        today = query.today,
                        firstDayOfWeek = query.firstDay,
                        loadedRange = query.range,
                        occurrences = occurrences,
                        categories = categories,
                    )
                }
            }
            .combine(showUpgradeBanner) { state, banner -> state.copy(showUpgradeBanner = banner) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState.Loading)

    fun onDismissUpgradeBanner() {
        val until = timeSource.now().plus(Duration.ofDays(UPGRADE_BANNER_SNOOZE_DAYS))
        viewModelScope.launch { settingsRepository.update { it.copy(upgradeBannerDismissedUntil = until) } }
    }

    /** The pager settled on the period starting at [periodStart]. */
    fun onPeriodSettled(periodStart: LocalDate) {
        val state = uiState.value as? CalendarUiState.Ready ?: return
        val mode = state.viewMode
        val firstDay = state.firstDayOfWeek
        if (CalendarPeriods.periodStart(mode, selectedDate.value, firstDay) == periodStart) return
        val todayValue = today.value
        select(if (CalendarPeriods.periodStart(mode, todayValue, firstDay) == periodStart) todayValue else periodStart)
    }

    fun onPrevious() = shiftPeriod(-1)

    fun onNext() = shiftPeriod(1)

    fun onToday() {
        refreshToday()
        select(today.value)
        if (today.value !in agendaRange.value) setAgendaRange(initialAgendaRange(today.value))
    }

    fun onDaySelected(date: LocalDate) {
        select(date)
        onViewModeSelected(CalendarViewMode.DAY)
    }

    fun onViewModeSelected(mode: CalendarViewMode) {
        if (mode == CalendarViewMode.AGENDA) setAgendaRange(initialAgendaRange(selectedDate.value))
        viewModelScope.launch { settingsRepository.update { it.copy(lastCalendarView = mode) } }
    }

    fun onAgendaLoadLater() = setAgendaRange(agendaRange.value.let { it.copy(endExclusive = it.endExclusive.plusDays(AGENDA_STEP_DAYS)) })

    fun onAgendaLoadEarlier() = setAgendaRange(agendaRange.value.let { it.copy(start = it.start.minusDays(AGENDA_STEP_DAYS)) })

    /** Call on resume: the date may have changed while the app was in the background. */
    fun refreshToday() {
        today.value = timeSource.today()
    }

    private fun shiftPeriod(periods: Long) {
        val state = uiState.value as? CalendarUiState.Ready ?: return
        val start = CalendarPeriods.periodStart(state.viewMode, selectedDate.value, state.firstDayOfWeek)
        onPeriodSettled(CalendarPeriods.shift(state.viewMode, start, periods))
    }

    private fun select(date: LocalDate) {
        selectedDate.value = date
        savedStateHandle[KEY_SELECTED] = date.toEpochDay()
    }

    private fun setAgendaRange(range: DateRange) {
        agendaRange.update { range }
        savedStateHandle[KEY_AGENDA] = longArrayOf(range.start.toEpochDay(), range.endExclusive.toEpochDay())
    }

    private fun initialAgendaRange(anchor: LocalDate) =
        DateRange(anchor.minusDays(AGENDA_BACK_DAYS), anchor.plusDays(AGENDA_STEP_DAYS))

    private companion object {
        const val KEY_SELECTED = "selectedDate"
        const val KEY_AGENDA = "agendaRange"
        const val AGENDA_BACK_DAYS = 7L
        const val AGENDA_STEP_DAYS = 90L
        const val UPGRADE_BANNER_SNOOZE_DAYS = 14L
    }
}
