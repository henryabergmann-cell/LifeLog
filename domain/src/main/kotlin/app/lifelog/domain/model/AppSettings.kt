package app.lifelog.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale

/** User preferences. Defaults apply until the user changes a value. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: FirstDayOfWeekSetting = FirstDayOfWeekSetting.SYSTEM,
    /** Offset of the default reminder for new events; null means "no reminder". */
    val defaultReminderMinutes: Int? = DEFAULT_REMINDER_MINUTES,
    /** Time of day that reminder offsets of all-day events are measured from. */
    val allDayReminderTime: LocalTime = DEFAULT_ALL_DAY_REMINDER_TIME,
    val defaultEventDurationMinutes: Int = DEFAULT_EVENT_DURATION_MINUTES,
    val lastCalendarView: CalendarViewMode = CalendarViewMode.MONTH,
    val onboardingDone: Boolean = false,
    val exactAlarmExplained: Boolean = false,
    val upgradeBannerDismissedUntil: Instant? = null,
) {
    companion object {
        const val DEFAULT_REMINDER_MINUTES = 15
        const val DEFAULT_EVENT_DURATION_MINUTES = 60
        val DEFAULT_ALL_DAY_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class FirstDayOfWeekSetting {
    SYSTEM,
    MONDAY,
    SATURDAY,
    SUNDAY;

    fun resolve(locale: Locale): DayOfWeek = when (this) {
        SYSTEM -> WeekFields.of(locale).firstDayOfWeek
        MONDAY -> DayOfWeek.MONDAY
        SATURDAY -> DayOfWeek.SATURDAY
        SUNDAY -> DayOfWeek.SUNDAY
    }
}

enum class CalendarViewMode { MONTH, WEEK, DAY, AGENDA }
