package com.devlife.devlifeos.ui.workspace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devlife.devlifeos.data.local.entity.JiraTicket
import com.devlife.devlifeos.data.local.entity.TicketNote
import com.devlife.devlifeos.data.repository.JiraTicketRepository
import com.devlife.devlifeos.data.repository.TicketNoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Ticket Workspace
 */
class TicketWorkspaceViewModel(
    application: Application,
    private val ticketRepository: JiraTicketRepository,
    private val noteRepository: TicketNoteRepository
) : AndroidViewModel(application) {

    private val _ticket = MutableLiveData<JiraTicket?>()
    val ticket: LiveData<JiraTicket?> = _ticket

    private val _lastUpdated = MutableLiveData<String>()
    val lastUpdated: LiveData<String> = _lastUpdated


    private var autoSaveJob: Job? = null
    private var ticketId: Long = -1

    fun loadTicket(ticketId: Long) {
        this.ticketId = ticketId
        viewModelScope.launch {
            val ticket = ticketRepository.getTicketById(ticketId)
            _ticket.value = ticket
            updateLastUpdatedText()
        }
    }

    fun getNoteByTicketId(ticketId: Long): LiveData<TicketNote?> {
        return noteRepository.getNoteByTicketId(ticketId).asLiveData()
    }

    fun saveNoteWithAutoSave(ticketId: Long, content: String) {
        // Cancel previous auto-save job
        autoSaveJob?.cancel()

        // Schedule new auto-save after 1 second of inactivity
        autoSaveJob = viewModelScope.launch {
            delay(1000)
            saveNote(ticketId, content)
        }
    }

    private suspend fun saveNote(ticketId: Long, content: String) {
        val existingNote = noteRepository.getNoteByTicketIdOnce(ticketId)
        val note = if (existingNote != null) {
            existingNote.copy(
                noteContent = content,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            TicketNote(
                ticketId = ticketId,
                noteContent = content
            )
        }
        noteRepository.saveNote(note)
        updateLastUpdatedText()
    }


    private fun updateLastUpdatedText() {
        val currentTime = System.currentTimeMillis()
        val ticket = _ticket.value
        val updatedAt = ticket?.updatedAt ?: currentTime

        val diff = currentTime - updatedAt
        val text = when {
            diff < 60000 -> "Last updated: just now"
            diff < 3600000 -> "Last updated: ${diff / 60000} minutes ago"
            diff < 86400000 -> "Last updated: ${diff / 3600000} hours ago"
            else -> {
                val date = Date(updatedAt)
                val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                "Last updated: ${format.format(date)}"
            }
        }
        _lastUpdated.postValue(text)
    }
}
