package com.devlife.devlifeos.data.repository

import com.devlife.devlifeos.data.local.dao.AlarmDao
import com.devlife.devlifeos.data.local.entity.Alarm
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing alarms
 */
class AlarmRepository(private val alarmDao: AlarmDao) {

    fun getAllAlarms(): Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(alarmId: Long): Alarm? = alarmDao.getAlarmById(alarmId)

    suspend fun getEnabledAlarms(): List<Alarm> = alarmDao.getEnabledAlarms()

    suspend fun insertAlarm(alarm: Alarm): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = alarmDao.deleteAlarm(alarm)

    suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean) =
        alarmDao.setAlarmEnabled(alarmId, enabled)

    suspend fun deleteAlarmById(alarmId: Long) = alarmDao.deleteAlarmById(alarmId)
}
