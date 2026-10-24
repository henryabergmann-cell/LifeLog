package app.lifelog.domain.usecase

import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.premium.PremiumManager
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.EventRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SaveEventUseCaseTest {

    private val repository = mockk<EventRepository>()
    private val useCase = SaveEventUseCase(repository)
    private val day = LocalDate.of(2026, 10, 6)

    private fun event(
        title: String = "Dentist",
        start: LocalDateTime = day.atTime(9, 0),
        end: LocalDateTime = day.atTime(10, 0),
        reminders: List<Int> = emptyList(),
    ) = Event(title = title, timing = EventTiming.Timed(start, end, ZoneId.of("UTC")), reminderMinutes = reminders)

    @Test
    fun `rejects a blank title`() = runTest {
        assertEquals(SaveEventResult.Invalid(EventValidationError.TITLE_MISSING), useCase(event(title = "  ")))
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `rejects an end that is not after the start`() = runTest {
        val result = useCase(event(end = day.atTime(9, 0)))
        assertEquals(SaveEventResult.Invalid(EventValidationError.END_BEFORE_START), result)
    }

    @Test
    fun `all-day events may start and end on the same day`() {
        val sameDay = Event(title = "Holiday", timing = EventTiming.AllDay(day, day))
        assertNull(SaveEventUseCase.validate(sameDay))
        val reversed = Event(title = "Holiday", timing = EventTiming.AllDay(day, day.minusDays(1)))
        assertEquals(EventValidationError.END_BEFORE_START, SaveEventUseCase.validate(reversed))
    }

    @Test
    fun `trims text, drops empty optional fields and normalizes reminders`() = runTest {
        val saved = slot<Event>()
        coEvery { repository.save(capture(saved)) } returns 7L

        val result = useCase(event(title = " Dentist ", reminders = listOf(30, 10, 30)).copy(location = " ", notes = " Bring card "))

        assertEquals(SaveEventResult.Saved(7L), result)
        assertEquals("Dentist", saved.captured.title)
        assertNull(saved.captured.location)
        assertEquals("Bring card", saved.captured.notes)
        assertEquals(listOf(10, 30), saved.captured.reminderMinutes)
    }
}

class SaveCategoryUseCaseTest {

    private val repository = mockk<CategoryRepository>(relaxed = true)
    private val premium = MutableStateFlow(false)
    private val useCase = SaveCategoryUseCase(
        repository,
        FeatureGate(object : PremiumManager { override val isPremium = premium }),
    )

    @Test
    fun `free tier is limited to three new categories`() = runTest {
        coEvery { repository.count() } returns 3

        assertEquals(SaveCategoryResult.LimitReached(3), useCase(Category(name = "Fourth", colorArgb = 0)))
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `premium has no category limit`() = runTest {
        premium.value = true
        coEvery { repository.count() } returns 50
        coEvery { repository.save(any()) } returns 51L

        assertEquals(SaveCategoryResult.Saved(51L), useCase(Category(name = "More", colorArgb = 0)))
    }

    @Test
    fun `editing an existing category is allowed at the limit`() = runTest {
        coEvery { repository.count() } returns 5
        coEvery { repository.save(any()) } returns 2L

        assertEquals(SaveCategoryResult.Saved(2L), useCase(Category(id = 2, name = " Work ", colorArgb = 0)))
        coVerify { repository.save(Category(id = 2, name = "Work", colorArgb = 0)) }
    }

    @Test
    fun `rejects a blank name`() = runTest {
        assertEquals(SaveCategoryResult.NameMissing, useCase(Category(name = " ", colorArgb = 0)))
    }
}

class EventDeletionTest {

    private val repository = mockk<EventRepository>(relaxed = true)
    private val deletion = EventDeletion(repository)
    private val event = Event(id = 4, title = "Gym", timing = EventTiming.AllDay(LocalDate.MIN, LocalDate.MIN))

    @Test
    fun `delete soft-deletes and offers undo`() = runTest {
        deletion.delete(event)

        coVerify { repository.softDelete(4) }
        assertEquals(DeletedEvent(4, "Gym"), deletion.lastDeleted.value)
    }

    @Test
    fun `undo restores and clears the pending deletion`() = runTest {
        deletion.delete(event)

        deletion.undo(DeletedEvent(4, "Gym"))

        coVerify { repository.restore(4) }
        assertNull(deletion.lastDeleted.value)
    }

    @Test
    fun `dismissing a stale deletion keeps a newer one`() = runTest {
        deletion.delete(event)
        deletion.delete(event.copy(id = 5, title = "Run"))

        deletion.dismiss(DeletedEvent(4, "Gym"))

        assertEquals(DeletedEvent(5, "Run"), deletion.lastDeleted.value)
    }
}

class GetOccurrencesUseCaseTest {

    @Test
    fun `queries a padded range and filters precisely`() = runTest {
        val zone = ZoneId.of("Europe/Berlin")
        val day = LocalDate.of(2026, 10, 6)
        val repository = mockk<EventRepository>()
        val inside = Event(id = 1, title = "In", timing = EventTiming.AllDay(day, day))
        val outside = Event(id = 2, title = "Out", timing = EventTiming.AllDay(day.minusDays(1), day.minusDays(1)))
        coEvery { repository.observeCandidates(any(), any()) } returns flowOf(listOf(inside, outside))

        val result = mutableListOf<Long>()
        GetOccurrencesUseCase(repository)(day, day.plusDays(1), zone).collect { list -> result += list.map { it.event.id } }

        assertEquals(listOf(1L), result)
        coVerify {
            repository.observeCandidates(
                day.minusDays(1).atStartOfDay(zone).toInstant(),
                day.plusDays(2).atStartOfDay(zone).toInstant(),
            )
        }
    }
}
