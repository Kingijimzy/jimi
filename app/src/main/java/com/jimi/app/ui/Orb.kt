package com.jimi.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.jimi.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE, LISTENING, THINKING, SPEAKING, ACTING, ERROR
}

/**
 * Premium Jarvis-style orb inspired by Dribbble AI orbs
 * (Abdullah Arshad / agentic sphere aesthetics):
 * layered glow, rotating arcs, aurora core, state-driven motion.
 */
@Composable
fun JimiOrb(
    state: OrbState = OrbState.IDLE,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "orb")

    val breath by infinite.animateFloat(
        0.94f, 1.06f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val rotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    val rotationFast by infinite.animateFloat(
        0f, -360f,
        infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotFast"
    )
    val pulse by infinite.animateFloat(
        0.88f, 1.12f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val wave by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )

    val scale = when (state) {
        OrbState.IDLE -> breath
        OrbState.LISTENING -> breath * 1.08f
        OrbState.THINKING -> 1.02f
        OrbState.SPEAKING -> pulse
        OrbState.ACTING -> pulse * 1.08f
        OrbState.ERROR -> breath
    }

    val c1 = when (state) {
        OrbState.IDLE -> JimiPrimary
        OrbState.LISTENING -> JimiPrimary
        OrbState.THINKING -> JimiSecondary
        OrbState.SPEAKING -> JimiAccent
        OrbState.ACTING -> JimiPink
        OrbState.ERROR -> JimiError
    }
    val c2 = when (state) {
        OrbState.IDLE -> JimiSecondary
        OrbState.LISTENING -> JimiAccent
        OrbState.THINKING -> JimiPrimary
        OrbState.SPEAKING -> JimiPrimary
        OrbState.ACTING -> JimiPrimary
        OrbState.ERROR -> Color(0xFFFF8A80)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(280.dp)
    ) {
        Canvas(Modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2.6f * scale

            // Soft outer aurora glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c1.copy(alpha = 0.22f),
                        c2.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r * 2.1f
                ),
                radius = r * 2.0f,
                center = center
            )

            // Listening expanding waves
            if (state == OrbState.LISTENING || state == OrbState.SPEAKING) {
                for (i in 0..2) {
                    val t = ((wave + i / 3f) % 1f)
                    drawCircle(
                        color = c1.copy(alpha = (1f - t) * 0.28f),
                        radius = r * (1.15f + t * 0.85f),
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // Rotating dashed orbital rings
            rotate(rotation, center) {
                drawArc(
                    color = c1.copy(alpha = 0.45f),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.35f, center.y - r * 1.35f),
                    size = Size(r * 2.7f, r * 2.7f),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = c2.copy(alpha = 0.35f),
                    startAngle = 200f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.35f, center.y - r * 1.35f),
                    size = Size(r * 2.7f, r * 2.7f),
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )
            }
            rotate(rotationFast, center) {
                drawArc(
                    color = c2.copy(alpha = 0.4f),
                    startAngle = 60f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.15f, center.y - r * 1.15f),
                    size = Size(r * 2.3f, r * 2.3f),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                )
            }

            // Thinking / acting particles on orbit
            if (state == OrbState.THINKING || state == OrbState.ACTING) {
                val n = 10
                for (i in 0 until n) {
                    val ang = Math.toRadians((rotation * 1.4f + i * (360f / n)).toDouble())
                    val dist = r * 1.42f
                    val p = Offset(
                        center.x + cos(ang).toFloat() * dist,
                        center.y + sin(ang).toFloat() * dist
                    )
                    drawCircle(
                        color = if (i % 2 == 0) c1 else c2,
                        radius = 3.2f + (i % 3),
                        center = p
                    )
                }
            }

            // Glass sphere body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        c1.copy(alpha = 0.85f),
                        c2.copy(alpha = 0.55f),
                        c1.copy(alpha = 0.15f)
                    ),
                    center = Offset(center.x - r * 0.25f, center.y - r * 0.3f),
                    radius = r * 1.35f
                ),
                radius = r,
                center = center
            )

            // Specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.7f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - r * 0.28f, center.y - r * 0.35f),
                    radius = r * 0.45f
                ),
                radius = r * 0.4f,
                center = Offset(center.x - r * 0.28f, center.y - r * 0.35f)
            )

            // Inner core
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = r * 0.18f,
                center = center
            )
        }
    }
}
