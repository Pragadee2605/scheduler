package com.devlife.devlifeos.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.local.entity.RepeatType
import com.devlife.devlifeos.databinding.ItemAlarmBinding
import java.util.*

/**
 * RecyclerView adapter for displaying alarms
 */
class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onDelete: (Alarm) -> Unit,
    private val onEdit: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(
        private val binding: ItemAlarmBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: Alarm) {
            // Format time with AM/PM in separate views
            val amPm = if (alarm.hour < 12) "AM" else "PM"
            val displayHour = when {
                alarm.hour == 0 -> 12
                alarm.hour > 12 -> alarm.hour - 12
                else -> alarm.hour
            }
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", displayHour, alarm.minute)
            binding.tvTime.text = timeString
            binding.tvAmPm.text = amPm

            // Set label
            binding.tvLabel.text = alarm.label.ifEmpty {
                "Alarm"
            }

            // Set repeat info
            binding.tvRepeat.text = getRepeatText(alarm)


            // Set enabled switch
            binding.switchEnabled.isChecked = alarm.isEnabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }

            // Delete button
            binding.btnDelete.setOnClickListener {
                onDelete(alarm)
            }

            // Click to edit
            binding.root.setOnClickListener {
                onEdit(alarm)
            }
        }

        private fun getRepeatText(alarm: Alarm): String {
            return when (alarm.repeatType) {
                RepeatType.ONCE -> "Once"
                RepeatType.DAILY -> "Every day"
                RepeatType.WEEKDAYS -> {
                    if (alarm.selectedDays.isEmpty()) {
                        "Select days"
                    } else {
                        val days = alarm.selectedDays.sorted().joinToString(", ") {
                            when (it) {
                                1 -> "Mon"
                                2 -> "Tue"
                                3 -> "Wed"
                                4 -> "Thu"
                                5 -> "Fri"
                                6 -> "Sat"
                                7 -> "Sun"
                                else -> ""
                            }
                        }
                        days
                    }
                }
            }
        }
    }

    private class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}
