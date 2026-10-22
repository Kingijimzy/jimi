package com.jimi.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size as boxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jimi.app.ui.theme.JimiAccent
import com.jimi.app.ui.theme.JimiError
import com.jimi.app.ui.theme.JimiPink
import com.jimi.app.ui.theme.JimiPrimary
import com.jimi.app.ui.theme.JimiSecondary
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE, LISTENING, THINKING, SPEAKING, ACTING, ERROR
}

/**
 * New 3D-style abstract orb (ChatAI / agentic sphere language):
 * mesh latitude lines, dual energy rings, aurora blob core, state motion.
 */
@Composable
fun JimiOrb(
    state: OrbState = OrbState.IDLE,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "orb3d")

    val breath by infinite.animateFloat(
        0.96f, 1.04f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val spin by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val spin2 by infinite.animateFloat(
        0f, -360f,
        infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin2"
    )
    val pulse by infinite.animateFloat(
        0.9f, 1.12f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val wave by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )
    val mesh by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "mesh"
    )

    val scale = when (state) {
        OrbState.IDLE -> breath
        OrbState.LISTENING -> breath * 1.06f
        OrbState.THINKING -> 1.03f
        OrbState.SPEAKING -> pulse
        OrbState.ACTING -> pulse * 1.05f
        OrbState.ERROR -> breath
    }

    val teal = JimiPrimary
    val violet = JimiSecondary
    val blue = JimiAccent
    val c1 = when (state) {
        OrbState.IDLE -> teal
        OrbState.LISTENING -> teal
        OrbState.THINKING -> violet
        OrbState.SPEAKING -> blue
        OrbState.ACTING -> JimiPink
        OrbState.ERROR -> JimiError
    }
    val c2 = when (state) {
        OrbState.IDLE -> violet
        OrbState.LISTENING -> blue
        OrbState.THINKING -> teal
        OrbState.SPEAKING -> teal
        OrbState.ACTING -> teal
        OrbState.ERROR -> Color(0xFFFF9AA5)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.boxSize(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val r = minOf(w, h) / 2.5f * scale

            // Soft ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(c1.copy(alpha = 0.28f), c2.copy(alpha = 0.1f), Color.Transparent),
                    center, r * 2.4f
                ),
                radius = r * 2.2f,
                center = center
            )

            // Listening rings
            if (state == OrbState.LISTENING || state == OrbState.SPEAKING) {
                for (i in 0..2) {
                    val t = ((wave + i / 3f) % 1f)
                    drawCircle(
                        color = c1.copy(alpha = (1f - t) * 0.35f),
                        radius = r * (1.2f + t * 0.9f),
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Outer energy ring
            rotate(spin, center) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(c1, c2, Color.Transparent, c1), center),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.32f, center.y - r * 1.32f),
                    size = Size(r * 2.64f, r * 2.64f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
            rotate(spin2, center) {
                drawArc(
                    color = c2.copy(alpha = 0.55f),
                    startAngle = 40f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.18f, center.y - r * 1.18f),
                    size = Size(r * 2.36f, r * 2.36f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }

            // Mesh latitude lines (3D sphere feel)
            val latCount = 5
            for (i in 1 until latCount) {
                val frac = i / latCount.toFloat()
                val yOff = (frac - 0.5f) * r * 1.6f
                val rx = r * sin(Math.PI * frac).toFloat() * 0.95f
                if (rx > 4f) {
                    drawOval(
                        color = Color.White.copy(alpha = 0.08f + 0.04f * sin(mesh * Math.PI * 2 + i).toFloat()),
                        topLeft = Offset(center.x - rx, center.y + yOff - 1.5f),
                        size = Size(rx * 2f, 3f),
                        style = Stroke(width = 1.2f)
                    )
                }
            }
            // Longitude arcs
            for (i in 0 until 6) {
                val ang = i * 30f + spin * 0.15f
                rotate(ang, center) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.07f),
                        startAngle = -70f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(center.x - r * 0.95f, center.y - r * 0.95f),
                        size = Size(r * 1.9f, r * 1.9f),
                        style = Stroke(width = 1f)
                    )
                }
            }

            // Thinking satellites
            if (state == OrbState.THINKING || state == OrbState.ACTING) {
                for (i in 0 until 8) {
                    val ang = Math.toRadians((spin * 1.5f + i * 45f).toDouble())
                    val dist = r * 1.38f
                    drawCircle(
                        color = if (i % 2 == 0) c1 else c2,
                        radius = 3.5f,
                        center = Offset(
                            center.x + cos(ang).toFloat() * dist,
                            center.y + sin(ang).toFloat() * dist
                        )
                    )
                }
            }

            // Core glass sphere
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.95f),
                        c1.copy(alpha = 0.9f),
                        c2.copy(alpha = 0.65f),
                        blue.copy(alpha = 0.35f),
                        Color(0xFF0B0B0F).copy(alpha = 0.4f)
                    ),
                    Offset(center.x - r * 0.2f, center.y - r * 0.28f),
                    r * 1.4f
                ),
                radius = r,
                center = center
            )

            // Specular
            val hi = Offset(center.x - r * 0.3f, center.y - r * 0.38f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
                    hi, r * 0.42f
                ),
                radius = r * 0.38f,
                center = hi
            )

            // Inner nucleus
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, c1.copy(alpha = 0.8f), Color.Transparent),
                    center, r * 0.35f
                ),
                radius = r * 0.22f,
                center = center
            )
        }
    }
}
