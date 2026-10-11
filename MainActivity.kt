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
import com.stark.jarvis.util.ContactCallHelper
import com.stark.jarvis.util.DeviceControlHelper
import com.stark.jarvis.util.WhatsAppHelper
import com.stark.jarvis.voice.JarvisVoiceEngine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var voiceEngine: JarvisVoiceEngine
    private lateinit var appLauncher: AppLauncherHelper
    private lateinit var contactCallHelper: ContactCallHelper
    private lateinit var whatsappHelper: WhatsAppHelper
    private lateinit var deviceControlHelper: DeviceControlHelper
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

    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            binding.tvStatusBanner.text = "CONTACT ACCESS AUTHORIZED"
        } else {
            binding.tvStatusBanner.text = "CONTACT ACCESS DENIED"
            Toast.makeText(this, "Contact access is required to find a contact to call.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appLauncher = AppLauncherHelper(this)
        contactCallHelper = ContactCallHelper(this)
        whatsappHelper = WhatsAppHelper(this)
        deviceControlHelper = DeviceControlHelper(this)
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

        if (lower.contains("call ") || lower.contains("dial ") || lower.contains("phone ")) {
            handleContactCall(lower)
            return
        }

        if (lower.contains("whatsapp") || lower.contains("whats app")) {
            handleWhatsApp(lower)
            return
        }

        if (lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("flashlight") || lower.contains("torch") || lower.contains("volume") || lower.contains("brightness") || lower.contains("airplane mode") || lower.contains("flight mode")) {
            handleDeviceControl(lower)
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
            respond("Sir, that command is not recognized in offline mode. Try: open app, go home, go back, call, WhatsApp, Wi‑Fi, or who am I?")
        }
    }

    private fun handleContactCall(command: String) {
        val lower = command.lowercase().trim()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            respond("I need contact access to search for the person you want to call.")
            return
        }

        val targetName = when {
            lower.contains("call ") -> lower.removePrefix("call ").trim()
            lower.contains("dial ") -> lower.removePrefix("dial ").trim()
            lower.contains("phone ") -> lower.removePrefix("phone ").trim()
            else -> lower
        }

        val aliasName = contactCallHelper.resolveContactAlias(targetName)
        val matches = contactCallHelper.findContactsByName(aliasName)

        when {
            matches.isEmpty() -> {
                respond("I could not find a contact named $aliasName. Please say the full name clearly or open your contacts.")
            }
            matches.size == 1 -> {
                val contact = matches.first()
                val dialIntent = contactCallHelper.buildDialIntent(contact.phoneNumber)
                startActivity(dialIntent)
                respond("Calling ${contact.displayName}, sir. Please confirm in the dialer before placing the call.")
            }
            else -> {
                val options = matches.take(5).joinToString(", ") { it.displayName }
                respond("I found several contacts: $options. Please tell me which one you want to call.")
            }
        }
    }

    private fun handleWhatsApp(command: String) {
        val lower = command.lowercase().trim()

        if (lower.contains("open whatsapp") || lower.contains("open whats app")) {
            val opened = whatsappHelper.openWhatsApp()
            respond(if (opened) "Opening WhatsApp, sir." else "I could not open WhatsApp on this device.")
            return
        }

        if (lower.contains("send") || lower.contains("message") || lower.contains("text")) {
            val target = lower
                .replace("whatsapp", "")
                .replace("whats app", "")
                .replace("send", "")
                .replace("message", "")
                .replace("text", "")
                .trim()

            if (target.isEmpty()) {
                val opened = whatsappHelper.openWhatsApp()
                respond(if (opened) "Opening WhatsApp so you can send a message." else "I could not open WhatsApp.")
                return
            }

            val matches = contactCallHelper.findContactsByName(target)
            if (matches.isNotEmpty()) {
                val contact = matches.first()
                val result = whatsappHelper.openWhatsAppChat(contact.phoneNumber)
                respond(if (result) "Opening WhatsApp chat with ${contact.displayName}, sir." else "I could not open a WhatsApp chat for ${contact.displayName}.")
                return
            }
        }

        val opened = whatsappHelper.openWhatsApp()
        respond(if (opened) "Opening WhatsApp, sir." else "I could not open WhatsApp on this device.")
    }

    private fun handleDeviceControl(command: String) {
        val lower = command.lowercase().trim()

        when {
            lower.contains("wifi on") || lower.contains("turn on wifi") -> {
                val ok = deviceControlHelper.toggleWifi(true)
                respond(if (ok) "Turning Wi‑Fi on, sir." else "I could not toggle Wi‑Fi on this device.")
            }
            lower.contains("wifi off") || lower.contains("turn off wifi") -> {
                val ok = deviceControlHelper.toggleWifi(false)
                respond(if (ok) "Turning Wi‑Fi off, sir." else "I could not toggle Wi‑Fi off on this device.")
            }
            lower.contains("bluetooth on") || lower.contains("turn on bluetooth") -> {
                val ok = deviceControlHelper.toggleBluetooth(true)
                respond(if (ok) "Turning Bluetooth on, sir." else "I could not enable Bluetooth on this device.")
            }
            lower.contains("bluetooth off") || lower.contains("turn off bluetooth") -> {
                val ok = deviceControlHelper.toggleBluetooth(false)
                respond(if (ok) "Turning Bluetooth off, sir." else "I could not disable Bluetooth on this device.")
            }
            lower.contains("flashlight on") || lower.contains("torch on") -> {
                val ok = deviceControlHelper.toggleFlashlight(true)
                respond(if (ok) "Turning on the flashlight, sir." else "I could not enable the flashlight on this device.")
            }
            lower.contains("flashlight off") || lower.contains("torch off") -> {
                val ok = deviceControlHelper.toggleFlashlight(false)
                respond(if (ok) "Turning off the flashlight, sir." else "I could not disable the flashlight on this device.")
            }
            lower.contains("volume up") || lower.contains("increase volume") -> {
                val ok = deviceControlHelper.setVolume(5)
                respond(if (ok) "Increasing volume, sir." else "I could not change the volume.")
            }
            lower.contains("volume down") || lower.contains("decrease volume") -> {
                val ok = deviceControlHelper.setVolume(-5)
                respond(if (ok) "Lowering volume, sir." else "I could not change the volume.")
            }
            lower.contains("brightness") -> {
                val opened = deviceControlHelper.openBrightnessSettings()
                respond(if (opened) "Opening brightness settings, sir." else "I could not open brightness settings.")
            }
            lower.contains("airplane mode") || lower.contains("flight mode") -> {
                val opened = deviceControlHelper.openAirplaneModeSettings()
                respond(if (opened) "Opening airplane mode settings, sir." else "I could not open airplane mode settings.")
            }
            else -> {
                respond("I can control Wi‑Fi, Bluetooth, flashlight, volume, brightness, and airplane mode.")
            }
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
