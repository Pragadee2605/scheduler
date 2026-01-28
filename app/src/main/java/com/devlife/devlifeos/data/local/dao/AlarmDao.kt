package com.devlife.devlifeos.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.devlife.devlifeos.data.local.entity.Alarm
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Alarm entity
 */
@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarmById(alarmId: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarms(): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id = :alarmId")
    suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean)

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: Long)
}
