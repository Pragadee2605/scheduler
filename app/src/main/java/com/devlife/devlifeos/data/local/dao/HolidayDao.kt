package com.devlife.devlifeos.data.local.dao

import androidx.room.*
import com.devlife.devlifeos.data.local.entity.Holiday
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Holiday entity
 */
@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays WHERE year = :year ORDER BY month ASC, day ASC")
    fun getHolidaysByYear(year: Int): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays ORDER BY year DESC, month ASC, day ASC")
    fun getAllHolidays(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays WHERE date = :date")
    suspend fun getHolidayByDate(date: String): Holiday?

    @Query("SELECT * FROM holidays WHERE year = :year AND month = :month AND day = :day")
    suspend fun getHolidayByYearMonthDay(year: Int, month: Int, day: Int): Holiday?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHoliday(holiday: Holiday)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHolidays(holidays: List<Holiday>)

    @Delete
    suspend fun deleteHoliday(holiday: Holiday)

    @Query("DELETE FROM holidays WHERE year = :year")
    suspend fun deleteHolidaysByYear(year: Int)

    @Query("SELECT COUNT(*) FROM holidays WHERE year = :year")
    suspend fun getHolidayCountForYear(year: Int): Int
}
