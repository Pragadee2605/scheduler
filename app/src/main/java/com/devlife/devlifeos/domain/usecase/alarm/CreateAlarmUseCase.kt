package com.devlife.devlifeos.domain.usecase.alarm

import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.domain.alarm.AlarmScheduler
import java.util.Calendar

/**
 * Use case for creating and scheduling a new alarm
 */
class CreateAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarm: Alarm): Result<Long> {
        return try {
            // Validate alarm data
            if (alarm.hour !in 0..23 || alarm.minute !in 0..59) {
                return Result.failure(IllegalArgumentException("Invalid time"))
            }

            // Insert alarm into database
            val alarmId = alarmRepository.insertAlarm(alarm)
            val savedAlarm = alarm.copy(id = alarmId)

            // Schedule alarm if enabled
            if (savedAlarm.isEnabled) {
                alarmScheduler.scheduleAlarm(savedAlarm)
            }

            Result.success(alarmId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
