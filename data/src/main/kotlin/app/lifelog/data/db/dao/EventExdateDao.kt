package app.lifelog.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.lifelog.data.db.entity.EventExdateEntity

@Dao
internal interface EventExdateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exdate: EventExdateEntity)

    @Query("DELETE FROM event_exdates WHERE eventId = :eventId AND originalStart = :originalStart")
    suspend fun delete(eventId: Long, originalStart: Long)

    @Query("DELETE FROM event_exdates WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)

    @Query("SELECT * FROM event_exdates WHERE eventId IN (:eventIds)")
    suspend fun getForEvents(eventIds: List<Long>): List<EventExdateEntity>
}
