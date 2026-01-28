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
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.domain.alarm.AlarmSchedulerImpl
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle dismiss action from notification
        if (intent?.action == ACTION_DISMISS_ALARM) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1) ?: -1
        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
                // Initialize MediaPlayer
                mediaPlayer = MediaPlayer().apply {
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

                    // Fade in
                    fadeIn(alarm.fadeInDuration, alarm.volume)
                }

                // Vibrate if enabled
                if (alarm.vibrate) {
                    startVibration()
                }

                // Reschedule alarm for next occurrence
                val alarmRepository = AlarmRepository(database.alarmDao())
                val alarmScheduler = AlarmSchedulerImpl(applicationContext, alarmRepository)
                alarmScheduler.scheduleAlarm(alarm)

            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private fun fadeIn(durationSeconds: Int, targetVolume: Int) {
        val steps = 50
        val stepDuration = (durationSeconds * 1000) / steps
        val volumeIncrement = (targetVolume / 100f) / steps

        serviceScope.launch {
            repeat(steps) { step ->
                val volume = volumeIncrement * (step + 1)
                mediaPlayer?.setVolume(volume, volume)
                delay(stepDuration.toLong())
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
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "alarm_channel"
    }
}
