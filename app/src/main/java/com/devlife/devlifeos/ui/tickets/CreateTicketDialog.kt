package com.devlife.devlifeos.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.JiraTicket
import com.devlife.devlifeos.data.local.entity.TicketStatus
import com.devlife.devlifeos.data.repository.JiraTicketRepository
import com.devlife.devlifeos.databinding.DialogCreateTicketBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Dialog for creating or editing a Jira ticket
 */
class CreateTicketDialog : DialogFragment() {

    private var _binding: DialogCreateTicketBinding? = null
    private val binding get() = _binding!!

    private var editingTicketId: Long? = null
    private lateinit var viewModel: TicketViewModel

    companion object {
        private const val ARG_TICKET_ID = "ticket_id"
        private const val ARG_TICKET_ID_STR = "ticket_id_str"
        private const val ARG_TITLE = "title"
        private const val ARG_STORY_POINTS = "story_points"
        private const val ARG_STATUS = "status"
        private const val ARG_DESCRIPTION = "description"

        fun newInstance(ticket: JiraTicket? = null): CreateTicketDialog {
            val dialog = CreateTicketDialog()
            if (ticket != null) {
                val bundle = Bundle().apply {
                    putLong(ARG_TICKET_ID, ticket.id)
                    putString(ARG_TICKET_ID_STR, ticket.ticketId)
                    putString(ARG_TITLE, ticket.title)
                    putInt(ARG_STORY_POINTS, ticket.storyPoints)
                    putString(ARG_STATUS, ticket.status.name)
                    putString(ARG_DESCRIPTION, ticket.description)
                }
                dialog.arguments = bundle
            }
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        loadArgumentsIfEditing()
        setupUI()
    }

    private fun loadArgumentsIfEditing() {
        arguments?.let { args ->
            editingTicketId = args.getLong(ARG_TICKET_ID, -1).takeIf { it != -1L }
        }
    }

    private fun setupViewModel() {
        val database = DevLifeDatabase.getDatabase(requireContext())
        val repository = JiraTicketRepository(database.jiraTicketDao())

        val factory = TicketViewModelFactory(requireActivity().application, repository)
        viewModel = ViewModelProvider(this, factory)[TicketViewModel::class.java]
    }

    private fun setupUI() {
        // Update title for editing mode
        if (editingTicketId != null) {
            binding.tvTitle.text = "Edit Ticket"
        }

        // Load existing data if editing
        arguments?.let { args ->
            binding.etTicketId.setText(args.getString(ARG_TICKET_ID_STR, ""))
            binding.etTitle.setText(args.getString(ARG_TITLE, ""))
            binding.etStoryPoints.setText(args.getInt(ARG_STORY_POINTS, 0).toString())
            binding.etDescription.setText(args.getString(ARG_DESCRIPTION, ""))

            args.getString(ARG_STATUS)?.let { statusName ->
                val statusIndex = TicketStatus.values().indexOfFirst { it.name == statusName }
                if (statusIndex != -1) {
                    binding.spinnerStatus.setSelection(statusIndex)
                }
            }
        }

        // Status spinner
        val statuses = TicketStatus.values().map { it.name.replace("_", " ") }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter

        // Save button
        binding.btnSave.setOnClickListener {
            saveTicket()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun saveTicket() {
        val ticketId = binding.etTicketId.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val storyPoints = binding.etStoryPoints.text.toString().toIntOrNull() ?: 0
        val description = binding.etDescription.text.toString().trim()
        val selectedStatus = TicketStatus.values()[binding.spinnerStatus.selectedItemPosition]

        if (ticketId.isEmpty() || title.isEmpty()) {
            Snackbar.make(binding.root, "Please fill all required fields", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Calculate completedAt if status is DONE
        val completedAt = if (selectedStatus == TicketStatus.DONE) {
            System.currentTimeMillis()
        } else {
            null
        }

        val ticket = JiraTicket(
            id = editingTicketId ?: 0,
            ticketId = ticketId,
            title = title,
            storyPoints = storyPoints,
            status = selectedStatus,
            description = description,
            completedAt = completedAt
        )

        if (editingTicketId != null) {
            // Update existing ticket
            viewModel.updateTicket(ticket)

            // Schedule auto-deletion if marked as DONE
            if (selectedStatus == TicketStatus.DONE) {
                viewModel.scheduleTicketDeletion(ticket)
            }

            dismiss()
        } else {
            // Create new ticket
            viewModel.createTicket(ticket) { result ->
                result.onSuccess { id ->
                    // Schedule auto-deletion if marked as DONE
                    if (selectedStatus == TicketStatus.DONE) {
                        viewModel.scheduleTicketDeletion(ticket.copy(id = id))
                    }
                    dismiss()
                }
                result.onFailure {
                    Snackbar.make(binding.root, "Error creating ticket", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
