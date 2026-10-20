package com.jimi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Elegant dark palette inspired by Jarvis
val JimiBlack = Color(0xFF0A0A0F)
val JimiSurface = Color(0xFF12121A)
val JimiCard = Color(0xFF1A1A24)
val JimiPrimary = Color(0xFF00D4FF)      // Cyan glow
val JimiSecondary = Color(0xFF7B61FF)    // Soft purple
val JimiAccent = Color(0xFF00FFA3)       // Soft green
val JimiText = Color(0xFFE8E8F0)
val JimiTextDim = Color(0xFF9A9AB0)
val JimiError = Color(0xFFFF6B6B)

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
    darkTheme: Boolean = true, // Always dark for Jarvis feel
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
