package com.stark.jarvis.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import android.bluetooth.BluetoothAdapter
import android.hardware.camera2.CameraManager

class DeviceControlHelper(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun toggleWifi(enable: Boolean): Boolean {
        return try {
            wifiManager?.isWifiEnabled
            wifiManager?.setWifiEnabled(enable)
            true
        } catch (e: Exception) {
            Log.e("DeviceControlHelper", "WiFi toggle failed", e)
            false
        }
    }

    fun toggleBluetooth(enable: Boolean): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return if (adapter == null) {
            false
        } else {
            if (enable) adapter.enable() else adapter.disable()
            true
        }
    }

    fun toggleFlashlight(enable: Boolean): Boolean {
        val packageManager = context.packageManager
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return false
        }

        return try {
            val cameraManager = context.getSystemService(CameraManager::class.java)
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enable)
            true
        } catch (e: Exception) {
            Log.e("DeviceControlHelper", "Torch toggle failed", e)
            false
        }
    }

    fun setVolume(delta: Int): Boolean {
        if (audioManager == null) return false
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (current + delta).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return true
    }

    fun openBrightnessSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceControlHelper", "Could not open brightness settings", e)
            false
        }
    }

    fun openAirplaneModeSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceControlHelper", "Could not open airplane mode settings", e)
            false
        }
    }
}
