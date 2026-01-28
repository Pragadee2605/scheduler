package com.devlife.devlifeos.ui.tickets

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devlife.devlifeos.data.repository.JiraTicketRepository

/**
 * Factory for creating TicketViewModel with dependencies
 */
class TicketViewModelFactory(
    private val application: Application,
    private val ticketRepository: JiraTicketRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketViewModel::class.java)) {
            return TicketViewModel(application, ticketRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
