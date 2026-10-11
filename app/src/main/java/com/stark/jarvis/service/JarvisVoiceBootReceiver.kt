package com.stark.jarvis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class JarvisVoiceBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.i("JarvisVoiceBootReceiver", "Boot received, starting JARVIS service")
            val serviceIntent = Intent(context, JarvisVoiceService::class.java).apply {
                action = JarvisVoiceService.ACTION_START
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("JarvisVoiceBootReceiver", "Unable to start JARVIS service on boot", e)
            }
        }
    }
}
