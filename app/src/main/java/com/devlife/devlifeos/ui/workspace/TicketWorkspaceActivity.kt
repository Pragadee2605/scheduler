package com.devlife.devlifeos.ui.workspace

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.repository.JiraTicketRepository
import com.devlife.devlifeos.data.repository.TicketNoteRepository
import com.devlife.devlifeos.databinding.ActivityTicketWorkspaceBinding

/**
 * Activity for Ticket Workspace - manage notes for a ticket
 */
class TicketWorkspaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTicketWorkspaceBinding
    private lateinit var viewModel: TicketWorkspaceViewModel
    private var ticketId: Long = -1

    companion object {
        const val EXTRA_TICKET_ID = "ticket_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketWorkspaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ticketId = intent.getLongExtra(EXTRA_TICKET_ID, -1)
        if (ticketId == -1L) {
            finish()
            return
        }

        setupViewModel()
        setupToolbar()
        setupObservers()
        setupNotesSection()

        viewModel.loadTicket(ticketId)
    }

    private fun setupViewModel() {
        val database = DevLifeDatabase.getDatabase(this)
        val ticketRepository = JiraTicketRepository(database.jiraTicketDao())
        val noteRepository = TicketNoteRepository(database.ticketNoteDao())

        val factory = TicketWorkspaceViewModelFactory(
            application,
            ticketRepository,
            noteRepository
        )
        viewModel = ViewModelProvider(this, factory)[TicketWorkspaceViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        // Observe ticket data
        viewModel.ticket.observe(this) { ticket ->
            ticket?.let {
                binding.tvTicketId.text = it.ticketId
                binding.tvTicketTitle.text = it.title
                supportActionBar?.title = it.ticketId
            }
        }

        // Observe last updated time
        viewModel.lastUpdated.observe(this) { text ->
            binding.tvLastUpdated.text = text
        }


        // Observe notes
        viewModel.getNoteByTicketId(ticketId).observe(this) { note ->
            note?.let {
                // Only update if different to avoid cursor jumping
                val currentText = binding.etNotes.text.toString()
                if (currentText != it.noteContent) {
                    binding.etNotes.setText(it.noteContent)
                    binding.etNotes.setSelection(it.noteContent.length)
                }
            }
        }
    }

    private fun setupNotesSection() {
        binding.etNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Auto-save after 1 second of inactivity
                s?.let {
                    viewModel.saveNoteWithAutoSave(ticketId, it.toString())
                }
            }
        })
    }


    override fun onPause() {
        super.onPause()
        // Save current note state
        val noteContent = binding.etNotes.text.toString()
        if (noteContent.isNotEmpty()) {
            viewModel.saveNoteWithAutoSave(ticketId, noteContent)
        }
    }
}
