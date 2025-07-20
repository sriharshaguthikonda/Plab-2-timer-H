package com.example.plab2timerh

import android.Manifest
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var darkModeSwitch: Switch
    private lateinit var ttsEditText: EditText
    private lateinit var tts: TextToSpeech
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeValueTextView: TextView
    private lateinit var volumeWarningTextView: TextView
    private var ttsVolume = 1.0f

    private lateinit var phase1Minutes: NumberPicker
    private lateinit var phase1Seconds: NumberPicker
    private lateinit var phase2Minutes: NumberPicker
    private lateinit var phase2Seconds: NumberPicker
    private lateinit var phase3Minutes: NumberPicker
    private lateinit var phase3Seconds: NumberPicker
    private lateinit var phase1StartButton: Button
    private lateinit var phase2StartButton: Button
    private lateinit var phase3StartButton: Button

    private var currentPhase = 0
    private var timeLeft: Long = 0
    private var isTtsInitialized = false
    private var isTimerRunning = false

    private var phase1Duration = 90000L
    private var phase2Duration = 360000L
    private var phase3Duration = 120000L

    private var wakeLock: PowerManager.WakeLock? = null
    private var timerService: TimerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            serviceBound = true
            timerService?.setTimerUpdateListener(object : TimerService.TimerUpdateListener {
                override fun onTimerUpdate(timeLeft: Long, phase: Int) {
                    runOnUiThread {
                        this@MainActivity.timeLeft = timeLeft
                        this@MainActivity.currentPhase = phase
                        updateTimerDisplay()
                    }
                }

                override fun onPhaseComplete(phase: Int) {
                    runOnUiThread {
                        // Wake up screen when phase completes
                        wakeUpScreen()
                        
                        when (phase) {
                            1 -> {
                                speak("Now please enter the room")
                                startPhase(2)
                            }
                            2 -> {
                                speak("2 minutes remaining")
                                startPhase(3)
                            }
                            3 -> {
                                speak("Now please move to the next room")
                                finishSequence()
                            }
                        }
                    }
                }
            })
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBound = false
        }
    }

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Check for battery optimization
        checkBatteryOptimization()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Plab2TimerH::WakeLock"
        )

        initViews()
        resetTimer()
        initTextToSpeech()
        
        // Start and bind to the service
        val intent = Intent(this, TimerService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission is required for timer alerts", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Handle if the intent is not available
                    e.printStackTrace()
                }
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Battery Optimization")
                    .setMessage("For the timer to work properly when the screen is off, please disable battery optimization for this app.")
                    .setPositiveButton("Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:$packageName")
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Handle if the intent is not available
                            e.printStackTrace()
                        }
                    }
                    .setNegativeButton("Later", null)
                    .show()
            }
        }
    }

    private fun initViews() {
        timerTextView = findViewById(R.id.timerTextView)
        statusTextView = findViewById(R.id.section1TextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        ttsEditText = findViewById(R.id.ttsEditText)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        volumeValueTextView = findViewById(R.id.volumeValueTextView)
        volumeWarningTextView = findViewById(R.id.volumeWarningTextView)

        volumeSeekBar.max = 200
        volumeSeekBar.progress = 100
        volumeValueTextView.text = getString(R.string.volume_default)
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeValueTextView.text = getString(R.string.volume_format, progress)
                ttsVolume = progress / 100f
                volumeWarningTextView.visibility = if (progress > 100) View.VISIBLE else View.GONE
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        phase1Minutes = findViewById(R.id.phase1Minutes)
        phase1Seconds = findViewById(R.id.phase1Seconds)
        phase2Minutes = findViewById(R.id.phase2Minutes)
        phase2Seconds = findViewById(R.id.phase2Seconds)
        phase3Minutes = findViewById(R.id.phase3Minutes)
        phase3Seconds = findViewById(R.id.phase3Seconds)
        phase1StartButton = findViewById(R.id.phase1StartButton)
        phase2StartButton = findViewById(R.id.phase2StartButton)
        phase3StartButton = findViewById(R.id.phase3StartButton)

        setupNumberPickers()

        startButton.setOnClickListener {
            wakeLock?.acquire(15 * 60 * 1000L)
            startSequentialTimer()
        }

        stopButton.setOnClickListener {
            stopTimer()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        phase1StartButton.setOnClickListener { startPhase(1) }
        phase2StartButton.setOnClickListener { startPhase(2) }
        phase3StartButton.setOnClickListener { startPhase(3) }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupNumberPickers() {
        listOf(
            Triple(phase1Minutes, 0, 59),
            Triple(phase1Seconds, 0, 59),
            Triple(phase2Minutes, 0, 59),
            Triple(phase2Seconds, 0, 59),
            Triple(phase3Minutes, 0, 59),
            Triple(phase3Seconds, 0, 59)
        ).forEach { (picker, min, max) ->
            picker.minValue = min
            picker.maxValue = max
            picker.wrapSelectorWheel = true
        }

        phase1Minutes.value = 1; phase1Seconds.value = 30
        phase2Minutes.value = 6; phase2Seconds.value = 0
        phase3Minutes.value = 2; phase3Seconds.value = 0

        listOf(phase1Minutes, phase1Seconds, phase2Minutes, phase2Seconds, phase3Minutes, phase3Seconds)
            .forEach { it.setOnValueChangedListener { _, _, _ -> updateDurations() } }
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                isTtsInitialized = tts.setLanguage(Locale.US) != TextToSpeech.LANG_MISSING_DATA
                tts.setSpeechRate(0.7f)
                tts.setPitch(1.0f)
            }
        }
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
        startPhase(1)
    }

    private fun startPhase(phase: Int) {
        if (!serviceBound) return
        
        currentPhase = phase
        updateDurations()
        timeLeft = when (phase) {
            1 -> phase1Duration
            2 -> phase2Duration
            3 -> phase3Duration
            else -> 0L
        }
        statusTextView.text = when (phase) {
            1 -> "Phase 1: Read the Question"
            2 -> "Phase 2: Enter the Room"
            3 -> "Phase 3: 2 minutes"
            else -> ""
        }

        // Use the service to handle the timer
        timerService?.startPhase(phase, timeLeft)
    }

    private fun finishSequence() {
        stopTimer()
        statusTextView.text = "All phases completed!"
        startButton.isEnabled = true
    }

    private fun stopTimer() {
        timerService?.stopTimer()
        isTimerRunning = false
        startButton.isEnabled = true
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    private fun resetTimer() {
    stopTimer()
    currentPhase = 0
    
    // Reset NumberPickers to default values
    phase1Minutes.value = 1
    phase1Seconds.value = 30
    phase2Minutes.value = 6
    phase2Seconds.value = 0
    phase3Minutes.value = 2
    phase3Seconds.value = 0
    
    // Update durations after resetting pickers
    updateDurations()
    
    // Reset to phase 1 duration for display
    timeLeft = phase1Duration
    
    // Reset UI elements
    statusTextView.text = "Ready to Start"
    updateTimerDisplay()
    
    // Ensure start button is enabled
    startButton.isEnabled = true
    isTimerRunning = false
    
    // Make sure service is also reset
    timerService?.resetTimer() 
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeft / 1000).toInt() / 60
        val seconds = (timeLeft / 1000).toInt() % 60
        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun wakeUpScreen() {
        try {
            // Modern approach to wake up screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            
            // Optionally dismiss keyguard
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, null)
            }
        } catch (e: Exception) {
            // Handle any security exceptions
            e.printStackTrace()
        }
    }

    private fun speak(text: String) {
        if (isTtsInitialized) {
            val params = Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, ttsVolume.coerceIn(0f, 2f))
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        stopTimer()
    }
}
