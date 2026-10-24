package app.lifelog.domain.usecase

import app.lifelog.domain.model.Event
import app.lifelog.domain.model.EventTiming
import app.lifelog.domain.repository.EventRepository
import javax.inject.Inject

class SaveEventUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(event: Event): SaveEventResult {
        validate(event)?.let { return SaveEventResult.Invalid(it) }
        // Reminder counts are limited in the editor, not here: after Premium lapses, saving an
        // older event must not silently drop its extra reminders (PLAN.md D2).
        val cleaned = event.copy(
            title = event.title.trim(),
            location = event.location?.trim()?.takeIf { it.isNotEmpty() },
            notes = event.notes?.trim()?.takeIf { it.isNotEmpty() },
            reminderMinutes = event.reminderMinutes.distinct().sorted(),
        )
        return SaveEventResult.Saved(repository.save(cleaned))
    }

    companion object {
        fun validate(event: Event): EventValidationError? = when {
            event.title.isBlank() -> EventValidationError.TITLE_MISSING
            !event.timing.endsAfterStart() -> EventValidationError.END_BEFORE_START
            event.reminderMinutes.any { it < 0 } -> EventValidationError.INVALID_REMINDER
            else -> null
        }

        private fun EventTiming.endsAfterStart(): Boolean = when (this) {
            is EventTiming.AllDay -> !endDate.isBefore(startDate)
            is EventTiming.Timed -> end.isAfter(start)
        }
    }
}

sealed interface SaveEventResult {
    data class Saved(val eventId: Long) : SaveEventResult
    data class Invalid(val error: EventValidationError) : SaveEventResult
}

enum class EventValidationError { TITLE_MISSING, END_BEFORE_START, INVALID_REMINDER }
