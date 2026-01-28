package com.devlife.devlifeos.ui.alarm

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.Alarm
import com.devlife.devlifeos.data.local.entity.RepeatType
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.databinding.ActivityCreateAlarmBinding
import com.devlife.devlifeos.domain.alarm.AlarmSchedulerImpl
import java.util.*

/**
 * Full-screen Activity for creating or editing an alarm
 * Premium 2026 design with better UX
 */
class CreateAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAlarmBinding
    private lateinit var viewModel: AlarmViewModel

    private var selectedHour = 7
    private var selectedMinute = 0
    private var selectedRepeatType = RepeatType.ONCE
    private var selectedRingtoneUri: String = "default"
    private var selectedRingtoneName: String = "Default alarm sound"

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        loadIntentData()
        setupToolbar()
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
            selectedHour = intent.getIntExtra(EXTRA_HOUR, 7)
            selectedMinute = intent.getIntExtra(EXTRA_MINUTE, 0)
            intent.getStringExtra(EXTRA_REPEAT_TYPE)?.let {
                selectedRepeatType = RepeatType.valueOf(it)
            }
            selectedRingtoneUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: "default"
            selectedRingtoneName = getRingtoneName(selectedRingtoneUri)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (editingAlarmId != null) "Edit Alarm" else "New Alarm"
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        // Update time display
        updateTimeDisplay()

        // Load existing data if editing
        intent?.let { intent ->
            binding.etLabel.setText(intent.getStringExtra(EXTRA_LABEL) ?: "")
            binding.switchAutoDisableWeekends.isChecked =
                intent.getBooleanExtra(EXTRA_AUTO_DISABLE_WEEKENDS, true)
            binding.switchAutoDisableHolidays.isChecked =
                intent.getBooleanExtra(EXTRA_AUTO_DISABLE_HOLIDAYS, true)
        }

        // Time picker button
        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
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

        // Ringtone selection
        binding.tvSelectedRingtone.text = selectedRingtoneName
        binding.btnSelectRingtone.setOnClickListener {
            openAudioFilePicker()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveAlarm()
        }
    }

    private fun updateTimeDisplay() {
        val amPm = if (selectedHour < 12) "AM" else "PM"
        val displayHour = when {
            selectedHour == 0 -> 12
            selectedHour > 12 -> selectedHour - 12
            else -> selectedHour
        }
        binding.tvSelectedTime.text = String.format(Locale.getDefault(), "%02d:%02d", displayHour, selectedMinute)
        binding.tvAmPm.text = amPm
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                updateTimeDisplay()
            },
            selectedHour,
            selectedMinute,
            false  // 12-hour format with AM/PM
        ).show()
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
        val alarm = Alarm(
            id = editingAlarmId ?: 0,
            hour = selectedHour,
            minute = selectedMinute,
            isEnabled = true,
            repeatType = when (binding.spinnerRepeatType.selectedItemPosition) {
                0 -> RepeatType.ONCE
                1 -> RepeatType.DAILY
                2 -> RepeatType.WEEKDAYS
                else -> RepeatType.ONCE
            },
            selectedDays = if (binding.spinnerRepeatType.selectedItemPosition == 2) {
                setOf(1, 2, 3, 4, 5) // Mon-Fri by default
            } else {
                emptySet()
            },
            label = binding.etLabel.text.toString(),
            soundUri = selectedRingtoneUri,
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
