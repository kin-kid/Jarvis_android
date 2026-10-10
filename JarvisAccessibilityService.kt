package com.stark.jarvis.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * JarvisAccessibilityService provides hands-free system navigation for JARVIS.
 *
 * Supported offline actions:
 * - Go Back: performGlobalAction(GLOBAL_ACTION_BACK)
 * - Go Home: performGlobalAction(GLOBAL_ACTION_HOME)
 * - Recent Apps: performGlobalAction(GLOBAL_ACTION_RECENTS)
 * - Notifications: performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
 * - Safe Lock Screen: performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
 *
 * SECURITY NOTICE FOR SAMSUNG GALAXY A16 (Knox Security):
 * This service explicitly uses official Android framework APIs.
 * It NEVER bypasses the lock screen, Knox security, PIN, or biometric fingerprint.
 * When requested to lock, it calls GLOBAL_ACTION_LOCK_SCREEN which immediately
 * activates the secure Samsung lock screen, requiring the user's authentic PIN/fingerprint
 * to unlock.
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisAccessibility"
        
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "JARVIS Navigation Accessibility Service connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No window scraping needed; service is purely for authorized user gestures
    }

    override fun onInterrupt() {
        Log.w(TAG, "JARVIS Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "JARVIS Accessibility Service destroyed.")
    }

    /**
     * Navigates back one screen (equivalent to pressing the Back button or back gesture).
     */
    fun performGoBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Navigates directly to the Android Home screen.
     */
    fun performGoHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Opens the Recent Apps overview / app switcher.
     */
    fun performShowRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Pulls down the system notification shade.
     */
    fun performOpenNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Safely locks the screen using Android 9+ official GLOBAL_ACTION_LOCK_SCREEN.
     * Complies with Samsung Knox security.
     */
    fun performLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Log.w(TAG, "Lock screen via Accessibility requires Android 9 (API 28) or higher.")
            false
        }
    }
}
