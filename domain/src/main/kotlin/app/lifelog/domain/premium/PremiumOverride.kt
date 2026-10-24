package app.lifelog.domain.premium

import kotlinx.coroutines.flow.Flow

/**
 * Lets debug builds simulate Premium for testing (PLAN.md §4.11). Release builds bind an
 * implementation that is always off, and the toggle UI only exists in the debug source set.
 */
interface PremiumOverride {
    val isForcedPremium: Flow<Boolean>

    suspend fun setForcedPremium(enabled: Boolean)
}
