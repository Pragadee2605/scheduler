package com.devlife.devlifeos.data.local.dao

import androidx.room.*
import com.devlife.devlifeos.data.local.entity.TicketNote
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TicketNote entity
 */
@Dao
interface TicketNoteDao {

    @Query("SELECT * FROM ticket_notes WHERE ticketId = :ticketId")
    fun getNoteByTicketId(ticketId: Long): Flow<TicketNote?>

    @Query("SELECT * FROM ticket_notes WHERE ticketId = :ticketId")
    suspend fun getNoteByTicketIdOnce(ticketId: Long): TicketNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TicketNote)

    @Update
    suspend fun updateNote(note: TicketNote)

    @Delete
    suspend fun deleteNote(note: TicketNote)

    @Query("DELETE FROM ticket_notes WHERE ticketId = :ticketId")
    suspend fun deleteNoteByTicketId(ticketId: Long)
}
