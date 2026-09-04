package com.devlife.devlifeos.ui.alarm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.devlife.devlifeos.R
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.local.entity.RepeatType
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.databinding.ActivityCreateAlarmBinding
import com.devlife.devlifeos.domain.alarm.AlarmSchedulerImpl
import com.google.android.material.button.MaterialButton
import java.util.Locale

/**
 * Full-screen Activity for creating or editing an alarm
 * Modern design with scrollable time pickers
 */
class CreateAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAlarmBinding
    private lateinit var viewModel: AlarmViewModel

    private var selectedHour = 7
    private var selectedMinute = 0
    private var isAm = true
    private var selectedRepeatType = RepeatType.ONCE
    private var selectedRingtoneUri: String = "default"
    private var selectedRingtoneName: String = "Default alarm sound"
    private var selectedDays = mutableSetOf<Int>()
    private var selectedSnoozeMinutes = 5
    private var selectedRingMinutes = 5

    private var editingAlarmId: Long? = null

    // File picker for custom ringtone
    private val audioFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            handleSelectedAudioFile(it)
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_LABEL = "label"
        const val EXTRA_REPEAT_TYPE = "repeat_type"
        const val EXTRA_AUTO_DISABLE_WEEKENDS = "auto_disable_weekends"
        const val EXTRA_AUTO_DISABLE_HOLIDAYS = "auto_disable_holidays"
        const val EXTRA_SOUND_URI = "sound_uri"
        const val EXTRA_SELECTED_DAYS = "selected_days"
        const val EXTRA_SNOOZE_DURATION = "snooze_duration"
        const val EXTRA_RING_DURATION = "ring_duration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        loadIntentData()
        setupToolbar()
        setupTimePickers()
        setupUI()
    }

    private fun setupViewModel() {
        val database = DevLifeDatabase.getDatabase(this)
        val repository = AlarmRepository(database.alarmDao())
        val scheduler = AlarmSchedulerImpl(this, repository)

        val factory = AlarmViewModelFactory(repository, scheduler)
        viewModel = ViewModelProvider(this, factory)[AlarmViewModel::class.java]
    }

    private fun loadIntentData() {
        intent?.let { intent ->
            editingAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1).takeIf { it != -1L }
            val hour24 = intent.getIntExtra(EXTRA_HOUR, 7)
            selectedMinute = intent.getIntExtra(EXTRA_MINUTE, 0)

            // Convert 24-hour to 12-hour format
            isAm = hour24 < 12
            selectedHour = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                hour24 == 12 -> 12
                else -> hour24
            }

            intent.getStringExtra(EXTRA_REPEAT_TYPE)?.let {
                selectedRepeatType = RepeatType.valueOf(it)
            }
            val daysArray = intent.getIntArrayExtra(EXTRA_SELECTED_DAYS)
            daysArray?.let {
                selectedDays.clear()
                selectedDays.addAll(it.toList())
            }
            selectedRingtoneUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: "default"
            selectedRingtoneName = getRingtoneName(selectedRingtoneUri)
            selectedSnoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_DURATION, 5)
            selectedRingMinutes = intent.getIntExtra(EXTRA_RING_DURATION, 5)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (editingAlarmId != null) "Edit Alarm" else "Set Alarm"
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTimePickers() {
        // Hour Picker (1-12)
        binding.hourPicker.apply {
            minValue = 1
            maxValue = 12
            value = selectedHour
            wrapSelectorWheel = true
            setFormatter { String.format(Locale.getDefault(), "%02d", it) }
            setOnValueChangedListener { _, _, newVal ->
                selectedHour = newVal
            }
        }

        // Minute Picker (0-59)
        binding.minutePicker.apply {
            minValue = 0
            maxValue = 59
            value = selectedMinute
            wrapSelectorWheel = true
            setFormatter { String.format(Locale.getDefault(), "%02d", it) }
            setOnValueChangedListener { _, _, newVal ->
                selectedMinute = newVal
            }
        }

        // AM/PM Picker
        binding.amPmPicker.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("AM", "PM")
            value = if (isAm) 0 else 1
            wrapSelectorWheel = true
            setOnValueChangedListener { _, _, newVal ->
                isAm = newVal == 0
            }
        }
    }

    private fun setupUI() {
        // Load existing data if editing
        intent?.let { intent ->
            binding.etLabel.setText(intent.getStringExtra(EXTRA_LABEL) ?: "")
            binding.switchAutoDisableWeekends.isChecked =
                intent.getBooleanExtra(EXTRA_AUTO_DISABLE_WEEKENDS, true)
            binding.switchAutoDisableHolidays.isChecked =
                intent.getBooleanExtra(EXTRA_AUTO_DISABLE_HOLIDAYS, true)
        }

        // Repeat type spinner
        val repeatTypes = listOf("Once", "Daily", "Weekdays")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, repeatTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRepeatType.adapter = adapter

        // Set selected repeat type
        binding.spinnerRepeatType.setSelection(when (selectedRepeatType) {
            RepeatType.ONCE -> 0
            RepeatType.DAILY -> 1
            RepeatType.WEEKDAYS -> 2
        })

        binding.spinnerRepeatType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 2) { // Weekdays
                    binding.dayPickerContainer.visibility = View.VISIBLE
                } else {
                    binding.dayPickerContainer.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupDayButtons()

        // Ringtone selection
        binding.tvSelectedRingtone.text = selectedRingtoneName
        binding.btnSelectRingtone.setOnClickListener {
            openAudioFilePicker()
        }

        setupDurationSpinners()

        // Save button
        binding.btnSave.setOnClickListener {
            saveAlarm()
        }
    }

    private fun setupDurationSpinners() {
        val snoozeOptions = listOf(1, 3, 5, 10, 15, 30)
        val snoozeDisplay = snoozeOptions.map { "$it min" }
        val snoozeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, snoozeDisplay)
        snoozeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSnooze.adapter = snoozeAdapter
        binding.spinnerSnooze.setSelection(snoozeOptions.indexOf(selectedSnoozeMinutes).coerceAtLeast(0))

        val ringOptions = listOf(1, 3, 5, 10, 15, 30)
        val ringDisplay = ringOptions.map { "$it min" }
        val ringAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ringDisplay)
        ringAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRingDuration.adapter = ringAdapter
        binding.spinnerRingDuration.setSelection(ringOptions.indexOf(selectedRingMinutes).coerceAtLeast(0))
    }

    private fun setupDayButtons() {
        val buttons = listOf(
            binding.btnMon to 1,
            binding.btnTue to 2,
            binding.btnWed to 3,
            binding.btnThu to 4,
            binding.btnFri to 5,
            binding.btnSat to 6,
            binding.btnSun to 7
        )

        buttons.forEach { (button, dayIndex) ->
            updateDayButtonUI(button, selectedDays.contains(dayIndex))
            button.setOnClickListener {
                toggleDaySelection(dayIndex, button)
            }
        }
    }

    private fun toggleDaySelection(dayIndex: Int, button: MaterialButton) {
        if (selectedDays.contains(dayIndex)) {
            selectedDays.remove(dayIndex)
            updateDayButtonUI(button, false)
        } else {
            selectedDays.add(dayIndex)
            updateDayButtonUI(button, true)
        }
    }

    private fun updateDayButtonUI(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_indigo))
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
            button.strokeWidth = 0
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_surfaceVariant))
            button.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant))
            button.strokeColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_outline)
            button.strokeWidth = 1
        }
    }

    private fun get24HourFormat(): Int {
        val hour12 = binding.hourPicker.value
        val isPm = binding.amPmPicker.value == 1

        return when {
            hour12 == 12 && !isPm -> 0  // 12 AM = 0
            hour12 == 12 && isPm -> 12  // 12 PM = 12
            isPm -> hour12 + 12         // PM hours
            else -> hour12              // AM hours
        }
    }

    private fun openAudioFilePicker() {
        try {
            audioFilePicker.launch(arrayOf("audio/*"))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Unable to open file picker",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleSelectedAudioFile(uri: Uri) {
        try {
            // Take persistable permission to access this file later
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            selectedRingtoneUri = uri.toString()
            selectedRingtoneName = getRingtoneName(uri.toString())
            binding.tvSelectedRingtone.text = selectedRingtoneName

            Toast.makeText(
                this,
                "Ringtone selected: $selectedRingtoneName",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error selecting ringtone: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getRingtoneName(uriString: String): String {
        return when (uriString) {
            "default" -> "Default alarm sound"
            else -> {
                try {
                    val uri = Uri.parse(uriString)
                    val cursor = contentResolver.query(
                        uri,
                        null,
                        null,
                        null,
                        null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                return it.getString(nameIndex) ?: "Custom ringtone"
                            }
                        }
                    }
                    "Custom ringtone"
                } catch (e: Exception) {
                    "Custom ringtone"
                }
            }
        }
    }

    private fun saveAlarm() {
        val hour24 = get24HourFormat()
        val minute = binding.minutePicker.value

        val alarm = Alarm(
            id = editingAlarmId ?: 0,
            hour = hour24,
            minute = minute,
            isEnabled = true,
            repeatType = when (binding.spinnerRepeatType.selectedItemPosition) {
                0 -> RepeatType.ONCE
                1 -> RepeatType.DAILY
                2 -> RepeatType.WEEKDAYS
                else -> RepeatType.ONCE
            },
            selectedDays = if (binding.spinnerRepeatType.selectedItemPosition == 2) {
                if (selectedDays.isEmpty()) setOf(1, 2, 3, 4, 5) else selectedDays.toSet()
            } else {
                emptySet()
            },
            label = binding.etLabel.text.toString(),
            soundUri = selectedRingtoneUri,
            snoozeDuration = when(binding.spinnerSnooze.selectedItemPosition) {
                0 -> 1; 1 -> 3; 2 -> 5; 3 -> 10; 4 -> 15; 5 -> 30; else -> 5
            },
            ringDuration = when(binding.spinnerRingDuration.selectedItemPosition) {
                0 -> 1; 1 -> 3; 2 -> 5; 3 -> 10; 4 -> 15; 5 -> 30; else -> 5
            },
            autoDisableOnWeekends = binding.switchAutoDisableWeekends.isChecked,
            autoDisableOnHolidays = binding.switchAutoDisableHolidays.isChecked
        )

        if (editingAlarmId != null) {
            // Update existing alarm
            viewModel.updateAlarm(alarm) { result ->
                result.onSuccess {
                    Toast.makeText(this, "Alarm updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                result.onFailure {
                    Toast.makeText(this, "Error updating alarm", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Create new alarm
            viewModel.createAlarm(alarm) { result ->
                result.onSuccess {
                    Toast.makeText(this, "Alarm created", Toast.LENGTH_SHORT).show()
                    finish()
                }
                result.onFailure {
                    Toast.makeText(this, "Error creating alarm", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
