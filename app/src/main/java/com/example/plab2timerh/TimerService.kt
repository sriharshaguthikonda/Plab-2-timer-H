package com.example.plab2timerh

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import java.util.*

class TimerService : Service() {
    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var handler: Handler? = null
    private var updateRunnable: Runnable? = null
    private var timerUpdateListener: TimerUpdateListener? = null
    private var startTime: Long = 0
    private var totalDuration: Long = 0
    private var currentPhase: Int = 0

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "TimerServiceChannel"
    }

    interface TimerUpdateListener {
        fun onTimerUpdate(timeLeft: Long, phase: Int)
        fun onPhaseComplete(phase: Int)
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart service if killed
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification("Timer ready...")
        startForeground(NOTIFICATION_ID, notification)

        // Initialize handler
        handler = Handler(Looper.getMainLooper())

        // Use PARTIAL_WAKE_LOCK to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Plab2TimerH::ServiceWakeLock"
        )
        wakeLock?.setReferenceCounted(false)
    }

    fun setTimerUpdateListener(listener: TimerUpdateListener) {
        this.timerUpdateListener = listener
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create stop action
        val stopIntent = Intent(this, TimerService::class.java)
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Plab 2 Timer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setShowWhen(true)
            .setUsesChronometer(false)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    fun startPhase(phase: Int, phaseDuration: Long) {
        // Stop any existing timer
        stopTimer()
        
        currentPhase = phase
        totalDuration = phaseDuration
        startTime = System.currentTimeMillis()
        
        // Acquire wake lock for the entire duration plus buffer
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(phaseDuration + 30000) // Extra 30 seconds buffer
        }

        // Create update runnable
        updateRunnable = object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - startTime
                val timeLeftInPhase = totalDuration - elapsedTime
                
                if (timeLeftInPhase > 0) {
                    val minutes = (timeLeftInPhase / 1000).toInt() / 60
                    val seconds = (timeLeftInPhase / 1000).toInt() % 60
                    val timeString = String.format("%02d:%02d", minutes, seconds)
                    
                    updateNotification("Phase $phase: $timeString remaining")
                    
                    // Update UI through listener
                    timerUpdateListener?.onTimerUpdate(timeLeftInPhase, phase)
                    
                    // Schedule next update
                    handler?.postDelayed(this, 1000L)
                } else {
                    // Phase completed
                    stopTimer()
                    timerUpdateListener?.onPhaseComplete(phase)
                }
            }
        }
        
        // Start the updates
        handler?.post(updateRunnable!!)
    }

    fun stopTimer() {
        updateRunnable?.let { handler?.removeCallbacks(it) }
        updateRunnable = null
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        updateNotification("Timer stopped")
    }
    
    fun resetTimer() {
        stopTimer()
        startTime = 0
        totalDuration = 0
        currentPhase = 0
        timerUpdateListener?.onTimerUpdate(0, 0)
        updateNotification("Timer reset")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for timer notifications"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                // Allow the notification to show as heads-up
                importance = NotificationManager.IMPORTANCE_HIGH
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Don't stop the service when task is removed
        // The service will continue running in the background
    }
}
