package com.devlife.devlifeos.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.devlife.devlifeos.data.local.entity.WorldClock

/**
 * DAO for WorldClock operations
 */
@Dao
interface WorldClockDao {

    @Query("SELECT * FROM world_clocks ORDER BY countryName ASC")
    fun getAllWorldClocks(): LiveData<List<WorldClock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(worldClock: WorldClock): Long

    @Delete
    suspend fun delete(worldClock: WorldClock)

    @Query("DELETE FROM world_clocks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM world_clocks")
    suspend fun getCount(): Int
}
