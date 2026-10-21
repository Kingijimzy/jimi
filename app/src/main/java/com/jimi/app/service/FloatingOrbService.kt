package com.jimi.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.jimi.app.MainActivity
import com.jimi.app.R

/**
 * Always-on floating orb (Jarvis style).
 * Tap opens Jimi. Drag to move. Requires overlay permission.
 */
class FloatingOrbService : Service() {

    companion object {
        private const val CHANNEL_ID = "jimi_orb"
        private const val NOTIF_ID = 42

        @Volatile
        var isRunning: Boolean = false
            private set

        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun start(context: Context) {
            val i = Intent(context, FloatingOrbService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOrbService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var orbView: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        if (!canDrawOverlays(this)) {
            stopSelf()
            return
        }
        showOrb()
        isRunning = true
    }

    override fun onDestroy() {
        removeOrb()
        isRunning = false
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Jimi Orb",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Floating Jimi assistant"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jimi is active")
            .setContentText("Floating orb on screen · tap notification to open")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun showOrb() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = (56 * resources.displayMetrics.density).toInt()

        val frame = FrameLayout(this).apply {
            setBackgroundColor(0xFF00D4FF.toInt())
            // Soft circle look via outline
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            alpha = 0.92f
        }

        // Inner white core
        val core = View(this).apply {
            setBackgroundColor(0xDDFFFFFF.toInt())
            layoutParams = FrameLayout.LayoutParams(
                size / 3, size / 3,
                Gravity.CENTER
            ).also {
                // will apply circle after layout
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        frame.addView(core)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val lp = WindowManager.LayoutParams(
            size, size,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - 24
            y = resources.displayMetrics.heightPixels / 3
        }
        params = lp

        var lastX = 0
        var lastY = 0
        var startX = 0
        var startY = 0
        var moved = false

        frame.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    startX = lp.x
                    startY = lp.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    if (kotlin.math.abs(dx) > 4 || kotlin.math.abs(dy) > 4) moved = true
                    lp.x = startX + (event.rawX.toInt() - lastX)
                    // fix: track from start
                    lp.x = startX + event.rawX.toInt() - lastX
                    lp.y = startY + event.rawY.toInt() - lastY
                    // Actually startX is initial; need accumulate from down position
                    lp.x = startX + (event.rawX.toInt() - lastX)
                    lp.y = startY + (event.rawY.toInt() - lastY)
                    try {
                        windowManager?.updateViewLayout(frame, lp)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val intent = Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(frame, lp)
            orbView = frame
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun removeOrb() {
        try {
            orbView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        orbView = null
    }
}
