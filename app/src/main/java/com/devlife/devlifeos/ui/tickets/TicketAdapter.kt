package com.devlife.devlifeos.ui.tickets

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devlife.devlifeos.data.local.entity.JiraTicket
import com.devlife.devlifeos.data.local.entity.TicketStatus
import com.devlife.devlifeos.databinding.ItemTicketBinding

/**
 * RecyclerView adapter for displaying Jira tickets
 */
class TicketAdapter(
    private val onEdit: (JiraTicket) -> Unit,
    private val onDelete: (JiraTicket) -> Unit,
    private val onStatusChange: (JiraTicket, TicketStatus) -> Unit,
    private val onTicketClick: (JiraTicket) -> Unit
) : ListAdapter<JiraTicket, TicketAdapter.TicketViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TicketViewHolder(
        private val binding: ItemTicketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: JiraTicket) {
            binding.tvTicketId.text = ticket.ticketId
            binding.tvTitle.text = ticket.title
            binding.tvStoryPoints.text = "${ticket.storyPoints} SP"
            binding.chipStatus.text = ticket.status.name.replace("_", " ")

            // Set status chip color
            val colorRes = when (ticket.status) {
                TicketStatus.TODO -> android.R.color.holo_blue_light
                TicketStatus.IN_PROGRESS -> android.R.color.holo_orange_light
                TicketStatus.IN_REVIEW -> android.R.color.holo_purple
                TicketStatus.DONE -> android.R.color.holo_green_light
                TicketStatus.BLOCKED -> android.R.color.holo_red_light
            }
            binding.chipStatus.setChipBackgroundColorResource(colorRes)

            // Make chip clickable to show status dropdown
            binding.chipStatus.setOnClickListener { view ->
                showStatusDropdown(view, ticket)
            }

            // Click on card to open workspace (tap to edit)
            binding.root.setOnClickListener {
                onTicketClick(ticket)
            }
        }

        private fun showStatusDropdown(view: android.view.View, ticket: JiraTicket) {
            val popupMenu = PopupMenu(view.context, view)

            // Add all status options
            TicketStatus.entries.forEachIndexed { index, status ->
                val statusName = status.name.replace("_", " ")
                popupMenu.menu.add(0, index, index, statusName)
            }

            // Handle menu item click
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedStatus = TicketStatus.entries[menuItem.itemId]
                onStatusChange(ticket, selectedStatus)
                true
            }

            popupMenu.show()
        }
    }

    private class TicketDiffCallback : DiffUtil.ItemCallback<JiraTicket>() {
        override fun areItemsTheSame(oldItem: JiraTicket, newItem: JiraTicket): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JiraTicket, newItem: JiraTicket): Boolean {
            return oldItem == newItem
        }
    }
}
