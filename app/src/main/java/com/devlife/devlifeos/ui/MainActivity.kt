package com.devlife.devlifeos.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.devlife.devlifeos.R
import com.devlife.devlifeos.databinding.ActivityMainBinding
import com.devlife.devlifeos.ui.alarm.AlarmListFragment
import com.devlife.devlifeos.ui.tickets.TicketListFragment

/**
 * Main activity with bottom navigation
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var permissionsChecked = false

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys
        if (deniedPermissions.isNotEmpty()) {
            showPermissionDeniedDialog(deniedPermissions.toList())
        } else {
            permissionsChecked = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup bottom navigation first (no heavy operations)
        setupBottomNavigation()

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(AlarmListFragment())
        }

        // Check permissions after UI is ready - post to avoid blocking initial render
        binding.root.post {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check if any permissions are missing
        val missingPermissions = mutableListOf<String>()

        // Check for missing runtime permissions
        if (requiredPermissions.isNotEmpty()) {
            missingPermissions.addAll(requiredPermissions)
        }

        // Check special permissions that require different handling
        val specialPermissionsNeeded = mutableListOf<String>()

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                specialPermissionsNeeded.add("Exact Alarm Scheduling")
            }
        }

        // Full screen intent permission (Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.USE_FULL_SCREEN_INTENT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                specialPermissionsNeeded.add("Full Screen Intent (for alarm screen)")
            }
        }

        // If there are missing permissions, show dialog
        if (missingPermissions.isNotEmpty() || specialPermissionsNeeded.isNotEmpty()) {
            showPermissionRequestDialog(missingPermissions, specialPermissionsNeeded)
        } else {
            permissionsChecked = true
        }
    }

    private fun showPermissionRequestDialog(
        runtimePermissions: List<String>,
        specialPermissions: List<String>
    ) {
        val message = buildString {
            append("This app requires the following permissions to function properly:\n\n")

            if (runtimePermissions.isNotEmpty()) {
                append("• Notifications - To show alarm notifications\n")
            }

            if (specialPermissions.isNotEmpty()) {
                specialPermissions.forEach { permission ->
                    append("• $permission\n")
                }
            }

            append("\nPlease grant these permissions to use the app.")
        }

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ ->
                if (runtimePermissions.isNotEmpty()) {
                    permissionLauncher.launch(runtimePermissions.toTypedArray())
                }

                // Handle special permissions
                if (specialPermissions.isNotEmpty()) {
                    handleSpecialPermissions()
                }
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun handleSpecialPermissions() {
        // For Android 12+, open exact alarm settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to app settings
                    openAppSettings()
                }
            }
        }
    }

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val message = buildString {
            append("Some permissions were denied:\n\n")
            deniedPermissions.forEach { permission ->
                when (permission) {
                    Manifest.permission.POST_NOTIFICATIONS ->
                        append("• Notifications - You won't receive alarm notifications\n")
                }
            }
            append("\nYou can grant these permissions later in Settings.")
        }

        AlertDialog.Builder(this)
            .setTitle("Permissions Denied")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continue Anyway") { _, _ ->
                permissionsChecked = true
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_alarms -> {
                    loadFragment(AlarmListFragment())
                    true
                }
                R.id.nav_time_converter -> {
                    loadFragment(com.devlife.devlifeos.ui.timeconverter.TimeConverterFragment())
                    true
                }
                R.id.nav_world_clock -> {
                    loadFragment(com.devlife.devlifeos.ui.worldclock.WorldClockFragment())
                    true
                }
                R.id.nav_tickets -> {
                    loadFragment(TicketListFragment())
                    true
                }
                R.id.nav_holidays -> {
                    loadFragment(HolidayListFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()
    }
}
