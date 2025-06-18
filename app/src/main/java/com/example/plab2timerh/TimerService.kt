package com.example.plab2timerh

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log

class TimerService : Service() {

    interface TimerCallback {
        fun onTick(timeLeft: Long, phase: Int)
        fun onPhaseChange(phase: Int, status: String)
        fun onTimerFinish()
    }

    private val binder = TimerBinder()
    private var countDownTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    fun startTimer(
        phase: Int,
        phase1Duration: Long,
        phase2Duration: Long,
        phase3Duration: Long,
        callback: TimerCallback
    ) {
        val duration = when (phase) {
            1 -> phase1Duration
            2 -> phase2Duration
            3 -> phase3Duration
            else -> 0L
        }

        Log.d(TAG, "Starting CountDownTimer for phase $phase, duration: $duration ms")

        callback.onPhaseChange(phase, "Phase $phase Started")

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "Tick: $millisUntilFinished ms left in phase $phase")
                callback.onTick(millisUntilFinished, phase)
            }

            override fun onFinish() {
                Log.d(TAG, "Timer finished for phase $phase")
                callback.onTimerFinish()
            }
        }.start()
    }

    fun stopTimer() {
        Log.d(TAG, "Stopping timer")
        countDownTimer?.cancel()
        countDownTimer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        Log.d(TAG, "Service destroyed")
    }

    companion object {
        private const val TAG = "TimerService"
    }
}