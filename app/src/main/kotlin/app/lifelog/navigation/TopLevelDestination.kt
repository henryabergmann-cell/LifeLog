package app.lifelog.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.lifelog.R
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    CALENDAR(CalendarRoute, CalendarRoute::class, R.drawable.ic_calendar_month, R.string.nav_calendar),
    JOURNAL(JournalRoute, JournalRoute::class, R.drawable.ic_journal, R.string.nav_journal),
    INSIGHTS(InsightsRoute, InsightsRoute::class, R.drawable.ic_insights, R.string.nav_insights),
    SETTINGS(SettingsRoute, SettingsRoute::class, R.drawable.ic_settings, R.string.nav_settings),
}
