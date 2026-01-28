package com.devlife.devlifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing ticket notes
 */
@Entity(tableName = "ticket_notes")
data class TicketNote(
    @PrimaryKey
    val ticketId: Long,

    val noteContent: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
