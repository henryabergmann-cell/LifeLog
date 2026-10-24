package app.lifelog.domain.usecase

import app.lifelog.domain.model.JournalEntry
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.repository.JournalRepository
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val repository: JournalRepository,
    private val featureGate: FeatureGate,
) {
    /**
     * Saves the entry, or deletes it when mood and note are both empty. Writing is Premium-only;
     * after Premium lapses the journal stays readable (PLAN.md D2).
     */
    suspend operator fun invoke(entry: JournalEntry): SaveJournalResult {
        if (!featureGate.currentLimits().isPremium) return SaveJournalResult.PremiumRequired
        if (entry.isEmpty) {
            repository.delete(entry.date)
            return SaveJournalResult.Deleted
        }
        repository.save(entry.copy(note = entry.note.trim().take(JournalEntry.MAX_NOTE_LENGTH)))
        return SaveJournalResult.Saved
    }
}

enum class SaveJournalResult { Saved, Deleted, PremiumRequired }
