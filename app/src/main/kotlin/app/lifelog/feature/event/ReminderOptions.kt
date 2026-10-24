package app.lifelog.feature.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.lifelog.R

/** Reminder offsets offered in the editor, in minutes before the start. */
val REMINDER_OPTIONS: List<Int> = listOf(0, 5, 10, 15, 30, 60, 120, 24 * 60, 2 * 24 * 60, 7 * 24 * 60)

/**
 * Human-readable reminder offset. For all-day events offsets count back from the all-day
 * reminder time set in Settings, so "0" reads as "on the day".
 */
@Composable
fun reminderLabel(minutesBefore: Int, allDay: Boolean): String = when {
    minutesBefore == 0 -> stringResource(if (allDay) R.string.reminder_on_the_day else R.string.reminder_at_start)
    minutesBefore % (24 * 60) == 0 -> (minutesBefore / (24 * 60)).let {
        pluralStringResource(R.plurals.reminder_days_before, it, it)
    }
    minutesBefore % 60 == 0 -> (minutesBefore / 60).let { pluralStringResource(R.plurals.reminder_hours_before, it, it) }
    else -> pluralStringResource(R.plurals.reminder_minutes_before, minutesBefore, minutesBefore)
}
