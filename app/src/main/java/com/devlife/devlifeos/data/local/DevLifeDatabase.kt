package com.devlife.devlifeos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devlife.devlifeos.data.local.converter.Converters
import com.devlife.devlifeos.data.local.dao.*
import com.devlife.devlifeos.data.local.entity.*

/**
 * Room Database for DevLife OS
 */
@Database(
    entities = [
        Alarm::class,
        Holiday::class,
        JiraTicket::class,
        WorkLog::class,
        TicketReminder::class,
        TicketNote::class,
        WorldClock::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DevLifeDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun holidayDao(): HolidayDao
    abstract fun jiraTicketDao(): JiraTicketDao
    abstract fun workLogDao(): WorkLogDao
    abstract fun ticketReminderDao(): TicketReminderDao
    abstract fun ticketNoteDao(): TicketNoteDao
    abstract fun worldClockDao(): WorldClockDao

    companion object {
        @Volatile
        private var INSTANCE: DevLifeDatabase? = null

        fun getDatabase(context: Context): DevLifeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevLifeDatabase::class.java,
                    "devlife_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
