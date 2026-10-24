package app.lifelog.domain.model

import java.time.Instant
import java.time.LocalDate

/** Premium: a short note and mood for one day. */
data class JournalEntry(
    val date: LocalDate,
    val mood: Mood? = null,
    val note: String = "",
    val updatedAt: Instant? = null,
) {
    val isEmpty: Boolean get() = mood == null && note.isBlank()

    companion object {
        const val MAX_NOTE_LENGTH = 1_000
    }
}

/** Five-step mood scale; [score] is what gets stored. */
enum class Mood(val score: Int) {
    AWFUL(1),
    BAD(2),
    OKAY(3),
    GOOD(4),
    GREAT(5);

    companion object {
        fun fromScore(score: Int?): Mood? = entries.firstOrNull { it.score == score }
    }
}
