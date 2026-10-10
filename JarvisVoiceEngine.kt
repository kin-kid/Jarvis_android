package com.stark.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Handles real-time Voice Recognition and Text-to-Speech synthesis for JARVIS.
 */
class JarvisVoiceEngine(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSoundLevelChanged: (Float) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "JarvisVoiceEngine"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isCurrentlyListening = false

    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // JARVIS Persona Voice Tuning: English UK or default with slightly deep pitch
            val result = textToSpeech?.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
            
            // Tony Stark's JARVIS signature vocal cadence:
            // Pitch: 0.92f (distinguished British baritone)
            // SpeechRate: 1.02f (precise and efficient)
            textToSpeech?.setPitch(0.92f)
            textToSpeech?.setSpeechRate(1.02f)
            isTtsInitialized = true
            Log.i(TAG, "JARVIS Text-To-Speech engine initialized successfully.")
        } else {
            Log.e(TAG, "Failed to initialize Android TextToSpeech engine.")
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isCurrentlyListening = true
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        onSoundLevelChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isCurrentlyListening = false
                        onListeningStateChanged(false)
                    }

                    override fun onError(error: Int) {
                        isCurrentlyListening = false
                        onListeningStateChanged(false)
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No vocal input recognized."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Vocal input timed out."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording buffer issue."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Acoustic sensor error ($error)"
                        }
                        onErrorOccurred(message)
                    }

                    override fun onResults(results: Bundle?) {
                        isCurrentlyListening = false
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedCommand = matches[0]
                            onSpeechResult(recognizedCommand)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            onErrorOccurred("Native Android SpeechRecognizer is unavailable on this device.")
        }
    }

    /**
     * Triggers the microphone to begin listening for a voice command.
     */
    fun startListening() {
        // Stop any active speech before listening
        stopSpeaking()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
            onErrorOccurred("Could not activate microphone.")
        }
    }

    /**
     * Stops the active listening session.
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isCurrentlyListening = false
            onListeningStateChanged(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recognition", e)
        }
    }

    /**
     * Speaks the given phrase aloud using JARVIS vocal synthesis.
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsInitialized) {
            Log.w(TAG, "TTS not yet initialized, queuing fallback.")
            return
        }

        val utteranceId = "JARVIS_REPLY_${System.currentTimeMillis()}"

        if (onComplete != null) {
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) onComplete()
                }
                override fun onError(utteranceId: String?) {}
            })
        }

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
