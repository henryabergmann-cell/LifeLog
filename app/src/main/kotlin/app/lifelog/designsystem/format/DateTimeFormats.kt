package app.lifelog.designsystem.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.lifelog.domain.model.Occurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Locale-aware formatters. Patterns come from Android's skeleton API, so field order and
 * separators follow the user's language (e.g. "Tue, Oct 6" vs. "Di., 6. Okt.").
 */
class DateTimeFormats(val locale: Locale, is24Hour: Boolean) {
    val time: DateTimeFormatter = skeleton(if (is24Hour) "Hm" else "hm")

    /** Compact full-hour label for the time grid: "13:00" or "1 PM". */
    val hour: DateTimeFormatter = skeleton(if (is24Hour) "Hm" else "ha")
    val monthYear: DateTimeFormatter = skeleton("MMMMy")
    val weekdayDayMonth: DateTimeFormatter = skeleton("EEEEdMMMM")
    val shortWeekdayDayMonth: DateTimeFormatter = skeleton("EEEdMMM")
    val dayMonth: DateTimeFormatter = skeleton("dMMM")
    val shortWeekday: DateTimeFormatter = skeleton("EEE")
    val shortMonth: DateTimeFormatter = skeleton("MMM")
    val fullDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    fun time(value: LocalTime): String = time.format(value)

    /**
     * "09:00 – 10:30" for single-day timed occurrences, "6 Oct 22:00 – 7 Oct 01:00" across
     * midnight, null for all-day occurrences (callers show "All day" or the date range).
     */
    fun timeRange(occurrence: Occurrence): String? {
        if (occurrence.isAllDay) return null
        val start = occurrence.start
        val end = occurrence.end
        return if (start.toLocalDate() == end.toLocalDate()) {
            "${time.format(start)} – ${time.format(end)}"
        } else {
            "${dateTime(start)} – ${dateTime(end)}"
        }
    }

    /** Date range of an all-day occurrence, e.g. "6 Oct – 8 Oct"; null for a single day. */
    fun allDayRange(occurrence: Occurrence): String? {
        val first = occurrence.start.toLocalDate()
        val last = occurrence.end.toLocalDate().minusDays(1)
        return if (last.isAfter(first)) "${dayMonth.format(first)} – ${dayMonth.format(last)}" else null
    }

    fun date(value: LocalDate): String = mediumDate.format(value)

    private fun dateTime(value: LocalDateTime): String = "${dayMonth.format(value)} ${time.format(value)}"

    private fun skeleton(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
fun rememberDateTimeFormats(): DateTimeFormats {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24Hour) { DateTimeFormats(locale, is24Hour) }
}
