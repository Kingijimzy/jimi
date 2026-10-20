package com.jimi.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jimi.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,       // Calm breathing
    LISTENING,  // Expanding rings
    THINKING,   // Spinning particles
    SPEAKING,   // Pulsing
    ACTING,     // Strong glow + activity
    ERROR       // Red warning
}

@Composable
fun JimiOrb(
    state: OrbState = OrbState.IDLE,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // Breathing animation (idle)
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Rotation for thinking
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse for speaking / acting
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scale = when (state) {
        OrbState.IDLE -> breath
        OrbState.LISTENING -> breath * 1.05f
        OrbState.THINKING -> 1f
        OrbState.SPEAKING -> pulse
        OrbState.ACTING -> pulse * 1.1f
        OrbState.ERROR -> breath
    }

    val coreColor = when (state) {
        OrbState.IDLE -> JimiPrimary
        OrbState.LISTENING -> JimiPrimary
        OrbState.THINKING -> JimiSecondary
        OrbState.SPEAKING -> JimiAccent
        OrbState.ACTING -> JimiPrimary
        OrbState.ERROR -> JimiError
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2.8f

            // Outer glow rings
            for (i in 1..3) {
                val ringScale = scale * (1f + i * 0.18f)
                val alpha = when (state) {
                    OrbState.IDLE -> 0.12f / i
                    OrbState.LISTENING -> 0.25f / i
                    OrbState.THINKING -> 0.18f / i
                    OrbState.SPEAKING -> 0.30f / i
                    OrbState.ACTING -> 0.35f / i
                    OrbState.ERROR -> 0.22f / i
                }
                drawCircle(
                    color = coreColor.copy(alpha = alpha),
                    radius = baseRadius * ringScale,
                    center = center,
                    style = Stroke(width = 2.5f * (4 - i))
                )
            }

            // Thinking particles
            if (state == OrbState.THINKING || state == OrbState.ACTING) {
                val particleCount = 8
                for (i in 0 until particleCount) {
                    val angle = Math.toRadians((rotation + i * (360f / particleCount)).toDouble())
                    val dist = baseRadius * 1.35f
                    val x = center.x + cos(angle).toFloat() * dist
                    val y = center.y + sin(angle).toFloat() * dist
                    drawCircle(
                        color = coreColor.copy(alpha = 0.7f),
                        radius = 4.5f,
                        center = Offset(x, y)
                    )
                }
            }

            // Core gradient circle
            val gradient = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = 0.95f),
                    coreColor.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * scale * 1.1f
            )
            drawCircle(
                brush = gradient,
                radius = baseRadius * scale,
                center = center
            )

            // Inner bright core
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = baseRadius * scale * 0.28f,
                center = center
            )
        }
    }
}
