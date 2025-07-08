package com.example.plab2timerh

import android.content.Intent
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TimerServiceTest {

    @Test
    fun phaseCompletesAndCallsListener() {
        val controller = Robolectric.buildService(TimerService::class.java).create().get()
        val latch = CountDownLatch(1)
        var completedPhase = -1
        controller.setTimerUpdateListener(object : TimerService.TimerUpdateListener {
            override fun onTimerUpdate(timeLeft: Long, phase: Int) {}
            override fun onPhaseComplete(phase: Int) {
                completedPhase = phase
                latch.countDown()
            }
        })

        controller.startPhase(1, 100)
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()
        latch.await(200, TimeUnit.MILLISECONDS)
        assertEquals(1, completedPhase)
    }

    @Test
    fun resetStopsTimerAndClearsPhase() {
        val controller = Robolectric.buildService(TimerService::class.java).create().get()
        var lastTimeLeft = -1L
        controller.setTimerUpdateListener(object : TimerService.TimerUpdateListener {
            override fun onTimerUpdate(timeLeft: Long, phase: Int) {
                lastTimeLeft = timeLeft
            }
            override fun onPhaseComplete(phase: Int) {}
        })

        controller.startPhase(1, 100)
        controller.resetTimer()
        assertEquals(0, lastTimeLeft)
    }
}
