package app.lifelog.billing

import app.lifelog.domain.premium.PremiumManager
import app.lifelog.domain.premium.PremiumOverride
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium state until M5 wires RevenueCat: nobody has a real subscription yet, so Premium is
 * only on when a debug build simulates it ([PremiumOverride]; always off in release builds).
 */
internal class PremiumManagerImpl @Inject constructor(
    override: PremiumOverride,
) : PremiumManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isPremium: StateFlow<Boolean> = override.isForcedPremium
        .map { forced -> forced /* || entitlement from RevenueCat (M5) */ }
        .stateIn(scope, SharingStarted.Eagerly, initialValue = false)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindPremiumManager(impl: PremiumManagerImpl): PremiumManager
}
