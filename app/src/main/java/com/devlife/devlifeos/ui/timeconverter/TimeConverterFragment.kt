package com.devlife.devlifeos.ui.timeconverter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devlife.devlifeos.databinding.FragmentTimeConverterBinding
import com.devlife.devlifeos.util.TimeFormatUtil
import java.util.*

/**
 * Time Converter Fragment - Convert 24-hour (Railway Time) to 12-hour format
 * A practical tool for quick time conversion
 */
class TimeConverterFragment : Fragment() {

    private var _binding: FragmentTimeConverterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeConverterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {

        // Convert button
        binding.btnConvert.setOnClickListener {
            convertTime()
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            clearInputs()
        }
    }

    private fun convertTime() {
        val hourText = binding.etHour24.text.toString()
        val minuteText = binding.etMinute.text.toString()

        if (hourText.isEmpty() || minuteText.isEmpty()) {
            clearResult()
            return
        }

        try {
            val hour24 = hourText.toInt()
            val minute = minuteText.toInt()

            // Validate input
            if (hour24 !in 0..23) {
                showError("Hour must be between 0-23")
                clearResult()
                return
            }

            if (minute !in 0..59) {
                showError("Minute must be between 0-59")
                clearResult()
                return
            }

            // Convert and display
            val formattedTime = TimeFormatUtil.convert24HourTo12Hour(hour24, minute)

            // Show result
            binding.tvResult12Hour.text = String.format(
                Locale.getDefault(),
                "%02d:%02d %s",
                formattedTime.hour12,
                formattedTime.minute,
                formattedTime.amPm
            )

            binding.tvResultExplanation.text = buildExplanation(hour24, minute, formattedTime)

            // Show success animation
            binding.cardResult.alpha = 0f
            binding.cardResult.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

        } catch (e: NumberFormatException) {
            showError("Please enter valid numbers")
            clearResult()
        }
    }

    private fun buildExplanation(hour24: Int, minute: Int, formattedTime: TimeFormatUtil.FormattedTime): String {
        val explanation = StringBuilder()

        explanation.append("🕐 Conversion:\n\n")
        explanation.append("24-Hour: ${String.format("%02d:%02d", hour24, minute)}\n\n")

        // Explain the conversion logic
        when {
            hour24 == 0 -> {
                explanation.append("✓ 00:00 = Midnight\n")
                explanation.append("✓ Becomes 12:00 AM\n")
            }
            hour24 < 12 -> {
                explanation.append("✓ Morning hours (AM)\n")
                explanation.append("✓ Hour stays: $hour24\n")
            }
            hour24 == 12 -> {
                explanation.append("✓ 12:00 = Noon\n")
                explanation.append("✓ Stays 12:00 PM\n")
            }
            else -> {
                explanation.append("✓ Afternoon/Evening (PM)\n")
                explanation.append("✓ Subtract 12: $hour24 - 12 = ${hour24 - 12}\n")
            }
        }

        explanation.append("\n12-Hour: ")
        explanation.append(String.format("%02d:%02d %s", formattedTime.hour12, minute, formattedTime.amPm))

        return explanation.toString()
    }


    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun clearResult() {
        binding.tvResult12Hour.text = "--:-- --"
        binding.tvResultExplanation.text = "Enter time above to see conversion"
        binding.cardExplanation.visibility = View.GONE
    }

    private fun clearInputs() {
        binding.etHour24.text?.clear()
        binding.etMinute.text?.clear()
        clearResult()
        binding.etHour24.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
