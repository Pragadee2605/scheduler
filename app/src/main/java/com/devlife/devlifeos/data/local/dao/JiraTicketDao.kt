package com.devlife.devlifeos.data.local.dao

import androidx.room.*
import com.devlife.devlifeos.data.local.entity.JiraTicket
import com.devlife.devlifeos.data.local.entity.TicketStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for JiraTicket entity
 */
@Dao
interface JiraTicketDao {

    @Query("SELECT * FROM jira_tickets ORDER BY updatedAt DESC")
    fun getAllTickets(): Flow<List<JiraTicket>>

    @Query("SELECT * FROM jira_tickets WHERE id = :ticketId")
    suspend fun getTicketById(ticketId: Long): JiraTicket?

    @Query("SELECT * FROM jira_tickets WHERE id = :ticketId")
    fun getTicketByIdFlow(ticketId: Long): Flow<JiraTicket?>

    @Query("SELECT * FROM jira_tickets WHERE status = :status ORDER BY updatedAt DESC")
    fun getTicketsByStatus(status: TicketStatus): Flow<List<JiraTicket>>

    @Query("SELECT * FROM jira_tickets WHERE ticketId LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    fun searchTickets(query: String): Flow<List<JiraTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: JiraTicket): Long

    @Update
    suspend fun updateTicket(ticket: JiraTicket)

    @Delete
    suspend fun deleteTicket(ticket: JiraTicket)

    @Query("UPDATE jira_tickets SET status = :status, updatedAt = :timestamp WHERE id = :ticketId")
    suspend fun updateTicketStatus(ticketId: Long, status: TicketStatus, timestamp: Long)
}
