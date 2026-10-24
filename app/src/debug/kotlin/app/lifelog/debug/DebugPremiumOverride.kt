package app.lifelog.debug

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import app.lifelog.domain.premium.PremiumOverride
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/** Debug builds only: persisted "simulate Premium" switch from Developer options. */
class DebugPremiumOverride(private val dataStore: DataStore<Preferences>) : PremiumOverride {

    override val isForcedPremium: Flow<Boolean> = dataStore.data.map { it[FORCED_PREMIUM] ?: false }

    override suspend fun setForcedPremium(enabled: Boolean) {
        dataStore.edit { it[FORCED_PREMIUM] = enabled }
    }

    private companion object {
        val FORCED_PREMIUM = booleanPreferencesKey("forced_premium")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DebugPremiumModule {

    @Provides
    @Singleton
    fun providePremiumOverride(@ApplicationContext context: Context): PremiumOverride =
        DebugPremiumOverride(PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("debug_options") })
}
