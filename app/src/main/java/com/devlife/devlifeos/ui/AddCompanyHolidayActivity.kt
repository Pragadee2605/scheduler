package com.devlife.devlifeos.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.devlife.devlifeos.R
import com.devlife.devlifeos.data.local.entity.Holiday
import com.devlife.devlifeos.data.local.entity.HolidayType
import com.devlife.devlifeos.databinding.ActivityAddCompanyHolidayBinding
import com.devlife.devlifeos.utils.HolidayOcrHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*

/**
 * Activity for adding company holidays manually or via image upload
 */
class AddCompanyHolidayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCompanyHolidayBinding
    private lateinit var viewModel: HolidayViewModel
    private lateinit var ocrHelper: HolidayOcrHelper
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                processImageForHolidays(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCompanyHolidayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewModel()
        setupListeners()
        updateDateDisplay()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Add Company Holiday"
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[HolidayViewModel::class.java]
        ocrHelper = HolidayOcrHelper(this)
    }

    private fun setupListeners() {
        // Date picker
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Upload calendar image
        binding.btnUploadCalendar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        // Add manually
        binding.btnAddManually.setOnClickListener {
            addHolidayManually()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        val dateStr = String.format(
            Locale.US,
            "%02d/%02d/%04d",
            selectedDate.get(Calendar.DAY_OF_MONTH),
            selectedDate.get(Calendar.MONTH) + 1,
            selectedDate.get(Calendar.YEAR)
        )
        binding.etDate.setText(dateStr)
    }

    private fun addHolidayManually() {
        val holidayName = binding.etHolidayName.text.toString().trim()

        if (holidayName.isEmpty()) {
            binding.etHolidayName.error = "Please enter holiday name"
            return
        }

        val holiday = Holiday(
            name = holidayName,
            date = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ),
            year = selectedDate.get(Calendar.YEAR),
            month = selectedDate.get(Calendar.MONTH) + 1,
            day = selectedDate.get(Calendar.DAY_OF_MONTH),
            type = HolidayType.COMPANY
        )

        viewModel.addHoliday(holiday)
        Toast.makeText(this, "Holiday added successfully", Toast.LENGTH_SHORT).show()

        // Clear fields
        binding.etHolidayName.text?.clear()
        selectedDate = Calendar.getInstance()
        updateDateDisplay()
    }

    private fun processImageForHolidays(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.scanning_calendar_image)

        // Disable buttons during processing
        setButtonsEnabled(false)

        lifecycleScope.launch {
            try {
                val holidays = ocrHelper.extractHolidaysFromImage(uri)

                binding.progressBar.visibility = View.GONE
                binding.tvStatus.visibility = View.GONE
                setButtonsEnabled(true)

                if (holidays.isEmpty()) {
                    Toast.makeText(
                        this@AddCompanyHolidayActivity,
                        "No holidays found in image. Please try again or add manually.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    showExtractedHolidays(holidays)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.visibility = View.GONE
                setButtonsEnabled(true)
                Toast.makeText(
                    this@AddCompanyHolidayActivity,
                    "Error processing image: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnSelectDate.isEnabled = enabled
        binding.btnAddManually.isEnabled = enabled
        binding.btnUploadCalendar.isEnabled = enabled
    }

    private fun showExtractedHolidays(holidays: List<Holiday>) {
        lifecycleScope.launch {
            val existingHolidays = viewModel.allHolidays.value
            val existingDates = existingHolidays.map { it.date }.toSet()

            // Filter out duplicates
            val newHolidays = holidays.filter { it.date !in existingDates }
            val duplicates = holidays.filter { it.date in existingDates }

            if (duplicates.isNotEmpty()) {
                android.util.Log.d("AddCompanyHoliday", "Skipping ${duplicates.size} duplicate holidays:")
                duplicates.forEach {
                    android.util.Log.d("AddCompanyHoliday", "  - ${it.name} on ${it.date} (already exists)")
                }
            }

            if (newHolidays.isEmpty()) {
                Toast.makeText(
                    this@AddCompanyHolidayActivity,
                    "All ${holidays.size} holidays already exist in the calendar!",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val holidayNames = newHolidays.map { "${it.name} - ${it.date}" }.toTypedArray()
            val checkedStates = BooleanArray(newHolidays.size) { true } // All selected by default

            val message = if (duplicates.isNotEmpty()) {
                "Found ${newHolidays.size} new holidays (${duplicates.size} already exist)\n\n✓ All holidays are selected by default"
            } else {
                "✓ All holidays are selected by default"
            }

            MaterialAlertDialogBuilder(this@AddCompanyHolidayActivity)
                .setTitle("Found ${newHolidays.size} New Holidays")
                .setMessage(message)
                .setMultiChoiceItems(holidayNames, checkedStates) { _, which, isChecked ->
                    checkedStates[which] = isChecked
                }
                .setPositiveButton("Add Selected") { _, _ ->
                    val selectedHolidays = newHolidays.filterIndexed { index, _ ->
                        checkedStates[index]
                    }

                    if (selectedHolidays.isNotEmpty()) {
                        viewModel.addHolidays(selectedHolidays)
                        Toast.makeText(
                            this@AddCompanyHolidayActivity,
                            "✓ ${selectedHolidays.size} holidays added successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // Close activity after adding
                    } else {
                        Toast.makeText(
                            this@AddCompanyHolidayActivity,
                            "No holidays selected",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrHelper.close()
    }
}
