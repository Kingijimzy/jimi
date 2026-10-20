package com.jimi.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Core phone control service.
 * This is the foundation for:
 * - Reading the screen
 * - Tapping, swiping, typing
 * - Opening apps and controlling UI
 * - Browser automation
 * - Game Sentinel vision loops
 */
class JimiAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: JimiAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We will use this later for smart screen change detection
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // --- Basic control methods (will be expanded) ---

    fun getRoot(): AccessibilityNodeInfo? = rootInActiveWindow

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun globalAction(action: Int): Boolean {
        return performGlobalAction(action)
    }

    fun goHome() = globalAction(GLOBAL_ACTION_HOME)
    fun goBack() = globalAction(GLOBAL_ACTION_BACK)
    fun openRecents() = globalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications() = globalAction(GLOBAL_ACTION_NOTIFICATIONS)
}
