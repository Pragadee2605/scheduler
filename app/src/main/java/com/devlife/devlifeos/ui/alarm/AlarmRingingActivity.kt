package com.devlife.devlifeos.ui.alarm

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.devlife.devlifeos.R
import com.devlife.devlifeos.data.local.DevLifeDatabase
import com.devlife.devlifeos.data.local.entity.RepeatType
import com.devlife.devlifeos.data.repository.AlarmRepository
import com.devlife.devlifeos.domain.alarm.AlarmSchedulerImpl
import com.devlife.devlifeos.receiver.AlarmReceiver
import com.devlife.devlifeos.service.AlarmService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Premium 2026 Alarm Ringing Activity with gesture controls
 * Features:
 * - Swipe to dismiss
 * - Tap to snooze (5 minutes)
 * - Animated gradient background
 * - Glassmorphism design
 */
class AlarmRingingActivity : AppCompatActivity() {

    private var alarmId: Long = -1

    // UI Elements
    private lateinit var swipeThumb: FrameLayout
    private lateinit var swipeContainer: FrameLayout
    private lateinit var tvAlarmTime: TextView
    private lateinit var tvAmPm: TextView
    private lateinit var tvAlarmLabel: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var btnSnooze: View

    // Swipe gesture variables
    private var initialX = 0f
    private var isDragging = false
    private val dismissThreshold = 0.7f // 70% swipe to dismiss

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_ringing)

        // Show on lock screen and turn screen on
        setupWindowFlags()

        // Prevent back button from dismissing alarm
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - user must swipe to dismiss
            }
        })

        alarmId = intent.getLongExtra(AlarmService.EXTRA_ALARM_ID, -1)

        initViews()
        setupUI()
        setupGestures()
        startPulseAnimation()
    }

    private fun setupWindowFlags() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmId = intent.getLongExtra(AlarmService.EXTRA_ALARM_ID, -1)
        setupUI()
    }

    private fun initViews() {
        swipeThumb = findViewById(R.id.swipeThumb)
        swipeContainer = findViewById(R.id.swipeContainer)
        tvAlarmTime = findViewById(R.id.tvAlarmTime)
        tvAmPm = findViewById(R.id.tvAmPm)
        tvAlarmLabel = findViewById(R.id.tvAlarmLabel)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        btnSnooze = findViewById(R.id.btnSnooze)
    }

    private fun setupUI() {
        // Display current time in 12-hour format
        val calendar = Calendar.getInstance()
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Convert to 12-hour format
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val amPm = if (hour24 < 12) "AM" else "PM"

        tvAlarmTime.text = String.format(Locale.getDefault(), "%02d:%02d", hour12, minute)
        tvAmPm.text = amPm

        // Display current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(Date())

        // Set alarm label
        tvAlarmLabel.text = getString(R.string.alarm)
    }

    private fun setupGestures() {
        // Swipe to dismiss gesture
        swipeThumb.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    isDragging = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val deltaX = event.rawX - initialX
                        if (deltaX > 0) { // Only allow right swipe
                            val maxSwipe = swipeContainer.width - swipeThumb.width - 8f
                            val newX = deltaX.coerceAtMost(maxSwipe)
                            swipeThumb.translationX = newX

                            // Update alpha based on progress
                            val progress = newX / maxSwipe
                            swipeThumb.alpha = 1f - (progress * 0.3f)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val deltaX = event.rawX - initialX
                        val maxSwipe = swipeContainer.width - swipeThumb.width - 8f
                        val progress = deltaX / maxSwipe

                        if (progress >= dismissThreshold) {
                            // Dismissed!
                            animateDismiss()
                        } else {
                            // Snap back
                            animateSnapBack()
                        }
                        isDragging = false
                    }
                    // Call performClick for accessibility
                    swipeThumb.performClick()
                    true
                }
                else -> false
            }
        }

        // Override performClick for accessibility
        swipeThumb.setOnClickListener {
            // This allows accessibility services to trigger the view
            // The actual dismiss logic is handled in the touch listener
        }

        // Tap to snooze
        btnSnooze.setOnClickListener {
            snoozeAlarm()
        }
    }

    private fun animateDismiss() {
        val maxSwipe = swipeContainer.width.toFloat()
        ObjectAnimator.ofFloat(swipeThumb, "translationX", swipeThumb.translationX, maxSwipe).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Delay slightly then dismiss
        swipeThumb.postDelayed({
            dismissAlarm()
        }, 250)
    }

    private fun animateSnapBack() {
        ObjectAnimator.ofFloat(swipeThumb, "translationX", swipeThumb.translationX, 0f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(swipeThumb, "alpha", swipeThumb.alpha, 1f).apply {
            duration = 200
            start()
        }
    }

    private fun startPulseAnimation() {
        // Subtle pulse animation on the time circle
        val cardTimeContainer = findViewById<View>(R.id.cardTimeContainer)

        val scaleUpX = ObjectAnimator.ofFloat(cardTimeContainer, "scaleX", 1f, 1.03f)
        val scaleUpY = ObjectAnimator.ofFloat(cardTimeContainer, "scaleY", 1f, 1.03f)
        val scaleDownX = ObjectAnimator.ofFloat(cardTimeContainer, "scaleX", 1.03f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(cardTimeContainer, "scaleY", 1.03f, 1f)

        scaleUpX.duration = 1000
        scaleUpY.duration = 1000
        scaleDownX.duration = 1000
        scaleDownY.duration = 1000

        scaleUpX.repeatCount = ValueAnimator.INFINITE
        scaleUpY.repeatCount = ValueAnimator.INFINITE
        scaleUpX.repeatMode = ValueAnimator.REVERSE
        scaleUpY.repeatMode = ValueAnimator.REVERSE

        scaleUpX.start()
        scaleUpY.start()
    }

    private fun snoozeAlarm() {
        // Animate feedback
        btnSnooze.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                btnSnooze.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        // Schedule alarm to ring again in 5 minutes
        scheduleSnooze()

        // Show toast feedback
        Toast.makeText(this, "Alarm snoozed for 5 minutes", Toast.LENGTH_SHORT).show()

        btnSnooze.postDelayed({
            // Stop current alarm
            dismissAlarm()
        }, 200)
    }

    private fun scheduleSnooze() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + 10000, // Different request code to avoid conflicts
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule alarm for 5 minutes from now
        val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes

        // Use setAlarmClock for reliable delivery
        val alarmClockInfo = AlarmManager.AlarmClockInfo(snoozeTime, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    private fun dismissAlarm() {
        // Reschedule or disable the alarm based on repeat type
        if (alarmId != -1L) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val database = DevLifeDatabase.getDatabase(applicationContext)
                    val alarmRepository = AlarmRepository(database.alarmDao())
                    val alarm = alarmRepository.getAlarmById(alarmId)

                    if (alarm != null) {
                        when (alarm.repeatType) {
                            RepeatType.ONCE -> {
                                // ONCE alarms should be disabled after ringing
                                alarmRepository.setAlarmEnabled(alarmId, false)
                            }
                            RepeatType.DAILY,
                            RepeatType.WEEKDAYS -> {
                                // Reschedule repeating alarms for next occurrence
                                val alarmScheduler = AlarmSchedulerImpl(
                                    applicationContext,
                                    alarmRepository
                                )
                                alarmScheduler.scheduleAlarm(alarm)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Stop the alarm service
        val serviceIntent = Intent(this, AlarmService::class.java)
        stopService(serviceIntent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle power button press to snooze
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            snoozeAlarm()
            return true
        }
        // Handle volume buttons to snooze
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            snoozeAlarm()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}


