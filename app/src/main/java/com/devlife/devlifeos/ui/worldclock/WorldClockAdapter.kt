package com.devlife.devlifeos.ui.worldclock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devlife.devlifeos.R
import com.devlife.devlifeos.data.local.entity.WorldClock
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying world clocks
 */
class WorldClockAdapter(
    private val onDeleteClick: (WorldClock) -> Unit
) : ListAdapter<WorldClock, WorldClockAdapter.WorldClockViewHolder>(WorldClockDiffCallback()) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorldClockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_world_clock, parent, false)
        return WorldClockViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorldClockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorldClockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        private val tvCountryName: TextView = itemView.findViewById(R.id.tvCountryName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTimeDiff: TextView = itemView.findViewById(R.id.tvTimeDiff)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(worldClock: WorldClock) {
            tvCityName.text = worldClock.mainCity
            tvCountryName.text = worldClock.countryName

            try {
                val timeZone = TimeZone.getTimeZone(worldClock.timeZoneId)
                val calendar = Calendar.getInstance(timeZone)

                timeFormat.timeZone = timeZone
                dateFormat.timeZone = timeZone

                tvTime.text = timeFormat.format(calendar.time)
                tvDate.text = dateFormat.format(calendar.time)

                // Calculate time difference from local timezone
                val localOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
                val targetOffset = timeZone.getOffset(System.currentTimeMillis())
                val diffMillis = targetOffset - localOffset
                val diffHours = diffMillis / (1000 * 60 * 60)

                tvTimeDiff.text = when {
                    diffHours > 0 -> "+$diffHours hrs"
                    diffHours < 0 -> "$diffHours hrs"
                    else -> "Same time"
                }
            } catch (_: Exception) {
                tvTime.text = "--:--"
                tvDate.text = "--"
                tvTimeDiff.text = ""
            }

            btnDelete.setOnClickListener {
                onDeleteClick(worldClock)
            }
        }
    }

    class WorldClockDiffCallback : DiffUtil.ItemCallback<WorldClock>() {
        override fun areItemsTheSame(oldItem: WorldClock, newItem: WorldClock): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorldClock, newItem: WorldClock): Boolean {
            return oldItem == newItem
        }
    }

    // Method to refresh times
    fun notifyTimeChanged() {
        notifyDataSetChanged()
    }
}
