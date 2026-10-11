package com.stark.jarvis.service

import android.util.Log

/**
 * Simple wake-word detector for "Hey JARVIS" or "Jarvis" trigger.
 * This is a lightweight alternative to a full hotword engine.
 * For production, consider Porcupine, Vosk, or Google Cloud Speech API.
 */
class WakeWordDetector {
    companion object {
        private const val TAG = "WakeWordDetector"
        private val WAKE_PHRASES = listOf(
            "hey jarvis",
            "jarvis",
            "hey sir",
            "hey assistant"
        )
    }

    fun isWakeWordDetected(recognizedText: String): Boolean {
        val normalized = recognizedText.trim().lowercase()
        return WAKE_PHRASES.any { normalized.contains(it) }
    }

    fun stripWakeWord(recognizedText: String): String {
        val normalized = recognizedText.trim().lowercase()
        var result = normalized

        for (phrase in WAKE_PHRASES) {
            if (result.contains(phrase)) {
                result = result.replace(phrase, "").trim()
                break
            }
        }

        return if (result.isEmpty()) "hello" else result
    }
}
