package com.jimi.app.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

/**
 * Phone capabilities that work without Accessibility Service.
 * Full screen control lives in JimiAccessibilityService.
 */
class PhoneTools(private val context: Context) {

    fun currentTime(): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun batteryStatus(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return "Unable to read battery status."
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (scale > 0) level * 100 / scale else level
        return "Battery is at $pct%" + if (charging) " and charging." else "."
    }

    fun deviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
    }

    fun openApp(nameOrPackage: String): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
        val match = apps.firstOrNull {
            it.loadLabel(pm).toString().equals(nameOrPackage, ignoreCase = true) ||
                    it.activityInfo.packageName.equals(nameOrPackage, ignoreCase = true)
        } ?: apps.firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(nameOrPackage.lowercase())
        }
        return if (match != null) {
            val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                "Opened ${match.loadLabel(pm)}."
            } else "Could not launch that app."
        } else {
            "I couldn't find an app named \"$nameOrPackage\"."
        }
    }

    fun openUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        return try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(u))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            "Opening $u"
        } catch (e: Exception) {
            "Could not open that link: ${e.message}"
        }
    }

    fun webSearch(query: String): String {
        return openUrl("https://www.google.com/search?q=" + Uri.encode(query))
    }

    fun openSettings(page: String = "main"): String {
        val action = when (page.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Opened $page settings."
        } catch (e: Exception) {
            "Could not open settings: ${e.message}"
        }
    }

    fun setVolume(action: String, percent: Int = 50): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        when (action.lowercase()) {
            "up" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "down" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "mute" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            "unmute" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
            "set" -> am.setStreamVolume(AudioManager.STREAM_MUSIC, (percent.coerceIn(0, 100) * max / 100), AudioManager.FLAG_SHOW_UI)
            else -> return "Use up, down, mute, unmute or set."
        }
        val now = am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
        return "Media volume is now about $now%."
    }

    fun flashlight(on: Boolean): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "This phone has no flashlight."
            cm.setTorchMode(id, on)
            if (on) "Flashlight on." else "Flashlight off."
        } catch (e: Exception) {
            "Could not control flashlight: ${e.message}"
        }
    }

    fun copyToClipboard(text: String): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Jimi", text))
        return "Copied to clipboard."
    }

    fun dial(number: String): String {
        val cleaned = number.filter { it.isDigit() || it == '+' }
        if (cleaned.length < 3) return "That doesn't look like a valid number."
        return try {
            val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            "Dialer opened with $cleaned. Press call when ready."
        } catch (e: Exception) {
            "Could not open dialer: ${e.message}"
        }
    }
}
