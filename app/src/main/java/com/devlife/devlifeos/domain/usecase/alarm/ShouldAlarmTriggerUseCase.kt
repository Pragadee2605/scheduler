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
}
