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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
        modifier = modifier.boxSize(280.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val r = minOf(w, h) / 2.6f * scale

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(c1.copy(alpha = 0.22f), c2.copy(alpha = 0.08f), Color.Transparent),
                    center, r * 2.1f
                ),
                radius = r * 2f,
                center = center
            )

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

            if (state == OrbState.THINKING || state == OrbState.ACTING) {
                val n = 10
                for (i in 0 until n) {
                    val ang = Math.toRadians((rotation * 1.4f + i * (360f / n)).toDouble())
                    val dist = r * 1.42f
                    drawCircle(
                        color = if (i % 2 == 0) c1 else c2,
                        radius = 3.2f + (i % 3),
                        center = Offset(
                            center.x + cos(ang).toFloat() * dist,
                            center.y + sin(ang).toFloat() * dist
                        )
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.95f),
                        c1.copy(alpha = 0.85f),
                        c2.copy(alpha = 0.55f),
                        c1.copy(alpha = 0.15f)
                    ),
                    Offset(center.x - r * 0.25f, center.y - r * 0.3f),
                    r * 1.35f
                ),
                radius = r,
                center = center
            )

            val highlight = Offset(center.x - r * 0.28f, center.y - r * 0.35f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                    highlight,
                    r * 0.45f
                ),
                radius = r * 0.4f,
                center = highlight
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = r * 0.18f,
                center = center
            )
        }
    }
}
