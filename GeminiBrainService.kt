package com.stark.jarvis.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Connects JARVIS to Google Gemini AI service for intelligent natural conversations.
 */
class GeminiBrainService(
    // Can be loaded from BuildConfig, local storage, or server proxy URL
    private val apiKey: String = "",
    private val serverProxyUrl: String? = null
) {
    companion object {
        private const val TAG = "GeminiBrainService"
        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Sends the user's voice prompt to Gemini and extracts JARVIS's spoken answer.
     */
    suspend fun queryGemini(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            // If proxy URL is provided (recommended for production without embedding keys)
            if (!serverProxyUrl.isNullOrEmpty()) {
                val jsonPayload = JsonObject().apply {
                    addProperty("message", prompt)
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(serverProxyUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val parsed = gson.fromJson(responseBody, JsonObject::class.java)
                return@withContext parsed.get("reply")?.asString
                    ?: "Neural connection operational, sir. How may I assist?"
            }

            // Direct Gemini REST API call
            if (apiKey.isBlank()) {
                return@withContext "Sir, the online neural link requires an active Gemini API key or proxy server. Operating on local offline command matrix."
            }

            val requestUrl = "$GEMINI_ENDPOINT?key=$apiKey"

            // Construct payload with JARVIS system persona
            val systemInstruction = "You are J.A.R.V.I.S., the AI assistant created for the user's Samsung Galaxy A16. Speak in 1-3 concise spoken sentences with polite British poise. Never use markdown formatting or asterisks."

            val jsonBody = """
                {
                    "system_instruction": {
                        "parts": [
                            {"text": "$systemInstruction"}
                        ]
                    },
                    "contents": [
                        {
                            "parts": [
                                {"text": ${gson.toJson(prompt)}}
                            ]
                        }
                    ]
                }
            """.trimIndent()

            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code: ${response.code}")
                return@withContext "Neural link latency detected. Please verify your connection or fallback to offline commands."
            }

            val responseString = response.body?.string() ?: ""
            val jsonRoot = gson.fromJson(responseString, JsonObject::class.java)
            val candidates = jsonRoot.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val firstCandidate = candidates[0].asJsonObject
                val content = firstCandidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    val text = parts[0].asJsonObject.get("text").asString
                    return@withContext cleanVoiceText(text)
                }
            }

            return@withContext "All systems operational, sir."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query Gemini", e)
            return@withContext "Apologies, sir. The neural link encountered an interruption: ${e.localizedMessage ?: "Connection error"}."
        }
    }

    private fun cleanVoiceText(text: String): String {
        return text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace(Regex("\\[.*?\\]"), "")
            .trim()
    }
}
