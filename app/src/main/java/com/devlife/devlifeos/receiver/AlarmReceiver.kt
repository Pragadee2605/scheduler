package com.devlife.devlifeos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.data.repository.HolidayRepository
import com.devlife.devlifeos.domain.alarm.AlarmSchedulerImpl
import com.devlife.devlifeos.domain.usecase.alarm.ShouldAlarmTriggerUseCase
import com.devlife.devlifeos.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling alarm triggers
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmSchedulerImpl.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = DevLifeDatabase.getDatabase(context)
                val alarmRepository = AlarmRepository(database.alarmDao())
                val holidayRepository = HolidayRepository(database.holidayDao())

                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm == null) {
                    pendingResult.finish()
                    return@launch
                }

                // Check if alarm should trigger (not on weekend/holiday)
                val shouldTriggerUseCase = ShouldAlarmTriggerUseCase(holidayRepository)
                val shouldTrigger = shouldTriggerUseCase(alarm)

                if (shouldTrigger) {
                    // Start AlarmService to play sound
                    val serviceIntent = Intent(context, AlarmService::class.java).apply {
                        putExtra(EXTRA_ALARM_ID, alarmId)
                    }
                    context.startForegroundService(serviceIntent)
                } else {
                    // Reschedule for next occurrence
                    val alarmScheduler = AlarmSchedulerImpl(context, alarmRepository)
                    alarmScheduler.scheduleAlarm(alarm)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
