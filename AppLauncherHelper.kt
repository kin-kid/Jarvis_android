package com.stark.jarvis.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

class AppLauncherHelper(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    private val appAliases = mapOf(
        "youtube" to listOf("com.google.android.youtube"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "camera" to listOf("com.sec.android.app.camera", "com.google.android.GoogleCamera"),
        "chrome" to listOf("com.android.chrome"),
        "settings" to listOf("com.android.settings"),
        "spotify" to listOf("com.spotify.music"),
        "maps" to listOf("com.google.android.apps.maps"),
        "phone" to listOf("com.samsung.android.dialer", "com.google.android.dialer"),
        "dialer" to listOf("com.samsung.android.dialer", "com.google.android.dialer"),
        "home" to listOf("com.android.launcher3", "com.sec.android.app.launcher"),
        "contacts" to listOf("com.android.contacts", "com.samsung.android.contacts")
    )

    fun openApp(targetName: String): Pair<Boolean, String> {
        val cleanName = targetName.lowercase().trim()

        if (cleanName.contains("camera")) {
            return tryLaunch(
                intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                successMessage = "Activating camera sensors, sir."
            )
        }

        if (cleanName.contains("settings")) {
            return tryLaunch(
                intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                successMessage = "Opening settings."
            )
        }

        if (cleanName.contains("phone") || cleanName.contains("dialer")) {
            return tryLaunch(
                intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                successMessage = "Opening phone dialer."
            )
        }

        for ((alias, packages) in appAliases) {
            if (cleanName.contains(alias)) {
                for (pkg in packages) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return Pair(true, "Launching ${alias.replaceFirstChar { it.uppercase() }}, sir.")
                    }
                }
            }
        }

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = packageManager.queryIntentActivities(mainIntent, 0)
            for (info in resolvedApps) {
                val appLabel = info.loadLabel(packageManager).toString().lowercase()
                if (appLabel.contains(cleanName) || cleanName.contains(appLabel)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return Pair(true, "Opening ${info.loadLabel(packageManager)}, sir.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppLauncherHelper", "Failed to query apps", e)
        }

        return Pair(false, "I could not locate the application \"$targetName\" on your device.")
    }

    private fun tryLaunch(intent: Intent, successMessage: String): Pair<Boolean, String> {
        return try {
            context.startActivity(intent)
            Pair(true, successMessage)
        } catch (e: Exception) {
            Pair(false, "I could not open that action on this device.")
        }
    }
}
