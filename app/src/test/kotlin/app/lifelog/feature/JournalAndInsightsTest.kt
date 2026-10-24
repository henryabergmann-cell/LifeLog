package app.lifelog.feature

import androidx.lifecycle.SavedStateHandle
import app.lifelog.MainDispatcherRule
import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import app.lifelog.domain.model.Mood
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.usecase.GetOccurrencesUseCase
import app.lifelog.domain.usecase.SaveJournalEntryUseCase
import app.lifelog.fakes.FakeCategoryRepository
import app.lifelog.fakes.FakeEventRepository
import app.lifelog.fakes.FakeJournalRepository
import app.lifelog.fakes.FakePremiumManager
import app.lifelog.fakes.FakeSettingsRepository
import app.lifelog.fakes.FakeTimeSource
import app.lifelog.feature.insights.InsightsRange
import app.lifelog.feature.insights.InsightsViewModel
import app.lifelog.feature.journal.JournalViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

private fun TestScope.keepActive(flow: Flow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
}

class JournalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val journal = FakeJournalRepository()
    private val premium = FakePremiumManager(premium = true)
    private val time = FakeTimeSource() // 2026-10-07
    private val today = LocalDate.of(2026, 10, 7)

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = JournalViewModel(
        savedStateHandle = handle,
        repository = journal,
        saveEntry = SaveJournalEntryUseCase(journal, FeatureGate(premium)),
        premiumManager = premium,
        timeSource = time,
    )

    @Test
    fun `typing is saved automatically after a short pause`() = runTest {
        val vm = viewModel()
        keepActive(vm.uiState)

        vm.onMoodSelected(Mood.GOOD)
        vm.onNoteChange("Nice day")
        assertEquals("Nice day", vm.noteText) // the text field sees the change immediately
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        assertEquals(0, journal.saveCount)

        advanceTimeBy(JournalViewModel.AUTOSAVE_DELAY_MS + 1)

        assertEquals(1, journal.saveCount) // one save for both changes
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        assertEquals(Mood.GOOD, vm.uiState.value.entries.single().mood)
        assertEquals("Nice day", vm.uiState.value.note)
    }

    @Test
    fun `switching days saves pending edits first`() = runTest {
        val vm = viewModel()
        keepActive(vm.uiState)

        vm.onNoteChange("Draft")
        vm.onPreviousDay()

        assertEquals(today.minusDays(1), vm.uiState.value.selectedDate)
        assertEquals("Draft", vm.uiState.value.entries.single { it.date == today }.note)
        assertEquals("", vm.uiState.value.note)
        assertEquals("", vm.noteText)

        vm.onNextDay()
        assertEquals("Draft", vm.noteText)
    }

    @Test
    fun `tapping the selected mood again clears it and an empty entry is removed`() = runTest {
        val vm = viewModel()
        keepActive(vm.uiState)
        vm.onMoodSelected(Mood.BAD)
        advanceTimeBy(JournalViewModel.AUTOSAVE_DELAY_MS + 1)

        vm.onMoodSelected(Mood.BAD)
        advanceTimeBy(JournalViewModel.AUTOSAVE_DELAY_MS + 1)

        assertNull(vm.uiState.value.mood)
        assertTrue(vm.uiState.value.entries.isEmpty())
    }

    @Test
    fun `the future can't be selected`() = runTest {
        val vm = viewModel()
        keepActive(vm.uiState)

        vm.onNextDay()

        assertEquals(today, vm.uiState.value.selectedDate)
        assertFalse(vm.uiState.value.canGoForward)
    }

    @Test
    fun `without Premium the journal is read-only`() = runTest {
        premium.isPremium.value = false
        val vm = viewModel()
        keepActive(vm.uiState)

        vm.onNoteChange("Should not stick")
        advanceTimeBy(JournalViewModel.AUTOSAVE_DELAY_MS + 1)

        assertTrue(vm.uiState.value.isReadOnly)
        assertEquals(0, journal.saveCount)
    }
}

class InsightsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ranges cover whole weeks or months up to today`() {
        val today = LocalDate.of(2026, 10, 7) // Wednesday

        assertEquals(
            LocalDate.of(2026, 9, 14) to LocalDate.of(2026, 10, 8),
            InsightsViewModel.dates(InsightsRange.LAST_4_WEEKS, today, DayOfWeek.MONDAY),
        )
        assertEquals(
            LocalDate.of(2025, 11, 1) to LocalDate.of(2026, 10, 8),
            InsightsViewModel.dates(InsightsRange.LAST_12_MONTHS, today, DayOfWeek.MONDAY),
        )
    }

    @Test
    fun `free users get no statistics, Premium users do`() = runTest {
        val events = FakeEventRepository()
        events.save(Event(title = "Gym", timing = EventTiming.AllDay(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6))))
        val premium = FakePremiumManager(premium = false)
        val vm = InsightsViewModel(
            SavedStateHandle(),
            GetOccurrencesUseCase(events),
            FakeCategoryRepository(),
            FakeSettingsRepository(),
            premium,
            FakeTimeSource(),
        )
        keepActive(vm.uiState)

        assertFalse(vm.uiState.value.isPremium)
        assertNull(vm.uiState.value.statistics)

        premium.isPremium.value = true

        assertEquals(1, vm.uiState.value.statistics?.appointmentCount)
    }
}
