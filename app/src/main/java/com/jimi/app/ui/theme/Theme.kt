package com.jimi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Jarvis / Abdullah Arshad–style dark AI engine palette */
val JimiBlack = Color(0xFF05050A)
val JimiSurface = Color(0xFF0C0C14)
val JimiCard = Color(0xFF14141F)
val JimiCardElevated = Color(0xFF1C1C2A)
val JimiPrimary = Color(0xFF00E5FF)       // Electric cyan
val JimiSecondary = Color(0xFF8B5CFF)     // Violet
val JimiAccent = Color(0xFF00FFC6)        // Mint
val JimiPink = Color(0xFFFF5AC8)          // Soft magenta for energy
val JimiText = Color(0xFFF2F2F8)
val JimiTextDim = Color(0xFF8B8BA3)
val JimiError = Color(0xFFFF5E6C)
val JimiRing = Color(0x3300E5FF)

private val DarkColorScheme = darkColorScheme(
    primary = JimiPrimary,
    secondary = JimiSecondary,
    tertiary = JimiAccent,
    background = JimiBlack,
    surface = JimiSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = JimiText,
    onSurface = JimiText,
    error = JimiError
)

@Composable
fun JimiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
