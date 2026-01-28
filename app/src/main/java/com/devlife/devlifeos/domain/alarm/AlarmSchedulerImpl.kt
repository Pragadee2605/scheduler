package com.devlife.devlifeos.domain.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.local.entity.RepeatType
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.receiver.AlarmReceiver
import java.util.Calendar

/**
 * Implementation of AlarmScheduler using Android AlarmManager
 */
class AlarmSchedulerImpl(
    private val context: Context,
    private val alarmRepository: AlarmRepository
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Use setAlarmClock for reliable alarm delivery
        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            calendar.timeInMillis,
            pendingIntent
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    override fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override suspend fun rescheduleAllAlarms() {
        val alarms = alarmRepository.getEnabledAlarms()
        alarms.forEach { alarm ->
            scheduleAlarm(alarm)
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
