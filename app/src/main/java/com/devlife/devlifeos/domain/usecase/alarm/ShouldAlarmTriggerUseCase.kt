package com.devlife.devlifeos.domain.usecase.alarm

import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.local.entity.RepeatType
import com.devlife.devlifeos.data.repository.HolidayRepository
import java.util.Calendar

/**
 * Use case for checking if an alarm should trigger
 */
class ShouldAlarmTriggerUseCase(
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(alarm: Alarm, calendar: Calendar = Calendar.getInstance()): Boolean {
        // Check if alarm is enabled
        if (!alarm.isEnabled) return false

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Check for weekend (Saturday=7, Sunday=1 in Calendar)
        if (alarm.autoDisableOnWeekends) {
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                return false
            }
        }

        // Check for holidays
        if (alarm.autoDisableOnHolidays) {
            val isHoliday = holidayRepository.isHoliday(year, month, day)
            if (isHoliday) return false
        }

        // Check repeat type
        when (alarm.repeatType) {
            RepeatType.ONCE -> return true
            RepeatType.DAILY -> return true
            RepeatType.WEEKDAYS -> {
                // Check if today is in selected days (1=Monday, 7=Sunday)
                val dayOfWeekIso = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                return alarm.selectedDays.contains(dayOfWeekIso)
            }
        }
    }

    /**
     * Find the next valid day for an alarm to trigger
     * Returns null if no valid day can be found
     */
    suspend fun findNextValidDay(alarm: Alarm, startCalendar: Calendar = Calendar.getInstance()): Calendar? {
        if (!alarm.isEnabled) return null

        val calendar = startCalendar.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
        calendar.set(Calendar.MINUTE, alarm.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If the time has passed today, start from tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // For ONCE alarms, don't search further
        if (alarm.repeatType == RepeatType.ONCE) {
            return if (invoke(alarm, calendar)) calendar else null
        }

        // Search for next valid day (limit to 90 days to avoid infinite loops)
        var attempts = 0
        while (attempts < 90) {
            if (invoke(alarm, calendar)) {
                return calendar
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            attempts++
        }

        return null // Could not find valid day in next 90 days
    }
}
