package com.devlife.devlifeos.data.local.dao

import androidx.room.*
import com.devlife.devlifeos.data.local.entity.WorkLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WorkLog entity
 */
@Dao
interface WorkLogDao {

    @Query("SELECT * FROM work_logs WHERE ticketId = :ticketId ORDER BY date DESC")
    fun getWorkLogsByTicketId(ticketId: Long): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE id = :workLogId")
    suspend fun getWorkLogById(workLogId: Long): WorkLog?

    @Query("SELECT * FROM work_logs WHERE ticketId = :ticketId AND date = :date")
    suspend fun getWorkLogByTicketAndDate(ticketId: Long, date: String): WorkLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLog(workLog: WorkLog): Long

    @Update
    suspend fun updateWorkLog(workLog: WorkLog)

    @Delete
    suspend fun deleteWorkLog(workLog: WorkLog)

    @Query("DELETE FROM work_logs WHERE ticketId = :ticketId")
    suspend fun deleteWorkLogsByTicketId(ticketId: Long)
}
