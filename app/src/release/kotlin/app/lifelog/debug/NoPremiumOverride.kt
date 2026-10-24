package app.lifelog.debug

import app.lifelog.domain.premium.PremiumOverride
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Release builds: Premium can never be simulated. */
object NoPremiumOverride : PremiumOverride {
    override val isForcedPremium: Flow<Boolean> = flowOf(false)

    override suspend fun setForcedPremium(enabled: Boolean) = Unit
}

@Module
@InstallIn(SingletonComponent::class)
object ReleasePremiumModule {

    @Provides
    fun providePremiumOverride(): PremiumOverride = NoPremiumOverride
}
