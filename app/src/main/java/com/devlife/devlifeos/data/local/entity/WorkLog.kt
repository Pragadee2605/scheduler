package com.devlife.devlifeos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Work log entity for daily ticket work logs
 */
@Entity(
    tableName = "work_logs",
    foreignKeys = [
        ForeignKey(
            entity = JiraTicket::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ticketId")]
)
data class WorkLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val ticketId: Long,

    val date: String, // Format: YYYY-MM-DD

    val notes: String = "",

    val timeSpentMinutes: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
