package com.devlife.devlifeos.domain.usecase.alarm

import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.domain.alarm.AlarmScheduler

/**
 * Use case for deleting an alarm
 */
class DeleteAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarmId: Long): Result<Unit> {
        return try {
            alarmScheduler.cancelAlarm(alarmId)
            alarmRepository.deleteAlarmById(alarmId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
