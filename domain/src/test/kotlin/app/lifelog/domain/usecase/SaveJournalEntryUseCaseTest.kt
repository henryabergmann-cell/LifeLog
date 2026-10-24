package app.lifelog.domain.usecase

import app.lifelog.domain.model.JournalEntry
import app.lifelog.domain.model.Mood
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.premium.PremiumManager
import app.lifelog.domain.repository.JournalRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SaveJournalEntryUseCaseTest {

    private val repository = mockk<JournalRepository>(relaxed = true)
    private val premium = MutableStateFlow(true)
    private val useCase = SaveJournalEntryUseCase(
        repository,
        FeatureGate(object : PremiumManager { override val isPremium = premium }),
    )
    private val day = LocalDate.of(2026, 10, 23)

    @Test
    fun `saves a trimmed note capped at the maximum length`() = runTest {
        val long = " " + "x".repeat(JournalEntry.MAX_NOTE_LENGTH + 50)

        assertEquals(SaveJournalResult.Saved, useCase(JournalEntry(day, Mood.GOOD, long)))

        coVerify { repository.save(JournalEntry(day, Mood.GOOD, "x".repeat(JournalEntry.MAX_NOTE_LENGTH))) }
    }

    @Test
    fun `an entry without mood and note is deleted`() = runTest {
        assertEquals(SaveJournalResult.Deleted, useCase(JournalEntry(day, mood = null, note = "  ")))

        coVerify { repository.delete(day) }
    }

    @Test
    fun `writing needs Premium`() = runTest {
        premium.value = false

        assertEquals(SaveJournalResult.PremiumRequired, useCase(JournalEntry(day, Mood.GREAT)))

        coVerify(exactly = 0) { repository.save(any()) }
    }
}
