package com.devlife.devlifeos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Holiday entity for storing India government holidays
 * Unique index on date ensures no duplicate holidays on the same date
 */
@Entity(
    tableName = "holidays",
    indices = [Index(value = ["date"], unique = true)]
)
data class Holiday(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val date: String, // Format: YYYY-MM-DD

    val year: Int,

    val month: Int, // 1-12

    val day: Int, // 1-31

    val type: HolidayType = HolidayType.NATIONAL,

    val state: String? = null, // For future state-wise holiday support

    val createdAt: Long = System.currentTimeMillis()
)

enum class HolidayType {
    NATIONAL,
    STATE,
  OPTIONAL,
    COMPANY
}
