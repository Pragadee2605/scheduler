package com.devlife.devlifeos.ui.alarm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.domain.alarm.AlarmScheduler
import com.devlife.devlifeos.domain.usecase.alarm.CreateAlarmUseCase
import com.devlife.devlifeos.domain.usecase.alarm.DeleteAlarmUseCase
import com.devlife.devlifeos.domain.usecase.alarm.ToggleAlarmUseCase
import kotlinx.coroutines.launch

/**
 * ViewModel for managing alarms
 */
class AlarmViewModel(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val allAlarms: LiveData<List<Alarm>> = alarmRepository.getAllAlarms().asLiveData()

    private val createAlarmUseCase = CreateAlarmUseCase(alarmRepository, alarmScheduler)
    private val toggleAlarmUseCase = ToggleAlarmUseCase(alarmRepository, alarmScheduler)
    private val deleteAlarmUseCase = DeleteAlarmUseCase(alarmRepository, alarmScheduler)

    fun createAlarm(alarm: Alarm, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            val result = createAlarmUseCase(alarm)
            onResult(result)
        }
    }

    fun updateAlarm(alarm: Alarm, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                alarmRepository.updateAlarm(alarm)
                alarmScheduler.scheduleAlarm(alarm)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            toggleAlarmUseCase(alarmId, enabled)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            deleteAlarmUseCase(alarmId)
        }
    }
}
