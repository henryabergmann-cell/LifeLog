package app.lifelog.data.settings

import androidx.datastore.preferences.core.edit
import app.lifelog.domain.model.AppSettings
import app.lifelog.domain.model.CalendarViewMode
import app.lifelog.domain.model.FirstDayOfWeekSetting
import app.lifelog.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class DataStoreSettingsRepositoryTest {

    private fun newDataStore() = InMemoryPreferencesDataStore()

    @Test
    fun `empty store yields defaults`() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())

        assertEquals(AppSettings(), repository.settings.first())
    }

    @Test
    fun `update persists every field`() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())
        val changed = AppSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            firstDayOfWeek = FirstDayOfWeekSetting.SUNDAY,
            defaultReminderMinutes = 30,
            allDayReminderTime = LocalTime.of(7, 45),
            defaultEventDurationMinutes = 90,
            lastCalendarView = CalendarViewMode.AGENDA,
            onboardingDone = true,
            exactAlarmExplained = true,
            upgradeBannerDismissedUntil = Instant.ofEpochMilli(1_800_000_000_000),
        )

        repository.update { changed }

        assertEquals(changed, repository.settings.first())
    }

    @Test
    fun `no default reminder survives a round trip`() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())

        repository.update { it.copy(defaultReminderMinutes = null) }

        assertNull(repository.settings.first().defaultReminderMinutes)
    }

    @Test
    fun `clearing the banner dismissal removes the stored value`() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())
        repository.update { it.copy(upgradeBannerDismissedUntil = Instant.ofEpochMilli(42)) }

        repository.update { it.copy(upgradeBannerDismissedUntil = null) }

        assertNull(repository.settings.first().upgradeBannerDismissedUntil)
    }

    @Test
    fun `unknown or invalid stored values fall back to defaults`() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[SettingsKeys.themeMode] = "SEPIA"
            it[SettingsKeys.allDayReminderMinuteOfDay] = 5_000
            it[SettingsKeys.defaultEventDurationMinutes] = 0
        }

        val settings = DataStoreSettingsRepository(dataStore).settings.first()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(AppSettings.DEFAULT_ALL_DAY_REMINDER_TIME, settings.allDayReminderTime)
        assertEquals(AppSettings.DEFAULT_EVENT_DURATION_MINUTES, settings.defaultEventDurationMinutes)
    }
}
