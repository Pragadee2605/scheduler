package com.devlife.devlifeos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.Holiday
import com.devlife.devlifeos.data.repository.HolidayRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Holiday operations
 */
class HolidayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HolidayRepository
    val allHolidays: StateFlow<List<Holiday>>

    init {
        val dao = DevLifeDatabase.getDatabase(application).holidayDao()
        repository = HolidayRepository(dao)
        allHolidays = repository.getAllHolidays()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addHoliday(holiday: Holiday) {
        viewModelScope.launch {
            repository.insertHoliday(holiday)
        }
    }

    fun addHolidays(holidays: List<Holiday>) {
        android.util.Log.d("HolidayViewModel", "addHolidays called with ${holidays.size} holidays")
        viewModelScope.launch {
            try {
                repository.insertHolidays(holidays)
                android.util.Log.d("HolidayViewModel", "✓ Insert completed for ${holidays.size} holidays (duplicates automatically skipped by database)")
                holidays.forEach {
                    android.util.Log.d("HolidayViewModel", "  → ${it.name} on ${it.date}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HolidayViewModel", "❌ Error inserting holidays", e)
            }
        }
    }

    fun deleteHoliday(holiday: Holiday) {
        viewModelScope.launch {
            repository.deleteHoliday(holiday)
        }
    }
}
