package com.devlife.devlifeos.domain.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        if (!alarm.isEnabled) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = calculateNextAlarmTime(alarm)
        if (calendar == null) {
            // Could not find a valid next occurrence (e.g., ONCE alarm in the past)
            return
        }

        // Use setAlarmClock for reliable alarm delivery
        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            calendar.timeInMillis,
            pendingIntent
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    /**
     * Calculate the next alarm time based on repeat type and current time
     */
    private fun calculateNextAlarmTime(alarm: Alarm): Calendar? {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time has passed today, start checking from tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return when (alarm.repeatType) {
            RepeatType.ONCE -> {
                // For ONCE, if the time has passed today, return null (don't reschedule)
                val todayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (todayCalendar.timeInMillis > System.currentTimeMillis()) {
                    todayCalendar
                } else {
                    null // Time has passed, don't reschedule ONCE alarms
                }
            }

            RepeatType.DAILY -> {
                // For DAILY, just use the calculated calendar (next occurrence)
                calendar
            }

            RepeatType.WEEKDAYS -> {
                // For WEEKDAYS, find the next day that matches selectedDays
                if (alarm.selectedDays.isEmpty()) {
                    return null // No days selected, can't schedule
                }

                // Search for the next valid day (limit to 7 days to avoid infinite loop)
                var attempts = 0
                while (attempts < 7) {
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    // Convert Calendar.DAY_OF_WEEK to ISO format (1=Monday, 7=Sunday)
                    val dayOfWeekIso = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

                    if (alarm.selectedDays.contains(dayOfWeekIso)) {
                        return calendar // Found a valid day
                    }

                    // Try next day
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    attempts++
                }

                null // Could not find a valid day in selected days
            }
        }
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
