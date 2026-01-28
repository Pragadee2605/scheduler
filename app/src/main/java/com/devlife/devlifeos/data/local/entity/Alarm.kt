package com.devlife.devlifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.devlife.devlifeos.data.local.converter.Converters
import java.util.Calendar

/**
 * Alarm entity for storing alarm configurations
 */
@Entity(tableName = "alarms")
@TypeConverters(Converters::class)
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val hour: Int, // 0-23
    val minute: Int, // 0-59

    val isEnabled: Boolean = true,

    val repeatType: RepeatType = RepeatType.ONCE,

    // For weekly repeat: set of days (1=Monday, 7=Sunday)
    val selectedDays: Set<Int> = emptySet(),

    val label: String = "",

    val soundUri: String = "default", // URI or "default"

    val volume: Int = 80, // 0-100

    val vibrate: Boolean = true,

    val fadeInDuration: Int = 10, // seconds

    val autoDisableOnWeekends: Boolean = true,

    val autoDisableOnHolidays: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)

enum class RepeatType {
    ONCE,
    DAILY,
    WEEKDAYS
}
