package com.devlife.devlifeos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.repository.JiraTicketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker to automatically delete completed tickets after 5 hours
 */
class TicketDeletionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ticketId = inputData.getLong("ticket_id", -1L)

            if (ticketId == -1L) {
                return@withContext Result.failure()
            }

            val database = DevLifeDatabase.getDatabase(applicationContext)
            val repository = JiraTicketRepository(database.jiraTicketDao())

            // Get the ticket and verify it's still in DONE status
            val ticket = repository.getTicketById(ticketId)

            if (ticket != null && ticket.status == com.devlife.devlifeos.data.local.entity.TicketStatus.DONE) {
                // Only delete if it's been 5 hours since completion
                val completedAt = ticket.completedAt ?: return@withContext Result.failure()
                val currentTime = System.currentTimeMillis()
                val fiveHoursInMillis = 5 * 60 * 60 * 1000L

                if (currentTime - completedAt >= fiveHoursInMillis) {
                    repository.deleteTicket(ticket)
                    return@withContext Result.success()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
