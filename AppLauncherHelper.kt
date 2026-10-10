package com.stark.jarvis.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

/**
 * Offline App Launcher for JARVIS on Samsung Galaxy A16.
 *
 * Resolves spoken app names directly to Android package intents without requiring
 * internet connection.
 */
class AppLauncherHelper(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Common package mappings for Samsung One UI & Android ecosystem
    private val appAliases = mapOf(
        "youtube" to listOf("com.google.android.youtube"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "camera" to listOf("com.sec.android.app.camera", "com.google.android.GoogleCamera"),
        "chrome" to listOf("com.android.chrome"),
        "browser" to listOf("com.sec.android.app.sbrowser", "com.android.chrome"),
        "settings" to listOf("com.android.settings"),
        "calculator" to listOf("com.sec.android.app.popupcalculator", "com.google.android.calculator"),
        "gallery" to listOf("com.sec.android.gallery3d", "com.google.android.apps.photos"),
        "photos" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d"),
        "spotify" to listOf("com.spotify.music"),
        "maps" to listOf("com.google.android.apps.maps"),
        "phone" to listOf("com.samsung.android.dialer", "com.google.android.dialer"),
        "dialer" to listOf("com.samsung.android.dialer", "com.google.android.dialer"),
        "messages" to listOf("com.samsung.android.messaging", "com.google.android.apps.messaging"),
        "clock" to listOf("com.sec.android.app.clockpackage", "com.google.android.deskclock"),
        "calendar" to listOf("com.samsung.android.calendar", "com.google.android.calendar")
    )

    /**
     * Attempts to open the requested app by name.
     * @return Pair<Boolean, String>: (success status, human-readable confirmation message)
     */
    fun openApp(targetName: String): Pair<Boolean, String> {
        val cleanName = targetName.lowercase().trim()

        // 1. Direct standard hardware actions
        if (cleanName.contains("camera")) {
            try {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return Pair(true, "Activating camera optical sensors, sir.")
            } catch (e: Exception) {
                Log.w("AppLauncher", "Direct camera intent fallback needed: ${e.message}")
            }
        }

        if (cleanName == "settings" || cleanName.contains("system settings")) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return Pair(true, "Accessing system settings panel.")
            } catch (e: Exception) {
                Log.w("AppLauncher", "Settings intent error: ${e.message}")
            }
        }

        if (cleanName == "phone" || cleanName.contains("dialer")) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return Pair(true, "Opening phone dialer interface.")
            } catch (e: Exception) {
                Log.w("AppLauncher", "Dialer intent error: ${e.message}")
            }
        }

        // 2. Lookup alias dictionary
        for ((alias, packages) in appAliases) {
            if (cleanName.contains(alias)) {
                for (pkg in packages) {
                    val intent = packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return Pair(true, "Launching ${alias.replaceFirstChar { it.uppercase() }}, sir.")
                    }
                }
            }
        }

        // 3. Fallback: Search all installed launcher activities matching the query
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = packageManager.queryIntentActivities(mainIntent, 0)
            for (info in resolvedApps) {
                val appLabel = info.loadLabel(packageManager).toString().lowercase()
                if (appLabel.contains(cleanName) || cleanName.contains(appLabel)) {
                    val packageName = info.activityInfo.packageName
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return Pair(true, "Opening ${info.loadLabel(packageManager)}, sir.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppLauncher", "Error querying installed applications", e)
        }

        return Pair(false, "Apologies, sir. I could not locate the application \"$targetName\" on your device.")
    }
}
