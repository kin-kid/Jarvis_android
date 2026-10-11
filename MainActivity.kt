package com.stark.jarvis

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.content.pm.PackageManager
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
    private var isOnlineMode = false

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            binding.tvStatusBanner.text = "ACOUSTIC SENSORS AUTHORIZED // STANDBY"
            voiceEngine.speak("Microphone authorized, sir.")
        } else {
            binding.tvStatusBanner.text = "MICROPHONE PERMISSION DENIED"
            Toast.makeText(this, "Microphone permission is required for voice commands.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appLauncher = AppLauncherHelper(this)
        geminiService = GeminiBrainService()

        setupVoiceEngine()
        setupUI()
        setupArcReactorAnimation()
        checkPermissionsAndServices()

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
            onListeningStateChanged = { listening ->
                runOnUiThread {
                    if (listening) {
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
                val scale = 1.0f + (rmsdB.coerceIn(0f, 10f) / 40f)
                binding.fabMicrophone.scaleX = scale
                binding.fabMicrophone.scaleY = scale
            },
            onErrorOccurred = { error ->
                runOnUiThread {
                    binding.tvStatusBanner.text = "NOTICE: $error"
                    resetPulsing()
                }
            }
        )
    }

    private fun setupUI() {
        binding.fabMicrophone.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                voiceEngine.startListening()
            } else {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        binding.switchOnlineMode.setOnCheckedChangeListener { _, checked ->
            isOnlineMode = checked
            val modeText = if (checked) "GEMINI AI LINK ACTIVE" else "OFFLINE LOCAL MATRIX ONLY"
            binding.tvStatusBanner.text = modeText
            voiceEngine.speak(if (checked) "Online neural link engaged." else "Operating in offline mode.")
        }

        binding.btnQuickBack.setOnClickListener { executeGoBack() }
        binding.btnQuickHome.setOnClickListener { executeGoHome() }
        binding.btnQuickCamera.setOnClickListener { executeOpenApp("camera") }
        binding.btnQuickYouTube.setOnClickListener { executeOpenApp("youtube") }
        binding.btnQuickLock.setOnClickListener { executeLockScreen() }
    }

    private fun setupArcReactorAnimation() {
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.viewArcGlow,
            PropertyValuesHolder.ofFloat("scaleX", 0.95f, 1.15f),
            PropertyValuesHolder.ofFloat("scaleY", 0.95f, 1.15f),
            PropertyValuesHolder.ofFloat("alpha", 0.4f, 0.9f)
        ).apply {
            duration = 1800
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
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

    private fun handleIncomingCommand(command: String) {
        val raw = command.trim()
        val lower = raw.lowercase()

        if (lower.contains("hey jarvis") || lower.contains("jarvis")) {
            val stripped = lower.replace("hey jarvis", "").replace("jarvis", "").trim()
            val filtered = if (stripped.isEmpty()) "hello" else stripped
            handleCommand(filtered)
            return
        }

        handleCommand(lower)
    }

    private fun handleCommand(command: String) {
        val lower = command.trim().lowercase()

        if (lower.contains("who am i") || lower.contains("what is my name") || lower.contains("what's my name")) {
            respond("You are David, sir. I am JARVIS, your personal assistant.")
            return
        }

        if (lower.contains("help") || lower.contains("what can you do")) {
            respond("I can open apps, navigate home, go back, and answer questions online when the neural link is active.")
            return
        }

        binding.tvUserQuery.text = "User: \"$command\""
        binding.tvStatusBanner.text = getString(R.string.jarvis_status_processing)

        if (lower.contains("go home") || lower == "home" || lower.contains("take me home")) {
            executeGoHome()
            return
        }

        if (lower.contains("go back") || lower == "back" || lower.contains("previous screen")) {
            executeGoBack()
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

        if (lower.contains("lock screen") || lower.contains("lock phone") || lower.contains("lock the device")) {
            executeLockScreen()
            return
        }

        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            val target = lower.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()
            executeOpenApp(target)
            return
        }

        if (isOnlineMode) {
            queryOnlineGemini(command)
        } else {
            respond("Sir, that command is not recognized in offline mode. Try: open app, go home, go back, or who am I?")
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
            respond("Opening notifications, sir.")
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimator?.cancel()
        voiceEngine.destroy()
    }
}
