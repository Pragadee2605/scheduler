package com.devlife.devlifeos.ui.worldclock

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.devlife.devlifeos.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog for searching and adding a world clock by country
 */
class SearchCountryDialogFragment : DialogFragment() {

    private lateinit var viewModel: WorldClockViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(requireParentFragment())[WorldClockViewModel::class.java]

        val view = layoutInflater.inflate(R.layout.dialog_search_country, null)

        val autoCompleteCountry = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteCountry)
        val btnSearch = view.findViewById<MaterialButton>(R.id.btnSearch)

        // Setup country autocomplete
        val countries = viewModel.getCountryList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countries)
        autoCompleteCountry.setAdapter(adapter)
        autoCompleteCountry.threshold = 1

        // Search button click
        btnSearch.setOnClickListener {
            val countryName = autoCompleteCountry.text.toString().trim()

            if (countryName.isEmpty()) {
                autoCompleteCountry.error = "Please enter a country name"
                return@setOnClickListener
            }

            if (!countries.contains(countryName)) {
                autoCompleteCountry.error = "Country not found. Please select from suggestions."
                return@setOnClickListener
            }

            viewModel.addWorldClockByCountry(countryName)
            dismiss()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Search Country")
            .setView(view)
            .setNegativeButton("Cancel", null)
            .create()

        return dialog
    }
}
