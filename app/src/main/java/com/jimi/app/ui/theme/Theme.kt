package com.jimi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** ChatAI / Aurora dark palette — teal + violet aurora */
val JimiBlack = Color(0xFF0B0B0F)
val JimiSurface = Color(0xFF121218)
val JimiCard = Color(0xFF1A1A22)
val JimiCardElevated = Color(0xFF22222E)
val JimiPrimary = Color(0xFF2EE6D6)       // Teal
val JimiSecondary = Color(0xFF7B6CFF)     // Soft violet
val JimiAccent = Color(0xFF5B8CFF)        // Blue accent
val JimiPink = Color(0xFFE56BFF)
val JimiText = Color(0xFFF4F4F8)
val JimiTextDim = Color(0xFF8E8E9A)
val JimiError = Color(0xFFFF6B7A)
val JimiUserBubble = Color(0xFF252532)
val JimiAiSeam = Color(0xFF2EE6D6)

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
