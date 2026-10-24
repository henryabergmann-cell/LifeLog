package app.lifelog.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.lifelog.data.db.dao.CategoryDao
import app.lifelog.data.db.dao.DeviceCalendarDao
import app.lifelog.data.db.dao.EventDao
import app.lifelog.data.db.dao.EventExdateDao
import app.lifelog.data.db.dao.JournalDao
import app.lifelog.data.db.dao.ReminderDao
import app.lifelog.data.db.dao.ScheduledAlarmDao
import app.lifelog.data.db.entity.CategoryEntity
import app.lifelog.data.db.entity.DeviceCalendarEntity
import app.lifelog.data.db.entity.DeviceEventLinkEntity
import app.lifelog.data.db.entity.EventEntity
import app.lifelog.data.db.entity.EventExdateEntity
import app.lifelog.data.db.entity.EventFtsEntity
import app.lifelog.data.db.entity.JournalEntryEntity
import app.lifelog.data.db.entity.ReminderEntity
import app.lifelog.data.db.entity.ScheduledAlarmEntity

@Database(
    entities = [
        CategoryEntity::class,
        EventEntity::class,
        EventFtsEntity::class,
        EventExdateEntity::class,
        ReminderEntity::class,
        ScheduledAlarmEntity::class,
        JournalEntryEntity::class,
        DeviceCalendarEntity::class,
        DeviceEventLinkEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class LifeLogDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun eventExdateDao(): EventExdateDao
    abstract fun reminderDao(): ReminderDao
    abstract fun scheduledAlarmDao(): ScheduledAlarmDao
    abstract fun journalDao(): JournalDao
    abstract fun deviceCalendarDao(): DeviceCalendarDao

    companion object {
        const val NAME = "life_log.db"
    }
}
