package com.devlife.devlifeos.data.repository

import com.devlife.devlifeos.data.local.dao.TicketNoteDao
import com.devlife.devlifeos.data.local.entity.TicketNote
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing ticket notes
 */
class TicketNoteRepository(private val ticketNoteDao: TicketNoteDao) {

    fun getNoteByTicketId(ticketId: Long): Flow<TicketNote?> =
        ticketNoteDao.getNoteByTicketId(ticketId)

    suspend fun getNoteByTicketIdOnce(ticketId: Long): TicketNote? =
        ticketNoteDao.getNoteByTicketIdOnce(ticketId)

    suspend fun saveNote(note: TicketNote) =
        ticketNoteDao.insertNote(note)

    suspend fun updateNote(note: TicketNote) =
        ticketNoteDao.updateNote(note)

    suspend fun deleteNote(note: TicketNote) =
        ticketNoteDao.deleteNote(note)

    suspend fun deleteNoteByTicketId(ticketId: Long) =
        ticketNoteDao.deleteNoteByTicketId(ticketId)
}
