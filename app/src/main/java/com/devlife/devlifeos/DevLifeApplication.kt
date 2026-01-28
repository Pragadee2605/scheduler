package com.devlife.devlifeos

import android.app.Application
import androidx.work.*
import com.devlife.devlifeos.worker.HolidaySyncWorker
import java.util.concurrent.TimeUnit

/**
 * Application class for DevLife OS
 * Initializes essential national holidays on first launch
 */
class DevLifeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize essential national holidays on first launch
        scheduleImmediateHolidaySync()

        // Schedule periodic holiday sync (once per year for new year)
        schedulePeriodicHolidaySync()
    }

    private fun scheduleImmediateHolidaySync() {
        // No network required - just loads essential holidays from hardcoded list
        val immediateWorkRequest = OneTimeWorkRequestBuilder<HolidaySyncWorker>()
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "holiday_immediate_sync",
            ExistingWorkPolicy.KEEP,
            immediateWorkRequest
        )
    }

    private fun schedulePeriodicHolidaySync() {
        // No network required - syncs essential holidays for new year
        // Sync every 30 days to catch new year changes
        val workRequest = PeriodicWorkRequestBuilder<HolidaySyncWorker>(
            30, TimeUnit.DAYS
        )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HolidaySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
