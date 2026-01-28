package com.devlife.devlifeos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.domain.alarm.AlarmSchedulerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling device boot
 * Reschedules all alarms after reboot
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = DevLifeDatabase.getDatabase(context)
                val alarmRepository = AlarmRepository(database.alarmDao())
                val alarmScheduler = AlarmSchedulerImpl(context, alarmRepository)

                // Reschedule all enabled alarms
                alarmScheduler.rescheduleAllAlarms()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
