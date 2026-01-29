package com.devlife.devlifeos.util

import java.util.*

/**
 * Utility class for time format conversion between 24-hour and 12-hour formats
 */
object TimeFormatUtil {

    /**
     * Converts 24-hour format to 12-hour format with AM/PM
     * @param hour24 Hour in 24-hour format (0-23)
     * @param minute Minute (0-59)
     * @return FormattedTime object containing display hour, minute, and AM/PM
     */
    fun convert24HourTo12Hour(hour24: Int, minute: Int): FormattedTime {
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12  // Midnight: 00:00 -> 12:00 AM
            hour24 > 12 -> hour24 - 12  // Afternoon/Evening: 13-23 -> 1-11 PM
            else -> hour24  // Morning: 1-12 -> 1-12 AM/PM
        }
        return FormattedTime(hour12, minute, amPm, hour24)
    }

    /**
     * Formats time as a readable string in 12-hour format
     * @param hour24 Hour in 24-hour format (0-23)
     * @param minute Minute (0-59)
     * @return Formatted string like "07:30 AM"
     */
    fun format12Hour(hour24: Int, minute: Int): String {
        val formatted = convert24HourTo12Hour(hour24, minute)
        return String.format(
            Locale.getDefault(),
            "%02d:%02d %s",
            formatted.hour12,
            formatted.minute,
            formatted.amPm
        )
    }

    /**
     * Data class to hold formatted time information
     */
    data class FormattedTime(
        val hour12: Int,       // Hour in 12-hour format (1-12)
        val minute: Int,       // Minute (0-59)
        val amPm: String,      // "AM" or "PM"
        val hour24: Int        // Original hour in 24-hour format (0-23)
    )
}
