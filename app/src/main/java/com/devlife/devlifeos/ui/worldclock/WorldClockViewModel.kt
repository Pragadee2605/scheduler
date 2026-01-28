package com.devlife.devlifeos.ui.worldclock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.WorldClock
import com.devlife.devlifeos.data.repository.WorldClockRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for World Clock feature
 */
class WorldClockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorldClockRepository
    val allWorldClocks: LiveData<List<WorldClock>>

    init {
        val dao = DevLifeDatabase.getDatabase(application).worldClockDao()
        repository = WorldClockRepository(dao)
        allWorldClocks = repository.allWorldClocks
    }

    fun getCountryList(): List<String> {
        return repository.getCountryTimezoneMap().keys.sorted()
    }

    fun addWorldClockByCountry(countryName: String) {
        viewModelScope.launch {
            val countryMap = repository.getCountryTimezoneMap()
            val cityAndTimezone = countryMap[countryName]

            if (cityAndTimezone != null) {
                val (mainCity, timezoneId) = cityAndTimezone
                val worldClock = WorldClock(
                    countryName = countryName,
                    mainCity = mainCity,
                    timeZoneId = timezoneId
                )
                repository.insert(worldClock)
            }
        }
    }

    fun deleteWorldClock(worldClock: WorldClock) {
        viewModelScope.launch {
            repository.delete(worldClock)
        }
    }
}
