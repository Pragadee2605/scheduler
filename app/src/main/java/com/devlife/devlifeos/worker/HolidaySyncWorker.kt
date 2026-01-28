package com.devlife.devlifeos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.Holiday
import com.devlife.devlifeos.data.local.entity.HolidayType
import com.devlife.devlifeos.data.repository.HolidayRepository
import java.util.Calendar

/**
 * Worker for syncing India government holidays
 * Provides essential national holidays as fallback
 * Users should primarily use OCR to upload company calendars
 */
class HolidaySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val holidayRepository: HolidayRepository by lazy {
        val database = DevLifeDatabase.getDatabase(applicationContext)
        HolidayRepository(database.holidayDao())
    }

    override suspend fun doWork(): Result {
        return try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val forceRefresh = inputData.getBoolean("force_refresh", false)

            // Check if holidays already exist for this year (unless force refresh)
            if (!forceRefresh) {
                val existingCount = holidayRepository.getHolidayCountForYear(currentYear)
                if (existingCount > 0) {
                    return Result.success()
                }
            }

            // Delete existing holidays if force refresh
            if (forceRefresh) {
                holidayRepository.deleteHolidaysByYear(currentYear)
            }

            // Insert essential national holidays
            val holidays = getEssentialHolidays(currentYear)
            holidayRepository.insertHolidays(holidays)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    /**
     * Essential fixed national holidays
     * Users should upload company calendars via OCR for complete holiday list
     */
    private fun getEssentialHolidays(year: Int): List<Holiday> {
        // Only fixed national holidays that don't change dates
        return listOf(
            Holiday(
                name = "Republic Day",
                date = "$year-01-26",
                year = year,
                month = 1,
                day = 26,
                type = HolidayType.NATIONAL
            ),
            Holiday(
                name = "Independence Day",
                date = "$year-08-15",
                year = year,
                month = 8,
                day = 15,
                type = HolidayType.NATIONAL
            ),
            Holiday(
                name = "Gandhi Jayanti",
                date = "$year-10-02",
                year = year,
                month = 10,
                day = 2,
                type = HolidayType.NATIONAL
            ),
            Holiday(
                name = "Christmas",
                date = "$year-12-25",
                year = year,
                month = 12,
                day = 25,
                type = HolidayType.NATIONAL
            )
        )
    }

    companion object {
        const val WORK_NAME = "holiday_sync_worker"
    }
}
