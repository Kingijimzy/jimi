package com.jimi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Classic black / graphite / silver / white palette. */
val JimiBlack = Color(0xFF08090B)
val JimiSurface = Color(0xFF111317)
val JimiCard = Color(0xFF191C21)
val JimiCardElevated = Color(0xFF23272E)
val JimiPrimary = Color(0xFFD3D8DE)       // Silver
val JimiSecondary = Color(0xFF9098A2)     // Graphite silver
val JimiAccent = Color(0xFFF7F8FA)        // White
val JimiPink = Color(0xFFBEC5CD)          // Neutral highlight
val JimiText = Color(0xFFF4F5F6)
val JimiTextDim = Color(0xFFA0A7B0)
val JimiError = Color(0xFFFF6978)
val JimiUserBubble = Color(0xFF252A31)
val JimiAiSeam = Color(0xFFBEC5CD)
val JimiDotMint = Color(0xFF8FFFD2)

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
fun JimiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}
