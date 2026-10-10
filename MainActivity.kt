package com.stark.jarvis

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.stark.jarvis.ai.GeminiBrainService
import com.stark.jarvis.databinding.ActivityMainBinding
import com.stark.jarvis.service.JarvisAccessibilityService
import com.stark.jarvis.util.AppLauncherHelper
import com.stark.jarvis.voice.JarvisVoiceEngine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var voiceEngine: JarvisVoiceEngine
    private lateinit var appLauncher: AppLauncherHelper
    private lateinit var geminiService: GeminiBrainService

    private var pulseAnimator: ObjectAnimator? = null
    private var isOnlineMode = true

    // Permission launcher for RECORD_AUDIO
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceEngine.speak("Acoustic sensors calibrated, sir. Microphone authorized.")
            binding.tvStatusBanner.text = "ACOUSTIC SENSORS AUTHORIZED // STANDBY"
        } else {
            binding.tvStatusBanner.text = "MICROPHONE PERMISSION DENIED"
            Toast.makeText(this, "Microphone access is required for voice commands.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize helper components
        appLauncher = AppLauncherHelper(this)
        geminiService = GeminiBrainService()

        setupVoiceEngine()
        setupArcReactorAnimation()
        setupUIListeners()
        checkPermissionsAndServices()

        // Welcome greeting on startup
        binding.root.postDelayed({
            voiceEngine.speak(getString(R.string.jarvis_initial_greeting))
        }, 800)
    }

    private fun setupVoiceEngine() {
        voiceEngine = JarvisVoiceEngine(
            context = this,
            onSpeechResult = { recognizedText ->
                handleIncomingCommand(recognizedText)
            },
            onListeningStateChanged = { isListening ->
                runOnUiThread {
                    if (isListening) {
                        binding.tvStatusBanner.text = getString(R.string.jarvis_status_listening)
                        binding.tvMicHint.text = "ACOUSTIC SENSOR ENGAGED..."
                        startIntensePulsing()
                    } else {
                        binding.tvMicHint.text = "TAP REACTOR TO COMMAND"
                        resetPulsing()
                    }
                }
            },
            onSoundLevelChanged = { rmsdB ->
                // Adjust glowing ring scale based on user voice volume
                val scale = 1.0f + (rmsdB.coerceIn(0f, 10f) / 40f)
                binding.fabMicrophone.scaleX = scale
                binding.fabMicrophone.scaleY = scale
            },
            onErrorOccurred = { errorMessage ->
                runOnUiThread {
                    binding.tvStatusBanner.text = "NOTICE: $errorMessage"
                    resetPulsing()
                }
            }
        )
    }

    private fun setupArcReactorAnimation() {
        // Continuous subtle futuristic breathing pulse
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.viewArcGlow,
            PropertyValuesHolder.ofFloat("scaleX", 0.95f, 1.15f),
            PropertyValuesHolder.ofFloat("scaleY", 0.95f, 1.15f),
            PropertyValuesHolder.ofFloat("alpha", 0.4f, 0.9f)
        ).apply {
            duration = 1800
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startIntensePulsing() {
        pulseAnimator?.duration = 600
    }

    private fun resetPulsing() {
        pulseAnimator?.duration = 1800
        binding.fabMicrophone.scaleX = 1.0f
        binding.fabMicrophone.scaleY = 1.0f
    }

    private fun setupUIListeners() {
        // Microphone FAB click
        binding.fabMicrophone.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                voiceEngine.startListening()
            } else {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        // Online / Offline Switch
        binding.switchOnlineMode.setOnCheckedChangeListener { _, isChecked ->
            isOnlineMode = isChecked
            val modeLabel = if (isChecked) "GEMINI AI LINK ACTIVE" else "OFFLINE LOCAL MATRIX ONLY"
            binding.tvStatusBanner.text = modeLabel
            voiceEngine.speak(if (isChecked) "Online neural link engaged." else "Operating strictly on local hardware.")
        }

        // Quick Command Buttons for manual offline testing
        binding.btnQuickBack.setOnClickListener { executeGoBack() }
        binding.btnQuickHome.setOnClickListener { executeGoHome() }
        binding.btnQuickCamera.setOnClickListener { executeOpenApp("camera") }
        binding.btnQuickYouTube.setOnClickListener { executeOpenApp("youtube") }
        binding.btnQuickLock.setOnClickListener { executeLockScreen() }
    }

    /**
     * Dispatcher: Checks offline hardware commands first; if none match, delegates to Gemini AI.
     */
    private fun handleIncomingCommand(command: String) {
        val lower = command.lowercase().trim()
        binding.tvUserQuery.text = "User: \"$command\""
        binding.tvStatusBanner.text = getString(R.string.jarvis_status_processing)

        // 1. Navigation offline commands
        if (lower.contains("go back") || lower == "back" || lower.contains("previous screen")) {
            executeGoBack()
            return
        }

        if (lower.contains("go home") || lower == "home screen" || lower == "take me home") {
            executeGoHome()
            return
        }

        if (lower.contains("recent apps") || lower.contains("overview") || lower.contains("app switcher")) {
            executeShowRecents()
            return
        }

        if (lower.contains("notifications") || lower.contains("show notifications")) {
            executeOpenNotifications()
            return
        }

        // 2. Lock screen offline command (Samsung Knox Secure Lock)
        if (lower.contains("lock screen") || lower.contains("lock phone") || lower == "lock the device" || lower == "lock") {
            executeLockScreen()
            return
        }

        // 3. Open apps offline command ("open youtube", "launch whatsapp", "start camera")
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            val appTarget = lower
                .removePrefix("open ")
                .removePrefix("launch ")
                .removePrefix("start ")
                .trim()
            executeOpenApp(appTarget)
            return
        }

        // 4. Online AI Intelligent Conversation (Gemini)
        if (isOnlineMode) {
            queryOnlineGemini(command)
        } else {
            val offlineReply = "Sir, that command is not recognized in offline mode. Say 'Open [app]', 'Go back', 'Go home', or 'Lock screen'."
            respond(offlineReply)
        }
    }

    private fun executeGoBack() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            val ok = service.performGoBack()
            respond(if (ok) "Navigating back, sir." else "Back navigation attempted.")
        } else {
            promptEnableAccessibility("Back navigation")
        }
    }

    private fun executeGoHome() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.performGoHome()
            respond("Returning to home screen, sir.")
        } else {
            promptEnableAccessibility("Home navigation")
        }
    }

    private fun executeShowRecents() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.performShowRecents()
            respond("Displaying active tasks, sir.")
        } else {
            promptEnableAccessibility("Recent apps overview")
        }
    }

    private fun executeOpenNotifications() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.performOpenNotifications()
            respond("Accessing notification telemetry.")
        } else {
            promptEnableAccessibility("Notifications")
        }
    }

    private fun executeLockScreen() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            respond("Locking terminal. Samsung Knox protection active, sir.") {
                service.performLockScreen()
            }
        } else {
            promptEnableAccessibility("Device lock")
        }
    }

    private fun executeOpenApp(appName: String) {
        val (success, reply) = appLauncher.openApp(appName)
        respond(reply)
    }

    private fun queryOnlineGemini(prompt: String) {
        binding.tvStatusBanner.text = "QUERYING GEMINI NEURAL NET..."
        lifecycleScope.launch {
            val answer = geminiService.queryGemini(prompt)
            respond(answer)
        }
    }

    private fun respond(text: String, onSpeechComplete: (() -> Unit)? = null) {
        runOnUiThread {
            binding.tvJarvisResponse.text = text
            binding.tvStatusBanner.text = getString(R.string.jarvis_status_speaking)
            voiceEngine.speak(text) {
                runOnUiThread {
                    binding.tvStatusBanner.text = getString(R.string.jarvis_status_standby)
                    onSpeechComplete?.invoke()
                }
            }
        }
    }

    private fun promptEnableAccessibility(feature: String) {
        val msg = "$feature requires JARVIS Accessibility Service in Samsung Settings."
        respond(msg)
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_INDEFINITE)
            .setAction("OPEN SETTINGS") {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            .show()
    }

    private fun checkPermissionsAndServices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimator?.cancel()
        voiceEngine.destroy()
    }
}
