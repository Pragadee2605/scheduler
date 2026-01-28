package com.devlife.devlifeos.domain.alarm

import com.devlife.devlifeos.data.local.entity.Alarm

/**
 * Interface for alarm scheduling operations
 */
interface AlarmScheduler {

    /**
     * Schedule an alarm using AlarmManager
     */
    fun scheduleAlarm(alarm: Alarm)

    /**
     * Cancel a scheduled alarm
     */
    fun cancelAlarm(alarmId: Long)

    /**
     * Reschedule all enabled alarms (e.g., after reboot)
     */
    suspend fun rescheduleAllAlarms()
}
