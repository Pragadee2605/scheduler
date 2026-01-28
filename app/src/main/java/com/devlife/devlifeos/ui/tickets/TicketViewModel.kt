package com.devlife.devlifeos.ui.tickets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.devlife.devlifeos.data.local.entity.JiraTicket
import com.devlife.devlifeos.data.local.entity.TicketStatus
import com.devlife.devlifeos.data.repository.JiraTicketRepository
import com.devlife.devlifeos.worker.TicketDeletionWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ViewModel for managing Jira tickets
 */
class TicketViewModel(
    application: Application,
    private val ticketRepository: JiraTicketRepository
) : AndroidViewModel(application) {

    val allTickets: LiveData<List<JiraTicket>> = ticketRepository.getAllTickets().asLiveData()

    fun createTicket(ticket: JiraTicket, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            try {
                val id = ticketRepository.insertTicket(ticket)
                onResult(Result.success(id))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updateTicket(ticket: JiraTicket) {
        viewModelScope.launch {
            ticketRepository.updateTicket(ticket)
        }
    }

    fun updateTicketStatus(ticketId: Long, status: TicketStatus) {
        viewModelScope.launch {
            ticketRepository.updateTicketStatus(ticketId, status)
        }
    }

    fun deleteTicket(ticket: JiraTicket) {
        viewModelScope.launch {
            ticketRepository.deleteTicket(ticket)
        }
    }

    fun searchTickets(query: String): LiveData<List<JiraTicket>> {
        return ticketRepository.searchTickets(query).asLiveData()
    }

    /**
     * Schedule automatic deletion of completed ticket after 5 hours
     */
    fun scheduleTicketDeletion(ticket: JiraTicket) {
        val workData = Data.Builder()
            .putLong("ticket_id", ticket.id)
            .build()

        val deletionWork = OneTimeWorkRequestBuilder<TicketDeletionWorker>()
            .setInitialDelay(5, TimeUnit.HOURS)
            .setInputData(workData)
            .addTag("ticket_deletion_${ticket.id}")
            .build()

        WorkManager.getInstance(getApplication()).enqueue(deletionWork)
    }
}
