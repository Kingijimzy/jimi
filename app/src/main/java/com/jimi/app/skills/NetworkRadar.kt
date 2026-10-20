package com.jimi.app.skills

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager

/**
 * Network Radar — quick network status and diagnostics.
 * No API cost. Useful before browsing or gaming.
 */
class NetworkRadar(private val context: Context) {

    fun status(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        if (caps == null) {
            return "No active network connection."
        }

        val parts = mutableListOf<String>()

        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                parts.add("Connected via Wi‑Fi")
                parts.add(wifiDetails())
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                parts.add("Connected via mobile data")
                parts.add(cellularDetails())
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                parts.add("Connected via Ethernet")
            }
            else -> parts.add("Connected (unknown type)")
        }

        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        parts.add(if (validated) "Internet reachable." else "Connected but internet may be limited.")

        val down = caps.linkDownstreamBandwidthKbps
        val up = caps.linkUpstreamBandwidthKbps
        if (down > 0) parts.add("Est. download ~${down / 1000} Mbps")
        if (up > 0) parts.add("Est. upload ~${up / 1000} Mbps")

        return parts.joinToString(" ")
    }

    private fun wifiDetails(): String {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            val ssid = info?.ssid?.replace("\"", "")?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            val rssi = info?.rssi ?: 0
            val quality = when {
                rssi >= -50 -> "excellent"
                rssi >= -60 -> "good"
                rssi >= -70 -> "fair"
                else -> "weak"
            }
            buildString {
                if (ssid != null) append("Network: $ssid. ")
                append("Signal: $quality ($rssi dBm).")
            }
        } catch (e: Exception) {
            "Wi‑Fi details unavailable."
        }
    }

    private fun cellularDetails(): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (tm.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_NR -> "5G"
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSPA -> "3G"
                    else -> "mobile"
                }
            } else "mobile"
            "Type: $type."
        } catch (e: Exception) {
            "Mobile details unavailable."
        }
    }

    fun help(): String {
        return "Network Radar checks Wi‑Fi, mobile data, signal quality and whether the internet is reachable. Say: network status, wifi status, or network radar."
    }
}
