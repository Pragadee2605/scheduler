package com.devlife.devlifeos.data.repository

import com.devlife.devlifeos.data.local.dao.TicketReminderDao
import com.devlife.devlifeos.data.local.entity.TicketReminder
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing ticket reminders
 */
class TicketReminderRepository(private val ticketReminderDao: TicketReminderDao) {

    fun getRemindersByTicketId(ticketId: Long): Flow<List<TicketReminder>> =
        ticketReminderDao.getRemindersByTicketId(ticketId)

    suspend fun getReminderById(reminderId: Long): TicketReminder? =
        ticketReminderDao.getReminderById(reminderId)

    suspend fun getUpcomingReminders(currentTime: Long): List<TicketReminder> =
        ticketReminderDao.getUpcomingReminders(currentTime)

    suspend fun insertReminder(reminder: TicketReminder): Long =
        ticketReminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: TicketReminder) =
        ticketReminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: TicketReminder) =
        ticketReminderDao.deleteReminder(reminder)

    suspend fun deleteRemindersByTicketId(ticketId: Long) =
        ticketReminderDao.deleteRemindersByTicketId(ticketId)
}
