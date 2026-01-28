package com.devlife.devlifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jira ticket entity for manual ticket tracking
 */
@Entity(tableName = "jira_tickets")
data class JiraTicket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val ticketId: String, // e.g., "ABC-123"

    val title: String,

    val storyPoints: Int = 0,

    val status: TicketStatus = TicketStatus.TODO,

    val description: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    val completedAt: Long? = null // Timestamp when ticket was marked as DONE
)

enum class TicketStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    BLOCKED
}
