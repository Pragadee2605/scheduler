package com.devlife.devlifeos.ui.worldclock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devlife.devlifeos.R
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
            viewModel.deleteWorldClock(worldClock)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

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
                    val worldClock = adapter.currentList[position]
                    viewModel.deleteWorldClock(worldClock)
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
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
