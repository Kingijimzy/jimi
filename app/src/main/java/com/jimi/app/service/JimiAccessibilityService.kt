package com.jimi.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.jimi.app.skills.TeachMode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Full phone control via Accessibility.
 * Powers: phone control, browser automation, Game Sentinel, macros, teach capture.
 */
class JimiAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: JimiAccessibilityService? = null
            private set

        fun isEnabled(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        TeachMode.bind(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !TeachMode.isRecording()) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED) return

        val text = sequenceOf(
            event.text?.joinToString(" ") { it.toString() },
            event.contentDescription?.toString()
        ).mapNotNull { it?.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return

        if (text.equals("Jimi", true) || text.length < 2) return
        TeachMode.recordClickText(text)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun getRoot(): AccessibilityNodeInfo? = rootInActiveWindow

    fun goHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    suspend fun tap(x: Float, y: Float, durationMs: Long = 50): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSuspend(gesture)
    }

    suspend fun swipe(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        durationMs: Long = 300
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSuspend(gesture)
    }

    private suspend fun dispatchGestureSuspend(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { cont ->
            val ok = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(false)
                }
            }, null)
            if (!ok && cont.isActive) cont.resume(false)
        }

    fun findByText(text: String, exact: Boolean = false): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null
        return findNodes(root) { node ->
            val t = node.text?.toString() ?: ""
            val d = node.contentDescription?.toString() ?: ""
            if (exact) t.equals(text, true) || d.equals(text, true)
            else t.contains(text, true) || d.contains(text, true)
        }.firstOrNull()
    }

    fun findClickableByText(text: String): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null
        return findNodes(root) { node ->
            val t = node.text?.toString() ?: ""
            val d = node.contentDescription?.toString() ?: ""
            (t.contains(text, true) || d.contains(text, true)) &&
                    (node.isClickable || node.isCheckable)
        }.firstOrNull() ?: findByText(text)?.let { climbToClickable(it) }
    }

    fun clickByText(text: String): Boolean {
        val node = findClickableByText(text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun typeText(text: String): Boolean {
        val root = getRoot() ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findNodes(root) { it.isEditable }.firstOrNull()
            ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun screenTextSummary(maxLen: Int = 800): String {
        val root = getRoot() ?: return "(no screen access)"
        val texts = mutableListOf<String>()
        collectText(root, texts)
        return texts.distinct().filter { it.isNotBlank() }.joinToString(" | ").take(maxLen)
    }

    private fun climbToClickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var n: AccessibilityNodeInfo? = node
        repeat(6) {
            if (n == null) return null
            if (n!!.isClickable || n!!.isCheckable) return n
            n = n!!.parent
        }
        return null
    }

    private fun findNodes(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            if (predicate(n)) result.add(n)
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return result
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.let { if (it.isNotBlank()) out.add(it) }
        node.contentDescription?.toString()?.let { if (it.isNotBlank()) out.add(it) }
        for (i in 0 until node.childCount) collectText(node.getChild(i), out)
    }
}
