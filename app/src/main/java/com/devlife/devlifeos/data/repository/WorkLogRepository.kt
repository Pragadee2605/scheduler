package com.devlife.devlifeos.data.repository

import com.devlife.devlifeos.data.local.dao.WorkLogDao
import com.devlife.devlifeos.data.local.entity.WorkLog
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing work logs
 */
class WorkLogRepository(private val workLogDao: WorkLogDao) {

    fun getWorkLogsByTicketId(ticketId: Long): Flow<List<WorkLog>> =
        workLogDao.getWorkLogsByTicketId(ticketId)

    suspend fun getWorkLogById(workLogId: Long): WorkLog? =
        workLogDao.getWorkLogById(workLogId)

    suspend fun getWorkLogByTicketAndDate(ticketId: Long, date: String): WorkLog? =
        workLogDao.getWorkLogByTicketAndDate(ticketId, date)

    suspend fun insertWorkLog(workLog: WorkLog): Long =
        workLogDao.insertWorkLog(workLog)

    suspend fun updateWorkLog(workLog: WorkLog) =
        workLogDao.updateWorkLog(workLog)

    suspend fun deleteWorkLog(workLog: WorkLog) =
        workLogDao.deleteWorkLog(workLog)

    suspend fun deleteWorkLogsByTicketId(ticketId: Long) =
        workLogDao.deleteWorkLogsByTicketId(ticketId)
}
