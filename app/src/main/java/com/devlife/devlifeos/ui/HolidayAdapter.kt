package com.devlife.devlifeos.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devlife.devlifeos.data.local.entity.Holiday
import com.devlife.devlifeos.databinding.ItemHolidayBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying holidays
 */
class HolidayAdapter : ListAdapter<Holiday, HolidayAdapter.HolidayViewHolder>(HolidayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
        val binding = ItemHolidayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HolidayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HolidayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HolidayViewHolder(
        private val binding: ItemHolidayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(holiday: Holiday) {
            binding.tvHolidayName.text = holiday.name

            // Parse and format date
            val calendar = Calendar.getInstance().apply {
                set(holiday.year, holiday.month - 1, holiday.day)
            }
            binding.tvHolidayDate.text = dateFormat.format(calendar.time)

            // Show day of week
            val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            binding.tvDayOfWeek.text = dayOfWeek

            // Show holiday type if it's a company holiday
        }
    }

    private class HolidayDiffCallback : DiffUtil.ItemCallback<Holiday>() {
        override fun areItemsTheSame(oldItem: Holiday, newItem: Holiday): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Holiday, newItem: Holiday): Boolean {
            return oldItem == newItem
        }
    }
}
