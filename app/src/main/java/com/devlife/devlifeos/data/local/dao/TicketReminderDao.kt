package com.devlife.devlifeos.data.local.dao

import androidx.room.*
import com.devlife.devlifeos.data.local.entity.TicketReminder
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TicketReminder entity
 */
@Dao
interface TicketReminderDao {

    @Query("SELECT * FROM ticket_reminders WHERE ticketId = :ticketId ORDER BY reminderTime ASC")
    fun getRemindersByTicketId(ticketId: Long): Flow<List<TicketReminder>>

    @Query("SELECT * FROM ticket_reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): TicketReminder?

    @Query("SELECT * FROM ticket_reminders WHERE isEnabled = 1 AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    suspend fun getUpcomingReminders(currentTime: Long): List<TicketReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TicketReminder): Long

    @Update
    suspend fun updateReminder(reminder: TicketReminder)

    @Delete
    suspend fun deleteReminder(reminder: TicketReminder)

    @Query("DELETE FROM ticket_reminders WHERE ticketId = :ticketId")
    suspend fun deleteRemindersByTicketId(ticketId: Long)
}
