package com.devlife.devlifeos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.repository.HolidayRepository
import com.devlife.devlifeos.databinding.FragmentHolidayListBinding
import com.devlife.devlifeos.worker.HolidaySyncWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for displaying India government holidays
 */
class HolidayListFragment : Fragment() {

    private var _binding: FragmentHolidayListBinding? = null
    private val binding get() = _binding!!

    private lateinit var holidayRepository: HolidayRepository
    private lateinit var adapter: HolidayAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHolidayListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRepository()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    // ...existing code...

    private fun setupFab() {
        binding.fabSyncHolidays.setOnClickListener {
            syncHolidays()
        }

        // Add button for company holidays
        binding.fabAddCompanyHoliday.setOnClickListener {
            showAddCompanyHolidayDialog()
        }
    }

    private fun showAddCompanyHolidayDialog() {
        val intent = Intent(requireContext(), AddCompanyHolidayActivity::class.java)
        startActivity(intent)
    }

    private fun syncHolidays() {
        // Check if binding is still valid
        if (_binding == null) return

        // Show progress message
        Snackbar.make(binding.root, "Syncing holidays...", Snackbar.LENGTH_SHORT).show()

        // Trigger immediate holiday sync with force refresh
        val workData = androidx.work.Data.Builder()
            .putBoolean("force_refresh", true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<HolidaySyncWorker>()
            .setInputData(workData)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(workRequest)

        // Show success message after a short delay
        binding.root.postDelayed({
            // Check again if binding is still valid before showing snackbar
            _binding?.let {
                Snackbar.make(it.root, "Holidays synced successfully!", Snackbar.LENGTH_SHORT).show()
            }
        }, 1500)
    }

    private fun setupRepository() {
        val database = DevLifeDatabase.getDatabase(requireContext())
        holidayRepository = HolidayRepository(database.holidayDao())
    }

    private fun setupRecyclerView() {
        adapter = HolidayAdapter()

        binding.recyclerViewHolidays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HolidayListFragment.adapter
        }
    }

    private fun observeData() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        viewLifecycleOwner.lifecycleScope.launch {
            holidayRepository.getHolidaysByYear(currentYear).collectLatest { holidays ->
                adapter.submitList(holidays)
                binding.emptyView.visibility = if (holidays.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
