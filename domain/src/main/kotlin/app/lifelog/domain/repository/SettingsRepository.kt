package app.lifelog.domain.repository

import app.lifelog.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    /** Atomically reads the current settings, applies [transform] and persists the result. */
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
