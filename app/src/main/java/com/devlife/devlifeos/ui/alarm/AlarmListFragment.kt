package com.devlife.devlifeos.ui.alarm

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
import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.local.entity.RepeatType
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.databinding.FragmentAlarmListBinding
import com.devlife.devlifeos.domain.alarm.AlarmSchedulerImpl
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Fragment for displaying list of alarms
 */
class AlarmListFragment : Fragment() {

    private var _binding: FragmentAlarmListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlarmViewModel
    private lateinit var adapter: AlarmAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmListBinding.inflate(inflater, container, false)
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
        val repository = AlarmRepository(database.alarmDao())
        val scheduler = AlarmSchedulerImpl(requireContext(), repository)

        val factory = AlarmViewModelFactory(repository, scheduler)
        viewModel = ViewModelProvider(this, factory)[AlarmViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onToggle = { alarm, enabled ->
                viewModel.toggleAlarm(alarm.id, enabled)
            },
            onDelete = { alarm ->
                viewModel.deleteAlarm(alarm.id)
            },
            onEdit = { alarm ->
                showEditAlarmActivity(alarm)
            }
        )

        binding.recyclerViewAlarms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AlarmListFragment.adapter
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
                    val alarm = adapter.currentList[position]
                    viewModel.deleteAlarm(alarm.id)
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewAlarms)
    }

    private fun observeData() {
        viewModel.allAlarms.observe(viewLifecycleOwner) { alarms ->
            adapter.submitList(alarms)
            binding.emptyView.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE

            // Update next alarm time
            updateNextAlarmTime(alarms)
        }
    }

    private fun setupFab() {
        binding.fabAddAlarm.setOnClickListener {
            showCreateAlarmActivity()
        }
    }

    private fun showCreateAlarmActivity() {
        val intent = Intent(requireContext(), CreateAlarmActivity::class.java)
        startActivity(intent)
    }

    private fun showEditAlarmActivity(alarm: Alarm) {
        val intent = Intent(requireContext(), CreateAlarmActivity::class.java).apply {
            putExtra(CreateAlarmActivity.EXTRA_ALARM_ID, alarm.id)
            putExtra(CreateAlarmActivity.EXTRA_HOUR, alarm.hour)
            putExtra(CreateAlarmActivity.EXTRA_MINUTE, alarm.minute)
            putExtra(CreateAlarmActivity.EXTRA_LABEL, alarm.label)
            putExtra(CreateAlarmActivity.EXTRA_REPEAT_TYPE, alarm.repeatType.name)
            putExtra(CreateAlarmActivity.EXTRA_AUTO_DISABLE_WEEKENDS, alarm.autoDisableOnWeekends)
            putExtra(CreateAlarmActivity.EXTRA_AUTO_DISABLE_HOLIDAYS, alarm.autoDisableOnHolidays)
            putExtra(CreateAlarmActivity.EXTRA_SOUND_URI, alarm.soundUri)
            putExtra(CreateAlarmActivity.EXTRA_SELECTED_DAYS, alarm.selectedDays.toIntArray())
            putExtra(CreateAlarmActivity.EXTRA_SNOOZE_DURATION, alarm.snoozeDuration)
            putExtra(CreateAlarmActivity.EXTRA_RING_DURATION, alarm.ringDuration)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateNextAlarmTime(alarms: List<Alarm>) {
        val enabledAlarms = alarms.filter { it.isEnabled }

        if (enabledAlarms.isEmpty()) {
            binding.tvNextAlarmTime.visibility = View.GONE
            return
        }

        // Find the next alarm to ring
        val nextAlarm = findNextAlarm(enabledAlarms)

        if (nextAlarm != null) {
            val timeRemaining = calculateTimeRemaining(nextAlarm)
            if (timeRemaining != null) {
                binding.tvNextAlarmTime.visibility = View.VISIBLE
                binding.tvNextAlarmTime.text = "Next alarm in $timeRemaining"
            } else {
                binding.tvNextAlarmTime.visibility = View.GONE
            }
        } else {
            binding.tvNextAlarmTime.visibility = View.GONE
        }
    }

    private fun findNextAlarm(alarms: List<Alarm>): Alarm? {
        val now = System.currentTimeMillis()
        var nextAlarm: Alarm? = null
        var minTimeToAlarm = Long.MAX_VALUE

        for (alarm in alarms) {
            val nextOccurrence = getNextOccurrence(alarm)
            val timeToAlarm = nextOccurrence - now

            if (timeToAlarm > 0 && timeToAlarm < minTimeToAlarm) {
                minTimeToAlarm = timeToAlarm
                nextAlarm = alarm
            }
        }

        return nextAlarm
    }

    private fun getNextOccurrence(alarm: Alarm): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Create alarm calendar
        val alarmCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If alarm time has passed today, move to tomorrow
        if (alarmCalendar.timeInMillis <= now) {
            alarmCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Handle repeat types
        when (alarm.repeatType) {
            RepeatType.WEEKDAYS -> {
                if (alarm.selectedDays.isNotEmpty()) {
                    // Find next occurrence based on selected days
                    var daysToAdd = 0
                    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val currentDayIndex = when (currentDayOfWeek) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        Calendar.SUNDAY -> 7
                        else -> 1
                    }

                    // Check if alarm already passed today
                    val alarmPassedToday = alarmCalendar.timeInMillis <= now

                    // Find next selected day
                    for (i in 0..7) {
                        val checkDay = (currentDayIndex + i) % 7
                        val normalizedDay = if (checkDay == 0) 7 else checkDay

                        // Skip today if alarm already passed
                        if (i == 0 && alarmPassedToday) continue

                        if (normalizedDay in alarm.selectedDays) {
                            daysToAdd = i
                            break
                        }
                    }

                    alarmCalendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
                    alarmCalendar.set(Calendar.MINUTE, alarm.minute)
                    alarmCalendar.set(Calendar.SECOND, 0)
                    alarmCalendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
                }
            }
            RepeatType.ONCE -> {
                // Already set correctly above
            }
            RepeatType.DAILY -> {
                // Already set correctly above
            }
        }

        return alarmCalendar.timeInMillis
    }

    private fun calculateTimeRemaining(alarm: Alarm): String? {
        val now = System.currentTimeMillis()
        val nextOccurrence = getNextOccurrence(alarm)
        val diffInMillis = nextOccurrence - now

        if (diffInMillis <= 0) return null

        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60

        return formatTimeRemaining(hours, minutes)
    }

    private fun formatTimeRemaining(hours: Long, minutes: Long): String {
        return when {
            hours > 0 && minutes > 0 -> {
                val hourStr = if (hours == 1L) "hour" else "hours"
                val minuteStr = if (minutes == 1L) "minute" else "minutes"
                "$hours $hourStr $minutes $minuteStr"
            }
            hours > 0 -> {
                val hourStr = if (hours == 1L) "hour" else "hours"
                "$hours $hourStr"
            }
            minutes > 0 -> {
                val minuteStr = if (minutes == 1L) "minute" else "minutes"
                "$minutes $minuteStr"
            }
            else -> "Less than a minute"
        }
    }
}
