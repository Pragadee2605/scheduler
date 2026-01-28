package com.devlife.devlifeos.ui.worldclock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devlife.devlifeos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Fragment displaying world clocks for different timezones
 */
class WorldClockFragment : Fragment() {

    private lateinit var viewModel: WorldClockViewModel
    private lateinit var adapter: WorldClockAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddClock: FloatingActionButton

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            adapter.notifyTimeChanged()
            handler.postDelayed(this, 1000) // Update every second
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_world_clock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[WorldClockViewModel::class.java]

        recyclerView = view.findViewById(R.id.rvWorldClocks)
        fabAddClock = view.findViewById(R.id.fabAddClock)

        setupRecyclerView()
        setupFab()
        observeWorldClocks()

        // Start updating time
        handler.post(updateTimeRunnable)
    }

    private fun setupRecyclerView() {
        adapter = WorldClockAdapter { worldClock ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Clock")
                .setMessage("Remove ${worldClock.countryName} from world clocks?")
                .setPositiveButton("Remove") { _, _ ->
                    viewModel.deleteWorldClock(worldClock)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        fabAddClock.setOnClickListener {
            showSearchCountryDialog()
        }
    }

    private fun observeWorldClocks() {
        viewModel.allWorldClocks.observe(viewLifecycleOwner) { clocks ->
            adapter.submitList(clocks)
        }
    }

    private fun showSearchCountryDialog() {
        val dialog = SearchCountryDialogFragment()
        dialog.show(childFragmentManager, "SearchCountryDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeRunnable)
    }
}
