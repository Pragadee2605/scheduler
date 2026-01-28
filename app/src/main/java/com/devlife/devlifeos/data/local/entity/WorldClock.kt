package com.devlife.devlifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a world clock timezone
 */
@Entity(tableName = "world_clocks")
data class WorldClock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val countryName: String,
    val mainCity: String, // Main city for display
    val timeZoneId: String // e.g., "America/New_York"
)
