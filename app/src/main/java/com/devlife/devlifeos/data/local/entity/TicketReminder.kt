package com.devlife.devlifeos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ticket reminder entity for ticket-specific reminders
 */
@Entity(
    tableName = "ticket_reminders",
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
data class TicketReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val ticketId: Long,

    val title: String,

    val message: String = "",

    val reminderTime: Long, // Timestamp in millis

    val isEnabled: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)
