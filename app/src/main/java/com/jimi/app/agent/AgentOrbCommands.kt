package com.jimi.app.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.jimi.app.service.FloatingOrbService

/**
 * Local commands for the always-on floating orb.
 */
object AgentOrbCommands {

    fun handle(context: Context, lower: String): String? {
        when {
            lower in listOf("show orb", "enable orb", "start orb", "floating orb on", "show floating orb") -> {
                if (!FloatingOrbService.canDrawOverlays(context)) {
                    try {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                    }
                    return "Allow Display over other apps for Jimi, then say: show orb"
                }
                FloatingOrbService.start(context)
                return "Floating orb is on. Tap it anytime to open Jimi. Drag to move."
            }
            lower in listOf("hide orb", "disable orb", "stop orb", "floating orb off", "hide floating orb") -> {
                FloatingOrbService.stop(context)
                return "Floating orb is off."
            }
            lower in listOf("orb status", "floating orb") -> {
                val overlay = FloatingOrbService.canDrawOverlays(context)
                val running = FloatingOrbService.isRunning
                return "Overlay permission: ${if (overlay) "yes" else "no"}. Orb running: ${if (running) "yes" else "no"}."
            }
            else -> return null
        }
    }
}
