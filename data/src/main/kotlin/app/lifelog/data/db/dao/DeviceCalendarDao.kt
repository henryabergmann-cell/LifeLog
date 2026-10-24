package app.lifelog.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.lifelog.data.db.entity.DeviceCalendarEntity
import app.lifelog.data.db.entity.DeviceEventLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DeviceCalendarDao {

    @Query("SELECT * FROM device_calendars ORDER BY accountName, displayName")
    fun observeCalendars(): Flow<List<DeviceCalendarEntity>>

    @Upsert
    suspend fun upsertCalendars(calendars: List<DeviceCalendarEntity>)

    @Query("DELETE FROM device_calendars WHERE calendarId NOT IN (:keepIds)")
    suspend fun deleteCalendarsExcept(keepIds: List<Long>)

    @Query("SELECT * FROM device_event_links WHERE deviceCalendarId = :calendarId")
    suspend fun getLinks(calendarId: Long): List<DeviceEventLinkEntity>

    @Upsert
    suspend fun upsertLink(link: DeviceEventLinkEntity)

    @Query("DELETE FROM device_event_links WHERE eventUid = :eventUid AND deviceCalendarId = :calendarId")
    suspend fun deleteLink(eventUid: String, calendarId: Long)
}
