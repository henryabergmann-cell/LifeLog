package app.lifelog.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.lifelog.data.db.entity.ScheduledAlarmEntity

@Dao
internal interface ScheduledAlarmDao {

    @Query("SELECT * FROM scheduled_alarms")
    suspend fun getAll(): List<ScheduledAlarmEntity>

    @Query("SELECT * FROM scheduled_alarms WHERE requestCode = :requestCode")
    suspend fun get(requestCode: Int): ScheduledAlarmEntity?

    @Upsert
    suspend fun upsertAll(alarms: List<ScheduledAlarmEntity>)

    @Query("DELETE FROM scheduled_alarms WHERE requestCode IN (:requestCodes)")
    suspend fun deleteAll(requestCodes: List<Int>)
}
