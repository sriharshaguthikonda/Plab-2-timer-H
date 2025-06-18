package com.example.plab2timerh
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TimerService.TimerCallback {
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

    private var timerService: TimerService? = null
    private var isBound = false
    private var currentPhase = 0 // 0 = not started, 1 = Phase 1, 2 = Phase 2, 3 = Phase 3
    private var isTimerRunning = false
    private var pendingPhase: Int? = null // Store phase to start after binding

    // Default timer durations in milliseconds
    private var phase1Duration = 90000L    // 1.5 minutes
    private var phase2Duration = 360000L   // 6 minutes  
    private var phase3Duration = 120000L   // 2 minutes
    private var timeLeft = 0L              // Remaining time in milliseconds

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true
            
            // Start the timer with default durations
            timerService?.startTimer(
                phase = 1,
                phase1Duration = phase1Duration,
                phase2Duration = phase2Duration,
                phase3Duration = phase3Duration,
                callback = this@MainActivity
            )
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            isBound = false
            timerService = null
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

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
        
        // Ensure start button is enabled
        startButton.isEnabled = true
        Log.d(TAG, "Start button enabled: ${startButton.isEnabled}")

        // Setup number pickers
        setupNumberPickers(phase1Minutes, 0, 59, 1)
        setupNumberPickers(phase1Seconds, 0, 59, 30)
        setupNumberPickers(phase2Minutes, 0, 59, 6)
        setupNumberPickers(phase2Seconds, 0, 59, 0)
        setupNumberPickers(phase3Minutes, 0, 59, 2)
        setupNumberPickers(phase3Seconds, 0, 59, 0)
        
        // Apply NumberPicker divider and text color
        setNumberPickerDivider(phase1Minutes)
        setNumberPickerDivider(phase1Seconds)
        setNumberPickerDivider(phase2Minutes)
        setNumberPickerDivider(phase2Seconds)
        setNumberPickerDivider(phase3Minutes)
        setNumberPickerDivider(phase3Seconds)
        setNumberPickerTextColor(phase1Minutes)
        setNumberPickerTextColor(phase1Seconds)
        setNumberPickerTextColor(phase2Minutes)
        setNumberPickerTextColor(phase2Seconds)
        setNumberPickerTextColor(phase3Minutes)
        setNumberPickerTextColor(phase3Seconds)

        // Update durations when values change
        phase1Minutes.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase1Seconds.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase2Minutes.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase2Seconds.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase3Minutes.setOnValueChangedListener { _, _, _ -> updateDurations() }
        phase3Seconds.setOnValueChangedListener { _, _, _ -> updateDurations() }

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)
                tts.setSpeechRate(0.7f)
                tts.setPitch(1.0f)
                Log.d(TAG, "TTS initialized: ${result == TextToSpeech.LANG_AVAILABLE}")
            } else {
                Log.e(TAG, "TTS initialization failed: status $status")
            }
        }

        // Button Listeners
        startButton.setOnClickListener {
            Toast.makeText(this, "Start clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Start Sequence button clicked")
            startSequentialTimer()
        }
        stopButton.setOnClickListener {
            Log.d(TAG, "Stop button clicked")
            stopTimer()
        }
        resetButton.setOnClickListener {
            Log.d(TAG, "Reset button clicked")
            resetTimer()
        }
        
        // Phase start buttons
        phase1StartButton.setOnClickListener {
            Log.d(TAG, "Phase 1 Start button clicked")
            startPhase(1)
        }
        phase2StartButton.setOnClickListener {
            Log.d(TAG, "Phase 2 Start button clicked")
            startPhase(2)
        }
        phase3StartButton.setOnClickListener {
            Log.d(TAG, "Phase 3 Start button clicked")
            startPhase(3)
        }

        // Dark Mode Switch
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Dark mode switch: $isChecked")
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Request battery optimization exemption
        requestBatteryOptimizationExemption()

        // Initialize display
        resetTimer()
    }

    private fun setupNumberPickers(picker: NumberPicker, min: Int, max: Int, value: Int) {
        picker.minValue = min
        picker.maxValue = max
        picker.value = value
        picker.wrapSelectorWheel = true
    }

    private fun setNumberPickerDivider(numberPicker: NumberPicker) {
        try {
            val dividerField = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
            dividerField.isAccessible = true
            val dividerDrawable = ContextCompat.getDrawable(this, R.drawable.number_picker_divider)
            dividerField.set(numberPicker, dividerDrawable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set NumberPicker divider", e)
        }
    }

    private fun setNumberPickerTextColor(numberPicker: NumberPicker) {
        try {
            val textField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            textField.isAccessible = true
            val paint = textField.get(numberPicker) as android.graphics.Paint
            paint.color = ContextCompat.getColor(this, android.R.color.white)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set NumberPicker text color", e)
        }
    }
    
    private fun updateDurations() {
        phase1Duration = (phase1Minutes.value * 60L + phase1Seconds.value) * 1000
        phase2Duration = (phase2Minutes.value * 60L + phase2Seconds.value) * 1000
        phase3Duration = (phase3Minutes.value * 60L + phase3Seconds.value) * 1000
        Log.d(TAG, "Durations updated: P1=$phase1Duration, P2=$phase2Duration, P3=$phase3Duration")
    }
    
    private fun startSequentialTimer() {
        if (isTimerRunning) {
            Log.d(TAG, "Timer already running, ignoring start request")
            return
        }
        Log.d(TAG, "Starting sequential timer")
        isTimerRunning = true
        startButton.isEnabled = false
        startPhase(1)
    }
    
    private fun startPhase(phase: Int) {
        Log.d(TAG, "Starting phase: $phase")
        currentPhase = phase
        updateDurations()
        
        if (isBound) {
            // If already bound, just update the timer with new phase
            timerService?.startTimer(
                phase = phase,
                phase1Duration = phase1Duration,
                phase2Duration = phase2Duration,
                phase3Duration = phase3Duration,
                callback = this@MainActivity
            )
        } else {
            // Otherwise, bind to the service which will start the timer
            val intent = Intent(this, TimerService::class.java)
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onTick(timeLeft: Long, phase: Int) {
        runOnUiThread {
            this@MainActivity.currentPhase = phase
            this@MainActivity.timeLeft = timeLeft
            updateTimerDisplay()
        }
    }

    override fun onPhaseChange(phase: Int, status: String) {
        runOnUiThread {
            currentPhase = phase
            statusTextView.text = status
            phase1StartButton.isEnabled = phase != 1
            phase2StartButton.isEnabled = phase != 2
            phase3StartButton.isEnabled = phase != 3
            Log.d(TAG, "Phase changed to: $phase, status: $status")
        }
    }

    override fun onTimerFinish() {
        runOnUiThread {
            isTimerRunning = false
            startButton.isEnabled = true
            timerTextView.text = "00:00"
            statusTextView.text = "Finished"
            phase1StartButton.isEnabled = true
            phase2StartButton.isEnabled = true
            phase3StartButton.isEnabled = true
            unbindServiceIfBound()
            Log.d(TAG, "Timer sequence finished")
        }
    }

    private fun stopTimer() {
        Log.d(TAG, "Stopping timer")
        timerService?.stopTimer()
        isTimerRunning = false
        startButton.isEnabled = true
        phase1StartButton.isEnabled = true
        phase2StartButton.isEnabled = true
        phase3StartButton.isEnabled = true
        unbindServiceIfBound()
    }

    private fun resetTimer() {
        Log.d(TAG, "Resetting timer")
        stopTimer()
        currentPhase = 0
        
        // Reset NumberPickers to default values
        phase1Minutes.value = 1
        phase1Seconds.value = 30
        phase2Minutes.value = 6
        phase2Seconds.value = 0
        phase3Minutes.value = 2
        phase3Seconds.value = 0
        
        updateDurations()
        timeLeft = phase1Duration
        statusTextView.text = "Ready to Start"
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeft / 1000).toInt() / 60
        val seconds = (timeLeft / 1000).toInt() % 60
        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
        
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
        Log.d(TAG, "Timer display updated: $minutes:$seconds")
    }

    private fun requestBatteryOptimizationExemption() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open battery optimization settings", e)
            }
        }
    }

    private fun unbindServiceIfBound() {
        if (isBound) {
            unbindService(connection)
            isBound = false
            Log.d(TAG, "Service unbound")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "MainActivity destroyed")
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        unbindServiceIfBound()
        super.onDestroy()
    }
}