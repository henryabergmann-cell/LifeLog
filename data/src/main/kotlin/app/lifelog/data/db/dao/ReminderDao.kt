package app.lifelog.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.lifelog.data.db.entity.ReminderEntity

@Dao
internal interface ReminderDao {

    @Insert
    suspend fun insertAll(reminders: List<ReminderEntity>): List<Long>

    @Query("SELECT * FROM reminders WHERE eventId = :eventId ORDER BY minutesBefore")
    suspend fun getForEvent(eventId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE eventId IN (:eventIds) ORDER BY eventId, minutesBefore")
    suspend fun getForEvents(eventIds: List<Long>): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)
}
