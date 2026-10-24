package app.lifelog.domain.premium

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Turns the Premium state into concrete limits, so screens never reason about billing. */
class FeatureGate @Inject constructor(
    private val premiumManager: PremiumManager,
) {
    val limits: Flow<FeatureLimits> = premiumManager.isPremium.map(::FeatureLimits)

    fun currentLimits(): FeatureLimits = FeatureLimits(premiumManager.isPremium.value)
}

data class FeatureLimits(val isPremium: Boolean) {
    val maxCategories: Int get() = if (isPremium) Int.MAX_VALUE else FREE_MAX_CATEGORIES
    val maxRemindersPerEvent: Int get() = if (isPremium) PREMIUM_MAX_REMINDERS else 1
    val customCategoryColors: Boolean get() = isPremium

    fun canCreateCategory(existingCount: Int): Boolean = existingCount < maxCategories

    companion object {
        const val FREE_MAX_CATEGORIES = 3
        const val PREMIUM_MAX_REMINDERS = 5
    }
}
