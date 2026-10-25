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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jimi.app.ui.theme.JimiDotMint
import com.jimi.app.ui.theme.JimiError
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE, LISTENING, THINKING, SPEAKING, ACTING, ERROR
}

/**
 * Orion/Jimi assistant visual: a lightweight 24 × 11 dot-matrix face.
 * It deliberately avoids a glass orb or heavy 3D effects on low-memory phones.
 */
@Composable
fun JimiOrb(
    state: OrbState = OrbState.IDLE,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "matrix-face")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.THINKING -> 1700
                    OrbState.ACTING -> 900
                    OrbState.SPEAKING -> 500
                    else -> 2200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "matrix-phase"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "matrix-breathe"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawDotMatrixFace(state, phase, breathe)
        }
    }
}

private fun DrawScope.drawDotMatrixFace(state: OrbState, phase: Float, breathe: Float) {
    val columns = 24
    val rows = 11
    val spacing = minOf(size.width / 26f, size.height / 13f)
    val radius = spacing * 0.19f
    val gridWidth = (columns - 1) * spacing
    val gridHeight = (rows - 1) * spacing
    val startX = (size.width - gridWidth) / 2f
    val startY = (size.height - gridHeight) / 2f
    val baseColor = if (state == OrbState.ERROR) JimiError else JimiDotMint

    for (row in 0 until rows) {
        for (col in 0 until columns) {
            val dx = (col - 11.5f) / 11.5f
            val dy = (row - 5f) / 5.2f
            if (dx * dx + dy * dy > 1.05f) continue

            val leftEye = row in 4..5 && col in 6..8
            val rightEye = row in 4..5 && col in 15..17
            val mouth = row in 7..8 && col in 9..14
            val wave = (sin(phase * 1.6f + col * 0.37f - row * 0.21f) + 1f) / 2f
            val energy = when (state) {
                OrbState.IDLE -> 0.28f + wave * 0.20f
                OrbState.LISTENING -> 0.38f + wave * 0.62f
                OrbState.THINKING -> 0.25f + (0.75f * (1f - kotlin.math.abs(((col / 23f + phase / (PI * 2f)) % 1f) * 2f - 1f)))
                OrbState.SPEAKING -> 0.30f + wave * 0.70f
                OrbState.ACTING -> if ((col + row) % 4 == (phase * 2f).toInt() % 4) 1f else 0.34f + wave * 0.18f
                OrbState.ERROR -> 0.45f + wave * 0.45f
            }
            val facialDetail = when {
                leftEye || rightEye -> 0.18f + wave * 0.12f
                mouth -> 0.22f + wave * 0.18f
                else -> 1f
            }
            val alpha = (energy * facialDetail * breathe).coerceIn(0.08f, 1f)
            val radiusScale = when (state) {
                OrbState.LISTENING, OrbState.SPEAKING -> 0.88f + wave * 0.28f
                OrbState.THINKING -> 0.78f + wave * 0.32f
                OrbState.ACTING -> if ((col + row) % 4 == (phase * 2f).toInt() % 4) 1.25f else 0.8f
                else -> 0.9f + wave * 0.12f
            }
            val point = Offset(startX + col * spacing, startY + row * spacing)

            // Small, low-cost glow underneath the dot; no blur layers or glass surface.
            drawCircle(
                color = baseColor.copy(alpha = alpha * 0.12f),
                radius = radius * 2.3f * radiusScale,
                center = point
            )
            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = radius * radiusScale,
                center = point
            )
        }
    }
}
