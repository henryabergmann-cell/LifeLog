package app.lifelog.domain.premium

import kotlinx.coroutines.flow.StateFlow

/**
 * The single gate for Premium. The UI never talks to billing code directly (PLAN.md §4).
 * M5 adds status, plans, purchase and refresh; until then a free-tier stub is bound.
 */
interface PremiumManager {
    val isPremium: StateFlow<Boolean>
}
