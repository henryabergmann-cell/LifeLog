package app.lifelog.feature

import androidx.lifecycle.SavedStateHandle
import app.lifelog.MainDispatcherRule
import app.lifelog.domain.model.AppSettings
import app.lifelog.domain.model.CalendarViewMode
import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.usecase.EventValidationError
import app.lifelog.domain.usecase.GetOccurrencesUseCase
import app.lifelog.domain.usecase.SaveCategoryUseCase
import app.lifelog.domain.usecase.SaveEventUseCase
import app.lifelog.fakes.FakeCategoryRepository
import app.lifelog.fakes.FakeEventRepository
import app.lifelog.fakes.FakePremiumManager
import app.lifelog.fakes.FakeSettingsRepository
import app.lifelog.fakes.FakeTimeSource
import app.lifelog.feature.calendar.CalendarUiState
import app.lifelog.feature.calendar.CalendarViewModel
import app.lifelog.feature.categories.CategoriesViewModel
import app.lifelog.feature.event.editor.EventEditorViewModel
import app.lifelog.feature.search.SearchResults
import app.lifelog.feature.search.SearchViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Keeps a WhileSubscribed state flow active (and eagerly updated) for the rest of the test. */
private fun TestScope.keepActive(flow: Flow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
}

private val TODAY: LocalDate = LocalDate.of(2026, 10, 7)

class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val events = FakeEventRepository()
    private val categories = FakeCategoryRepository()
    private val time = FakeTimeSource()

    private val premium = FakePremiumManager()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        CalendarViewModel(handle, settings, GetOccurrencesUseCase(events), categories, premium, time)

    @Test
    fun `free users see the upgrade hint until they dismiss it, Premium users never`() = runTest {
        val vm = viewModel()
        assertTrue(ready(vm).showUpgradeBanner)

        vm.onDismissUpgradeBanner()
        assertEquals(false, (vm.uiState.value as CalendarUiState.Ready).showUpgradeBanner)

        settings.update { it.copy(upgradeBannerDismissedUntil = null) }
        premium.isPremium.value = true
        assertEquals(false, (vm.uiState.value as CalendarUiState.Ready).showUpgradeBanner)
    }

    private fun TestScope.ready(vm: CalendarViewModel): CalendarUiState.Ready {
        keepActive(vm.uiState)
        return vm.uiState.value as CalendarUiState.Ready
    }

    @Test
    fun `starts on today in the saved view with occurrences of the loaded window`() = runTest {
        events.save(Event(title = "Dentist", timing = EventTiming.AllDay(TODAY.plusDays(3), TODAY.plusDays(3))))

        val state = ready(viewModel())

        assertEquals(CalendarViewMode.MONTH, state.viewMode)
        assertEquals(TODAY, state.selectedDate)
        assertEquals(listOf("Dentist"), state.occurrences.map { it.event.title })
    }

    @Test
    fun `switching the view is persisted`() = runTest {
        val vm = viewModel()
        ready(vm)

        vm.onViewModeSelected(CalendarViewMode.WEEK)

        assertEquals(CalendarViewMode.WEEK, settings.current.lastCalendarView)
        assertEquals(CalendarViewMode.WEEK, (vm.uiState.value as CalendarUiState.Ready).viewMode)
    }

    @Test
    fun `settling on another month selects its first day, the current month selects today`() = runTest {
        val vm = viewModel()
        ready(vm)

        vm.onPeriodSettled(LocalDate.of(2026, 11, 1))
        assertEquals(LocalDate.of(2026, 11, 1), (vm.uiState.value as CalendarUiState.Ready).selectedDate)

        vm.onPrevious()
        assertEquals(TODAY, (vm.uiState.value as CalendarUiState.Ready).selectedDate)
    }

    @Test
    fun `tapping a day opens it in the day view`() = runTest {
        val vm = viewModel()
        ready(vm)

        vm.onDaySelected(TODAY.plusDays(5))

        val state = vm.uiState.value as CalendarUiState.Ready
        assertEquals(CalendarViewMode.DAY, state.viewMode)
        assertEquals(TODAY.plusDays(5), state.selectedDate)
    }

    @Test
    fun `selected date survives process death`() = runTest {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        ready(first)
        first.onPeriodSettled(LocalDate.of(2027, 1, 1))

        val restored = viewModel(handle)

        assertEquals(LocalDate.of(2027, 1, 1), ready(restored).selectedDate)
    }
}

class EventEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val events = FakeEventRepository()
    private val settings = FakeSettingsRepository(AppSettings(defaultEventDurationMinutes = 90, defaultReminderMinutes = 30))
    private val time = FakeTimeSource()

    private fun viewModel(handle: SavedStateHandle) = EventEditorViewModel(
        savedStateHandle = handle,
        repository = events,
        saveEvent = SaveEventUseCase(events),
        settingsRepository = settings,
        timeSource = time,
        categoryRepository = FakeCategoryRepository(),
        featureGate = FeatureGate(FakePremiumManager()),
    )

    // Explicit value type: otherwise the Int literal below would be inferred as Long.
    private fun newEventHandle() = SavedStateHandle(
        mapOf<String, Any?>(
            EventEditorViewModel.ARG_DATE_EPOCH_DAY to LocalDate.of(2026, 10, 10).toEpochDay(),
            EventEditorViewModel.ARG_START_MINUTE to 600,
        ),
    )

    @Test
    fun `a new event starts at the tapped slot with the default length and reminder`() = runTest {
        val vm = viewModel(newEventHandle())
        keepActive(vm.uiState)

        val draft = checkNotNull(vm.uiState.value.draft)
        assertEquals(LocalDate.of(2026, 10, 10).atTime(10, 0), draft.start)
        assertEquals(LocalDate.of(2026, 10, 10).atTime(11, 30), draft.end)
        assertEquals(listOf(30), draft.reminderMinutes)
        assertEquals("Z", draft.zoneId)
        assertTrue(vm.uiState.value.isNew)
    }

    @Test
    fun `saving without a title shows an error and saves nothing`() = runTest {
        val vm = viewModel(newEventHandle())
        keepActive(vm.uiState)

        vm.onSave()

        assertEquals(EventValidationError.TITLE_MISSING, vm.uiState.value.validationError)
        assertTrue(events.all.isEmpty())
    }

    @Test
    fun `moving the start keeps the duration`() = runTest {
        val vm = viewModel(newEventHandle())
        keepActive(vm.uiState)

        vm.onStartTimeChange(LocalTime.of(14, 0))

        val draft = checkNotNull(vm.uiState.value.draft)
        assertEquals(LocalTime.of(15, 30), draft.end.toLocalTime())
    }

    @Test
    fun `saving a valid event stores it and closes the editor`() = runTest {
        val vm = viewModel(newEventHandle())
        keepActive(vm.uiState)

        vm.onTitleChange("  Dentist ")
        vm.onSave()

        val saved = events.all.single()
        assertEquals("Dentist", saved.title)
        assertEquals(
            EventTiming.Timed(LocalDate.of(2026, 10, 10).atTime(10, 0), LocalDate.of(2026, 10, 10).atTime(11, 30), ZoneOffset.UTC),
            saved.timing,
        )
        assertEquals(saved.id, vm.uiState.value.savedEventId)
    }

    @Test
    fun `duplicating creates a new event from a copy`() = runTest {
        val id = events.save(Event(title = "Yoga", timing = EventTiming.AllDay(TODAY, TODAY), reminderMinutes = listOf(60)))
        val vm = viewModel(SavedStateHandle(mapOf(EventEditorViewModel.ARG_DUPLICATE_OF_ID to id)))
        keepActive(vm.uiState)

        assertEquals(0L, vm.uiState.value.draft?.eventId)
        assertTrue(vm.uiState.value.isDirty)
        vm.onSave()

        assertEquals(2, events.all.size)
        assertEquals(listOf("Yoga", "Yoga"), events.all.map { it.title })
    }

    @Test
    fun `an unsaved draft survives process death`() = runTest {
        val handle = newEventHandle()
        val first = viewModel(handle)
        keepActive(first.uiState)
        first.onTitleChange("Half-typed")

        val restored = viewModel(handle)
        keepActive(restored.uiState)

        assertEquals("Half-typed", restored.uiState.value.draft?.title)
        assertTrue(restored.uiState.value.isDirty)
    }

    @Test
    fun `editing a missing event reports not found`() = runTest {
        val vm = viewModel(SavedStateHandle(mapOf(EventEditorViewModel.ARG_EVENT_ID to 99L)))
        keepActive(vm.uiState)

        assertTrue(vm.uiState.value.notFound)
        assertNull(vm.uiState.value.draft)
    }
}

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `finds events after the debounce and resets on blank input`() = runTest {
        val events = FakeEventRepository()
        events.save(Event(title = "Dentist", timing = EventTiming.AllDay(TODAY, TODAY)))
        events.save(Event(title = "Gym", timing = EventTiming.AllDay(TODAY, TODAY)))
        val vm = SearchViewModel(SavedStateHandle(), events, FakeCategoryRepository(), FakeTimeSource())
        keepActive(vm.uiState)

        vm.onQueryChange("dent")
        advanceTimeBy(300)

        val found = vm.uiState.value.results as SearchResults.Found
        assertEquals(listOf("Dentist"), found.occurrences.map { it.event.title })

        vm.onQueryChange("  ")
        advanceTimeBy(300)
        assertEquals(SearchResults.Idle, vm.uiState.value.results)
    }
}

class CategoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val three = listOf(
        Category(id = 1, name = "Work", colorArgb = 1),
        Category(id = 2, name = "Family", colorArgb = 2),
        Category(id = 3, name = "Sport", colorArgb = 3),
    )

    private fun viewModel(premium: Boolean, categories: FakeCategoryRepository = FakeCategoryRepository(three)): CategoriesViewModel {
        val gate = FeatureGate(FakePremiumManager(premium))
        return CategoriesViewModel(categories, SaveCategoryUseCase(categories, gate), gate)
    }

    @Test
    fun `free tier explains the limit instead of opening the dialog`() = runTest {
        val vm = viewModel(premium = false)
        keepActive(vm.uiState)

        vm.onAdd()

        assertEquals(3, vm.uiState.value.limitReached)
        assertNull(vm.uiState.value.dialog)
    }

    @Test
    fun `premium can add more categories`() = runTest {
        val repository = FakeCategoryRepository(three)
        val vm = viewModel(premium = true, categories = repository)
        keepActive(vm.uiState)

        vm.onAdd()
        assertNotNull(vm.uiState.value.dialog)
        vm.onNameChange("Travel")
        vm.onSaveDialog()

        assertNull(vm.uiState.value.dialog)
        assertEquals(4, repository.count())
    }

    @Test
    fun `a blank name is rejected in the dialog`() = runTest {
        val vm = viewModel(premium = false, categories = FakeCategoryRepository())
        keepActive(vm.uiState)

        vm.onAdd()
        vm.onSaveDialog()

        assertTrue(vm.uiState.value.dialog?.nameError == true)
    }
}
