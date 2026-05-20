package com.devlife.devlifeos.ui.tickets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.JiraTicket
import com.devlife.devlifeos.data.local.entity.TicketStatus
import com.devlife.devlifeos.data.repository.JiraTicketRepository
import com.devlife.devlifeos.databinding.FragmentTicketListBinding
import com.devlife.devlifeos.ui.workspace.TicketWorkspaceActivity

/**
 * Fragment for displaying list of Jira tickets
 */
class TicketListFragment : Fragment() {

    private var _binding: FragmentTicketListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TicketViewModel
    private lateinit var adapter: TicketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        observeData()
        setupFab()
    }

    private fun setupViewModel() {
        val database = DevLifeDatabase.getDatabase(requireContext())
        val repository = JiraTicketRepository(database.jiraTicketDao())

        val factory = TicketViewModelFactory(requireActivity().application, repository)
        viewModel = ViewModelProvider(this, factory)[TicketViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = TicketAdapter(
            onEdit = { ticket ->
                showEditTicketDialog(ticket)
            },
            onDelete = { ticket ->
                viewModel.deleteTicket(ticket)
            },
            onStatusChange = { ticket, newStatus ->
                handleStatusChange(ticket, newStatus)
            },
            onTicketClick = { ticket ->
                openTicketWorkspace(ticket)
            }
        )

        binding.recyclerViewTickets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TicketListFragment.adapter
        }

        // Swipe to delete
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val ticket = adapter.currentList[position]
                    viewModel.deleteTicket(ticket)
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewTickets)
    }

    private fun observeData() {
        viewModel.allTickets.observe(viewLifecycleOwner) { tickets ->
            adapter.submitList(tickets)
            binding.emptyView.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupFab() {
        binding.fabAddTicket.setOnClickListener {
            showCreateTicketDialog()
        }
    }

    private fun showCreateTicketDialog() {
        val dialog = CreateTicketDialog.newInstance()
        dialog.show(childFragmentManager, "CreateTicketDialog")
    }

    private fun showEditTicketDialog(ticket: JiraTicket) {
        val dialog = CreateTicketDialog.newInstance(ticket)
        dialog.show(childFragmentManager, "EditTicketDialog")
    }

    private fun handleStatusChange(ticket: JiraTicket, newStatus: TicketStatus) {
        // Calculate completedAt if status is DONE
        val completedAt = if (newStatus == TicketStatus.DONE) {
            System.currentTimeMillis()
        } else {
            null
        }

        val updatedTicket = ticket.copy(
            status = newStatus,
            completedAt = completedAt,
            updatedAt = System.currentTimeMillis()
        )

        viewModel.updateTicket(updatedTicket)

        // Schedule auto-deletion if marked as DONE
        if (newStatus == TicketStatus.DONE) {
            viewModel.scheduleTicketDeletion(updatedTicket)
        }
    }

    private fun openTicketWorkspace(ticket: JiraTicket) {
        val intent = Intent(requireContext(), TicketWorkspaceActivity::class.java)
        intent.putExtra(TicketWorkspaceActivity.EXTRA_TICKET_ID, ticket.id)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
