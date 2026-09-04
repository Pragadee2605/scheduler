package com.devlife.devlifeos.service

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.devlife.devlifeos.R
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.ui.alarm.AlarmRingingActivity
import kotlinx.coroutines.*
import androidx.core.net.toUri

/**
 * Foreground service for playing alarm sound with fade-in
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentAlarmId: Long = -1
    private var timeoutJob: Job? = null
    
    private val activeAlarmIds = mutableSetOf<Long>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1) ?: -1
        
        // Handle dismiss action (manual from activity or notification)
        if (intent?.action == ACTION_DISMISS_ALARM) {
            if (alarmId != -1L) {
                // If dismissing from notification, we need to make sure the activity also closes
                // and the ONCE alarm is disabled.
                val timeoutIntent = Intent(ACTION_ALARM_TIMEOUT).apply {
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    setPackage(packageName)
                }
                sendBroadcast(timeoutIntent)
                activeAlarmIds.remove(alarmId)
            }
            if (activeAlarmIds.isEmpty()) {
                stopAlarm()
            } else if (alarmId == currentAlarmId) {
                // If we dismissed the currently playing alarm, play the previous one in stack
                val nextId = activeAlarmIds.last()
                currentAlarmId = nextId
                updateNotification(nextId)
                serviceScope.launch {
                    playAlarm(nextId)
                }
            }
            return START_NOT_STICKY
        }

        if (alarmId == -1L) {
            if (activeAlarmIds.isEmpty()) stopSelf()
            return START_NOT_STICKY
        }

        activeAlarmIds.add(alarmId)
        currentAlarmId = alarmId

        // Create notification channel
        createNotificationChannel()

        // Start as foreground service
        val notification = createNotification(alarmId)
        startForeground(NOTIFICATION_ID, notification)

        // Load alarm and play sound
        serviceScope.launch {
            playAlarm(alarmId)
        }

        // Launch AlarmRingingActivity
        val activityIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(activityIntent)

        return START_NOT_STICKY
    }

    private suspend fun playAlarm(alarmId: Long) = withContext(Dispatchers.IO) {
        val database = DevLifeDatabase.getDatabase(applicationContext)
        val alarm = database.alarmDao().getAlarmById(alarmId) ?: return@withContext

        // Get sound URI
        val soundUri = if (alarm.soundUri == "default") {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            alarm.soundUri.toUri()
        }

        withContext(Dispatchers.Main) {
            try {
                // Stop and release previous media player if it exists
                mediaPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
                
                // Cancel previous timeout
                timeoutJob?.cancel()

                // Initialize MediaPlayer
                val mp = MediaPlayer().apply {
                    setDataSource(applicationContext, soundUri)
                    isLooping = true
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )

                    // Start with volume 0 for fade-in
                    setVolume(0f, 0f)
                    prepare()
                    start()
                }
                mediaPlayer = mp

                // Fade in - pass the instance to avoid affecting future players
                fadeIn(mp, alarm.fadeInDuration, alarm.volume)

                // Set timeout for auto-dismiss
                timeoutJob = serviceScope.launch {
                    delay(alarm.ringDuration * 60 * 1000L)
                    handleAutoDismiss()
                }

                // Vibrate if enabled
                if (alarm.vibrate) {
                    startVibration()
                }

                // Note: Rescheduling is now handled in AlarmRingingActivity when user dismisses
                // This prevents conflicts with snooze and ensures ONCE alarms don't repeat

            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private fun fadeIn(mp: MediaPlayer, durationSeconds: Int, targetVolume: Int) {
        val steps = 50
        val stepDuration = (durationSeconds * 1000) / steps
        val volumeIncrement = (targetVolume / 100f) / steps

        serviceScope.launch {
            repeat(steps) { step ->
                try {
                    if (mediaPlayer != mp) return@launch // Stop if this is no longer the current player
                    val volume = volumeIncrement * (step + 1)
                    mp.setVolume(volume, volume)
                    delay(stepDuration.toLong())
                } catch (e: Exception) {
                    // mp might be released
                    return@launch
                }
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000) // Vibrate for 1s, pause 1s, repeat

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    fun stopAlarm() {
        timeoutJob?.cancel()
        timeoutJob = null

        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (e: Exception) {
                // Ignore if already stopped
            }
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleAutoDismiss() {
        val idToDismiss = currentAlarmId
        
        // Notify activity to finish and reschedule
        val intent = Intent(ACTION_ALARM_TIMEOUT).apply {
            putExtra(EXTRA_ALARM_ID, idToDismiss)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        
        activeAlarmIds.remove(idToDismiss)
        
        if (activeAlarmIds.isEmpty()) {
            stopAlarm()
        } else {
            // Play next alarm in stack
            val nextId = activeAlarmIds.last()
            currentAlarmId = nextId
            updateNotification(nextId)
            serviceScope.launch {
                playAlarm(nextId)
            }
        }
    }

    private fun updateNotification(alarmId: Long) {
        val notification = createNotification(alarmId)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for active alarms"
                setSound(null, null) // Sound is handled by MediaPlayer
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(alarmId: Long): Notification {
        // Intent to open AlarmRingingActivity when notification is tapped
        val activityIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to dismiss alarm from notification action button
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        val dismissPendingIntent = PendingIntent.getService(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm Ringing")
            .setContentText("Tap to open alarm")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(activityPendingIntent)
            .setFullScreenIntent(activityPendingIntent, true)
            .addAction(R.drawable.ic_alarm, "Dismiss", dismissPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val ACTION_DISMISS_ALARM = "com.devlife.devlifeos.ACTION_DISMISS_ALARM"
        const val ACTION_ALARM_TIMEOUT = "com.devlife.devlifeos.ACTION_ALARM_TIMEOUT"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "alarm_channel"
    }
}
