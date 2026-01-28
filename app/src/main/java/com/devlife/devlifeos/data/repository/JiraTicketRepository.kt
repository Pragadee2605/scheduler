package com.devlife.devlifeos.data.repository

import com.devlife.devlifeos.data.local.dao.JiraTicketDao
import com.devlife.devlifeos.data.local.entity.JiraTicket
import com.devlife.devlifeos.data.local.entity.TicketStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Jira tickets
 */
class JiraTicketRepository(private val jiraTicketDao: JiraTicketDao) {

    fun getAllTickets(): Flow<List<JiraTicket>> = jiraTicketDao.getAllTickets()

    suspend fun getTicketById(ticketId: Long): JiraTicket? =
        jiraTicketDao.getTicketById(ticketId)

    fun getTicketByIdFlow(ticketId: Long): Flow<JiraTicket?> =
        jiraTicketDao.getTicketByIdFlow(ticketId)

    fun getTicketsByStatus(status: TicketStatus): Flow<List<JiraTicket>> =
        jiraTicketDao.getTicketsByStatus(status)

    fun searchTickets(query: String): Flow<List<JiraTicket>> =
        jiraTicketDao.searchTickets(query)

    suspend fun insertTicket(ticket: JiraTicket): Long =
        jiraTicketDao.insertTicket(ticket)

    suspend fun updateTicket(ticket: JiraTicket) =
        jiraTicketDao.updateTicket(ticket)

    suspend fun deleteTicket(ticket: JiraTicket) =
        jiraTicketDao.deleteTicket(ticket)

    suspend fun updateTicketStatus(ticketId: Long, status: TicketStatus) =
        jiraTicketDao.updateTicketStatus(ticketId, status, System.currentTimeMillis())
}
