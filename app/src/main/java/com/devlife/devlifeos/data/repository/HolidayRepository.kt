package com.devlife.devlifeos.data.repository

import com.devlife.devlifeos.data.local.dao.HolidayDao
import com.devlife.devlifeos.data.local.entity.Holiday
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing India government holidays
 */
class HolidayRepository(private val holidayDao: HolidayDao) {

    fun getHolidaysByYear(year: Int): Flow<List<Holiday>> =
        holidayDao.getHolidaysByYear(year)

    fun getAllHolidays(): Flow<List<Holiday>> =
        holidayDao.getAllHolidays()

    suspend fun getHolidayByDate(date: String): Holiday? =
        holidayDao.getHolidayByDate(date)

    suspend fun getHolidayByYearMonthDay(year: Int, month: Int, day: Int): Holiday? =
        holidayDao.getHolidayByYearMonthDay(year, month, day)

    suspend fun insertHoliday(holiday: Holiday) =
        holidayDao.insertHoliday(holiday)

    suspend fun insertHolidays(holidays: List<Holiday>) =
        holidayDao.insertHolidays(holidays)

    suspend fun deleteHoliday(holiday: Holiday) =
        holidayDao.deleteHoliday(holiday)

    suspend fun deleteHolidaysByYear(year: Int) =
        holidayDao.deleteHolidaysByYear(year)

    suspend fun getHolidayCountForYear(year: Int): Int =
        holidayDao.getHolidayCountForYear(year)

    suspend fun isHoliday(year: Int, month: Int, day: Int): Boolean {
        return getHolidayByYearMonthDay(year, month, day) != null
    }
}
