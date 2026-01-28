package com.devlife.devlifeos.data.repository

import androidx.lifecycle.LiveData
import com.devlife.devlifeos.data.local.dao.WorldClockDao
import com.devlife.devlifeos.data.local.entity.WorldClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for WorldClock operations
 */
class WorldClockRepository(private val worldClockDao: WorldClockDao) {

    val allWorldClocks: LiveData<List<WorldClock>> = worldClockDao.getAllWorldClocks()

    suspend fun insert(worldClock: WorldClock): Long {
        return withContext(Dispatchers.IO) {
            worldClockDao.insert(worldClock)
        }
    }

    suspend fun delete(worldClock: WorldClock) {
        withContext(Dispatchers.IO) {
            worldClockDao.delete(worldClock)
        }
    }

    suspend fun getCount(): Int {
        return withContext(Dispatchers.IO) {
            worldClockDao.getCount()
        }
    }

    // Get country to timezone mapping
    fun getCountryTimezoneMap(): Map<String, Pair<String, String>> {
        // Map of Country Name to (Main City, Timezone ID)
        return mapOf(
            "United States" to Pair("New York", "America/New_York"),
            "United Kingdom" to Pair("London", "Europe/London"),
            "Japan" to Pair("Tokyo", "Asia/Tokyo"),
            "Australia" to Pair("Sydney", "Australia/Sydney"),
            "United Arab Emirates" to Pair("Dubai", "Asia/Dubai"),
            "Singapore" to Pair("Singapore", "Asia/Singapore"),
            "France" to Pair("Paris", "Europe/Paris"),
            "Germany" to Pair("Berlin", "Europe/Berlin"),
            "China" to Pair("Shanghai", "Asia/Shanghai"),
            "India" to Pair("New Delhi", "Asia/Kolkata"),
            "Canada" to Pair("Toronto", "America/Toronto"),
            "Mexico" to Pair("Mexico City", "America/Mexico_City"),
            "Brazil" to Pair("São Paulo", "America/Sao_Paulo"),
            "Argentina" to Pair("Buenos Aires", "America/Buenos_Aires"),
            "Italy" to Pair("Rome", "Europe/Rome"),
            "Spain" to Pair("Madrid", "Europe/Madrid"),
            "Netherlands" to Pair("Amsterdam", "Europe/Amsterdam"),
            "Sweden" to Pair("Stockholm", "Europe/Stockholm"),
            "Russia" to Pair("Moscow", "Europe/Moscow"),
            "Thailand" to Pair("Bangkok", "Asia/Bangkok"),
            "Hong Kong" to Pair("Hong Kong", "Asia/Hong_Kong"),
            "South Korea" to Pair("Seoul", "Asia/Seoul"),
            "Indonesia" to Pair("Jakarta", "Asia/Jakarta"),
            "Philippines" to Pair("Manila", "Asia/Manila"),
            "New Zealand" to Pair("Auckland", "Pacific/Auckland"),
            "Egypt" to Pair("Cairo", "Africa/Cairo"),
            "South Africa" to Pair("Johannesburg", "Africa/Johannesburg"),
            "Nigeria" to Pair("Lagos", "Africa/Lagos"),
            "Kenya" to Pair("Nairobi", "Africa/Nairobi"),
            "Switzerland" to Pair("Zurich", "Europe/Zurich"),
            "Belgium" to Pair("Brussels", "Europe/Brussels"),
            "Austria" to Pair("Vienna", "Europe/Vienna"),
            "Poland" to Pair("Warsaw", "Europe/Warsaw"),
            "Turkey" to Pair("Istanbul", "Europe/Istanbul"),
            "Saudi Arabia" to Pair("Riyadh", "Asia/Riyadh"),
            "Israel" to Pair("Jerusalem", "Asia/Jerusalem"),
            "Malaysia" to Pair("Kuala Lumpur", "Asia/Kuala_Lumpur"),
            "Vietnam" to Pair("Hanoi", "Asia/Ho_Chi_Minh"),
            "Pakistan" to Pair("Karachi", "Asia/Karachi"),
            "Bangladesh" to Pair("Dhaka", "Asia/Dhaka"),
            "Chile" to Pair("Santiago", "America/Santiago"),
            "Colombia" to Pair("Bogotá", "America/Bogota"),
            "Peru" to Pair("Lima", "America/Lima"),
            "Venezuela" to Pair("Caracas", "America/Caracas"),
            "Portugal" to Pair("Lisbon", "Europe/Lisbon"),
            "Greece" to Pair("Athens", "Europe/Athens"),
            "Czech Republic" to Pair("Prague", "Europe/Prague"),
            "Hungary" to Pair("Budapest", "Europe/Budapest"),
            "Romania" to Pair("Bucharest", "Europe/Bucharest"),
            "Ukraine" to Pair("Kyiv", "Europe/Kiev"),
            "Norway" to Pair("Oslo", "Europe/Oslo"),
            "Denmark" to Pair("Copenhagen", "Europe/Copenhagen"),
            "Finland" to Pair("Helsinki", "Europe/Helsinki"),
            "Ireland" to Pair("Dublin", "Europe/Dublin"),
            "Iceland" to Pair("Reykjavik", "Atlantic/Reykjavik")
        )
    }
}
