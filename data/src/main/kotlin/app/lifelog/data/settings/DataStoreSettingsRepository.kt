package app.lifelog.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lifelog.data.di.SettingsPreferences
import app.lifelog.domain.model.AppSettings
import app.lifelog.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

internal class DataStoreSettingsRepository @Inject constructor(
    @param:SettingsPreferences private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toAppSettings())) }
    }
}

internal object SettingsKeys {
    val themeMode = stringPreferencesKey("theme_mode")
    val dynamicColor = booleanPreferencesKey("dynamic_color")
    val firstDayOfWeek = stringPreferencesKey("first_day_of_week")
    val defaultReminderMinutes = intPreferencesKey("default_reminder_minutes")
    val allDayReminderMinuteOfDay = intPreferencesKey("all_day_reminder_minute_of_day")
    val defaultEventDurationMinutes = intPreferencesKey("default_event_duration_minutes")
    val lastCalendarView = stringPreferencesKey("last_calendar_view")
    val onboardingDone = booleanPreferencesKey("onboarding_done")
    val exactAlarmExplained = booleanPreferencesKey("exact_alarm_explained")
    val upgradeBannerDismissedUntil = longPreferencesKey("upgrade_banner_dismissed_until")

    /** Stored in [defaultReminderMinutes] when the user chose "no reminder". */
    const val NO_REMINDER = -1
}

private fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    val reminder = this[SettingsKeys.defaultReminderMinutes]
    return AppSettings(
        themeMode = enumOrDefault(this[SettingsKeys.themeMode], defaults.themeMode),
        dynamicColor = this[SettingsKeys.dynamicColor] ?: defaults.dynamicColor,
        firstDayOfWeek = enumOrDefault(this[SettingsKeys.firstDayOfWeek], defaults.firstDayOfWeek),
        defaultReminderMinutes = when (reminder) {
            null -> defaults.defaultReminderMinutes
            SettingsKeys.NO_REMINDER -> null
            else -> reminder
        },
        allDayReminderTime = this[SettingsKeys.allDayReminderMinuteOfDay]
            ?.takeIf { it in 0 until MINUTES_PER_DAY }
            ?.let { LocalTime.of(it / 60, it % 60) }
            ?: defaults.allDayReminderTime,
        defaultEventDurationMinutes = this[SettingsKeys.defaultEventDurationMinutes]
            ?.takeIf { it > 0 }
            ?: defaults.defaultEventDurationMinutes,
        lastCalendarView = enumOrDefault(this[SettingsKeys.lastCalendarView], defaults.lastCalendarView),
        onboardingDone = this[SettingsKeys.onboardingDone] ?: defaults.onboardingDone,
        exactAlarmExplained = this[SettingsKeys.exactAlarmExplained] ?: defaults.exactAlarmExplained,
        upgradeBannerDismissedUntil = this[SettingsKeys.upgradeBannerDismissedUntil]?.let(Instant::ofEpochMilli),
    )
}

private fun MutablePreferences.write(settings: AppSettings) {
    this[SettingsKeys.themeMode] = settings.themeMode.name
    this[SettingsKeys.dynamicColor] = settings.dynamicColor
    this[SettingsKeys.firstDayOfWeek] = settings.firstDayOfWeek.name
    this[SettingsKeys.defaultReminderMinutes] = settings.defaultReminderMinutes ?: SettingsKeys.NO_REMINDER
    this[SettingsKeys.allDayReminderMinuteOfDay] =
        settings.allDayReminderTime.hour * 60 + settings.allDayReminderTime.minute
    this[SettingsKeys.defaultEventDurationMinutes] = settings.defaultEventDurationMinutes
    this[SettingsKeys.lastCalendarView] = settings.lastCalendarView.name
    this[SettingsKeys.onboardingDone] = settings.onboardingDone
    this[SettingsKeys.exactAlarmExplained] = settings.exactAlarmExplained
    val dismissedUntil = settings.upgradeBannerDismissedUntil
    if (dismissedUntil == null) {
        remove(SettingsKeys.upgradeBannerDismissedUntil)
    } else {
        this[SettingsKeys.upgradeBannerDismissedUntil] = dismissedUntil.toEpochMilli()
    }
}

private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
    name?.let { stored -> enumValues<E>().firstOrNull { it.name == stored } } ?: default

private const val MINUTES_PER_DAY = 24 * 60
