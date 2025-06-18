package com.example.plab2timerh

import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var darkModeSwitch: Switch
    private lateinit var ttsEditText: EditText
    private lateinit var tts: TextToSpeech
    
    // Phase controls
    private lateinit var phase1Minutes: NumberPicker
    private lateinit var phase1Seconds: NumberPicker
    private lateinit var phase2Minutes: NumberPicker
    private lateinit var phase2Seconds: NumberPicker
    private lateinit var phase3Minutes: NumberPicker
    private lateinit var phase3Seconds: NumberPicker
    private lateinit var phase1StartButton: Button
    private lateinit var phase2StartButton: Button
    private lateinit var phase3StartButton: Button

    private var currentTimer: CountDownTimer? = null
    private var currentPhase = 0 // 0 = not started, 1 = Phase 1, 2 = Phase 2, 3 = Phase 3
    private var timeLeft: Long = 0
    private var isTtsInitialized = false
    private var isTimerRunning = false

    // Default timer durations in milliseconds
    private var phase1Duration = 90000L    // 1.5 minutes
    private var phase2Duration = 360000L   // 6 minutes  
    private var phase3Duration = 120000L   // 2 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        timerTextView = findViewById(R.id.timerTextView)
        statusTextView = findViewById(R.id.section1TextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        ttsEditText = findViewById(R.id.ttsEditText)
        
        // Initialize phase time pickers
        phase1Minutes = findViewById(R.id.phase1Minutes)
        phase1Seconds = findViewById(R.id.phase1Seconds)
        phase2Minutes = findViewById(R.id.phase2Minutes)
        phase2Seconds = findViewById(R.id.phase2Seconds)
        phase3Minutes = findViewById(R.id.phase3Minutes)
        phase3Seconds = findViewById(R.id.phase3Seconds)
        phase1StartButton = findViewById(R.id.phase1StartButton)
        phase2StartButton = findViewById(R.id.phase2StartButton)
        phase3StartButton = findViewById(R.id.phase3StartButton)
        
        // Setup number pickers
        setupNumberPickers(phase1Minutes, 0, 59, 1)   // 1 minute
        setupNumberPickers(phase1Seconds, 0, 59, 30)  // 30 seconds
        setupNumberPickers(phase2Minutes, 0, 59, 6)   // 6 minutes
        setupNumberPickers(phase2Seconds, 0, 59, 0)   // 0 seconds
        setupNumberPickers(phase3Minutes, 0, 59, 2)   // 2 minutes
        setupNumberPickers(phase3Seconds, 0, 59, 0)   // 0 seconds
        
        // Update durations when values change
        phase1Minutes.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase1Seconds.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase2Minutes.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase2Seconds.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase3Minutes.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase3Seconds.setOnValueChangedListener { _, _, _ -> updateDurations() }

        // Initialize TTS with maximum volume, slow speed, and clear pitch
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)
                isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED

                // Set TTS properties
                tts.setSpeechRate(0.7f) // Slower speech rate (0.5 = half speed)
                tts.setPitch(1.0f)      // Normal pitch for clarity
                val params = Bundle()
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // Maximum volume
                tts.speak("", TextToSpeech.QUEUE_FLUSH, params, null)
            }
        }

        // Button Listeners
        startButton.setOnClickListener { startSequentialTimer() }
        stopButton.setOnClickListener { stopTimer() }
        resetButton.setOnClickListener { resetTimer() }
        
        // Phase start buttons
        phase1StartButton.setOnClickListener { startPhase(1) }
        phase2StartButton.setOnClickListener { startPhase(2) }
        phase3StartButton.setOnClickListener { startPhase(3) }

        // Dark Mode Switch
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Initialize display
        resetTimer()
    }

    private fun setupNumberPickers(picker: NumberPicker, min: Int, max: Int, value: Int) {
        picker.minValue = min
        picker.maxValue = max
        picker.value = value
        picker.wrapSelectorWheel = true
    }
    
    private fun updateDurations() {
        phase1Duration = (phase1Minutes.value * 60L + phase1Seconds.value) * 1000
        phase2Duration = (phase2Minutes.value * 60L + phase2Seconds.value) * 1000
        phase3Duration = (phase3Minutes.value * 60L + phase3Seconds.value) * 1000
    }
    
    private fun startSequentialTimer() {
        if (isTimerRunning) return
        
        isTimerRunning = true
        startButton.isEnabled = false
        currentPhase = 1
        startPhase(currentPhase)
    }

    private fun startPhase(phase: Int) {
        // Stop any running timer
        currentTimer?.cancel()
        
        currentPhase = phase
        isTimerRunning = true
        updateDurations()
        
        when (phase) {
            1 -> {
                timeLeft = phase1Duration
                statusTextView.text = "Phase 1: Read the Question"
                phase1StartButton.isEnabled = false
                phase2StartButton.isEnabled = true
                phase3StartButton.isEnabled = true
            }
            2 -> {
                timeLeft = phase2Duration
                statusTextView.text = "Phase 2: Enter the Room"
                phase1StartButton.isEnabled = true
                phase2StartButton.isEnabled = false
                phase3StartButton.isEnabled = true
            }
            3 -> {
                timeLeft = phase3Duration
                statusTextView.text = "Phase 3: 2 minutes"
                phase1StartButton.isEnabled = true
                phase2StartButton.isEnabled = true
                phase3StartButton.isEnabled = false
            }
        }
        
        currentTimer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                when (currentPhase) {
                    1 -> {
                        speakMessage("Now please enter the room")
                        if (phase2Duration > 0) startPhase(2)
                        else finishSequence()
                    }
                    2 -> {
                        speakMessage("2 minutes remaining")
                        if (phase3Duration > 0) startPhase(3)
                        else finishSequence()
                    }
                    3 -> {
                        speakMessage("Now please move to the next room")
                        finishSequence()
                    }
                }
            }
        }.start()
    }

    private fun finishSequence() {
        timerTextView.text = "00:00"
        statusTextView.text = "Sequence Complete"
        isTimerRunning = false
        startButton.isEnabled = true
    }

    private fun stopTimer() {
        currentTimer?.cancel()
        isTimerRunning = false
        // Re-enable all phase buttons when stopping
        phase1StartButton.isEnabled = true
        phase2StartButton.isEnabled = true
        phase3StartButton.isEnabled = true
        startButton.isEnabled = true
    }

    private fun resetTimer() {
        stopTimer()
        currentPhase = 0
        
        // Reset NumberPickers to default values
        phase1Minutes.value = 1    // 1 minute
        phase1Seconds.value = 30   // 30 seconds
        phase2Minutes.value = 6    // 6 minutes
        phase2Seconds.value = 0    // 0 seconds
        phase3Minutes.value = 2    // 2 minutes
        phase3Seconds.value = 0    // 0 seconds
        
        // Update durations based on the reset values
        updateDurations()
        
        // Reset timer display
        timeLeft = phase1Duration
        statusTextView.text = "Ready to Start"
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeft / 1000).toInt() / 60
        val seconds = (timeLeft / 1000).toInt() % 60
        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
        
        // Update phase indicators
        when (currentPhase) {
            1 -> {
                phase1Minutes.value = minutes
                phase1Seconds.value = seconds
            }
            2 -> {
                phase2Minutes.value = minutes
                phase2Seconds.value = seconds
            }
            3 -> {
                phase3Minutes.value = minutes
                phase3Seconds.value = seconds
            }
        }
    }

    private fun speakMessage(message: String) {
        if (isTtsInitialized) {
            val params = Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // Maximum volume
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, params, null)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}