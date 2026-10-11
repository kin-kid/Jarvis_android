package com.stark.jarvis.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

class WhatsAppHelper(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun openWhatsApp(): Boolean {
        return openPackage("com.whatsapp") || openPackage("com.whatsapp.w4b")
    }

    fun openWhatsAppChat(phoneNumber: String): Boolean {
        val normalized = phoneNumber.filter { it.isDigit() || it == '+' }
        if (normalized.isEmpty()) return false

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=${Uri.encode(normalized)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryLaunch(intent)
    }

    fun shareText(message: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryLaunch(intent)
    }

    private fun openPackage(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else false
        } catch (e: Exception) {
            Log.e("WhatsAppHelper", "Failed to open package: $packageName", e)
            false
        }
    }

    private fun tryLaunch(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("WhatsAppHelper", "Failed to launch WhatsApp intent", e)
            false
        }
    }
}
