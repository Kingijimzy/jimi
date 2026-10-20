package com.jimi.app.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

/**
 * Low-level phone capabilities.
 * Full Accessibility control will be added in JimiAccessibilityService.
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

    // Placeholders for upcoming full control
    fun openApp(name: String): String = "Opening $name... (full control coming next)"
    fun openUrl(url: String): String = "Opening $url... (browser automation coming next)"
}
