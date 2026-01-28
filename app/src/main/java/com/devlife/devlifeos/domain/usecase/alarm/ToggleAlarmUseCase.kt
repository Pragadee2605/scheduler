package com.devlife.devlifeos.domain.usecase.alarm

import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.domain.alarm.AlarmScheduler

/**
 * Use case for toggling alarm enabled state
 */
class ToggleAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarmId: Long, enabled: Boolean): Result<Unit> {
        return try {
            val alarm = alarmRepository.getAlarmById(alarmId)
                ?: return Result.failure(IllegalArgumentException("Alarm not found"))

            alarmRepository.setAlarmEnabled(alarmId, enabled)

            if (enabled) {
                alarmScheduler.scheduleAlarm(alarm.copy(isEnabled = true))
            } else {
                alarmScheduler.cancelAlarm(alarmId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
