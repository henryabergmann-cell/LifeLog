package app.lifelog.feature.journal

import app.lifelog.R
import app.lifelog.domain.model.Mood

/** Emoji shown for a mood. Always paired with the text label, so mood never relies on the image alone. */
val Mood.emoji: String
    get() = when (this) {
        Mood.AWFUL -> "😫" // 😫
        Mood.BAD -> "🙁" // 🙁
        Mood.OKAY -> "😐" // 😐
        Mood.GOOD -> "🙂" // 🙂
        Mood.GREAT -> "😄" // 😄
    }

val Mood.label: Int
    get() = when (this) {
        Mood.AWFUL -> R.string.mood_awful
        Mood.BAD -> R.string.mood_bad
        Mood.OKAY -> R.string.mood_okay
        Mood.GOOD -> R.string.mood_good
        Mood.GREAT -> R.string.mood_great
    }
