package com.devlife.devlifeos.ui.workspace

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devlife.devlifeos.data.repository.JiraTicketRepository
import com.devlife.devlifeos.data.repository.TicketNoteRepository

/**
 * Factory for creating TicketWorkspaceViewModel with dependencies
 */
class TicketWorkspaceViewModelFactory(
    private val application: Application,
    private val ticketRepository: JiraTicketRepository,
    private val noteRepository: TicketNoteRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketWorkspaceViewModel::class.java)) {
            return TicketWorkspaceViewModel(
                application,
                ticketRepository,
                noteRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
