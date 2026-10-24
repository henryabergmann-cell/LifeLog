package app.lifelog.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. Property names double as SavedStateHandle keys in the
// destination ViewModels (see their ARG_* constants).

@Serializable
data object CalendarRoute

@Serializable
data object JournalRoute

@Serializable
data object InsightsRoute

@Serializable
data object SettingsRoute

@Serializable
data class EventDetailRoute(val eventId: Long)

/** Edit ([eventId]), duplicate ([duplicateOfId]) or create (optionally at a date and time). */
@Serializable
data class EventEditorRoute(
    val eventId: Long = -1,
    val duplicateOfId: Long = -1,
    val dateEpochDay: Long = Long.MIN_VALUE,
    val startMinute: Int = -1,
)

@Serializable
data object SearchRoute

@Serializable
data object CategoriesRoute

@Serializable
data object PremiumRoute
