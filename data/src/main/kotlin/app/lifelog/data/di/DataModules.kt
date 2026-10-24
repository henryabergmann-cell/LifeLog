package app.lifelog.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.lifelog.data.BuildConfig
import app.lifelog.data.db.LifeLogDatabase
import app.lifelog.data.db.dao.CategoryDao
import app.lifelog.data.db.dao.DeviceCalendarDao
import app.lifelog.data.db.dao.EventDao
import app.lifelog.data.db.dao.EventExdateDao
import app.lifelog.data.db.dao.JournalDao
import app.lifelog.data.db.dao.ReminderDao
import app.lifelog.data.db.dao.ScheduledAlarmDao
import app.lifelog.data.repository.RoomCategoryRepository
import app.lifelog.data.repository.RoomEventRepository
import app.lifelog.data.repository.RoomJournalRepository
import app.lifelog.domain.repository.JournalRepository
import app.lifelog.data.settings.DataStoreSettingsRepository
import app.lifelog.data.time.SystemTimeSource
import app.lifelog.data.work.WorkManagerMaintenanceScheduler
import app.lifelog.domain.maintenance.MaintenanceScheduler
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.EventRepository
import app.lifelog.domain.repository.SettingsRepository
import app.lifelog.domain.time.TimeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SettingsPreferences

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeLogDatabase =
        Room.databaseBuilder(context, LifeLogDatabase::class.java, LifeLogDatabase.NAME)
            .apply {
                // Debug only, while the schema is still evolving before the first release.
                // Release builds must always ship real migrations (tested against data/schemas).
                if (BuildConfig.DEBUG) fallbackToDestructiveMigration(dropAllTables = true)
            }
            .build()

    @Provides fun categoryDao(db: LifeLogDatabase): CategoryDao = db.categoryDao()
    @Provides fun eventDao(db: LifeLogDatabase): EventDao = db.eventDao()
    @Provides fun eventExdateDao(db: LifeLogDatabase): EventExdateDao = db.eventExdateDao()
    @Provides fun reminderDao(db: LifeLogDatabase): ReminderDao = db.reminderDao()
    @Provides fun scheduledAlarmDao(db: LifeLogDatabase): ScheduledAlarmDao = db.scheduledAlarmDao()
    @Provides fun journalDao(db: LifeLogDatabase): JournalDao = db.journalDao()
    @Provides fun deviceCalendarDao(db: LifeLogDatabase): DeviceCalendarDao = db.deviceCalendarDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    @Provides
    @Singleton
    @SettingsPreferences
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindEventRepository(impl: RoomEventRepository): EventRepository

    @Binds
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindJournalRepository(impl: RoomJournalRepository): JournalRepository

    @Binds
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource

    @Binds
    abstract fun bindMaintenanceScheduler(impl: WorkManagerMaintenanceScheduler): MaintenanceScheduler
}
