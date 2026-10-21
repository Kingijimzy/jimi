package com.jimi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimi.app.ui.theme.*

@Composable
fun OnboardingCard(
    onOpenSettings: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(JimiCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Unlock full Jimi",
                color = JimiPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = JimiTextDim)
            }
        }
        Text(
            text = "Without setup, Jimi still controls volume, apps, torch, time, battery.\n\n" +
                    "For Jarvis-level control (tap screen, macros, FC Mobile):\n" +
                    "1. Turn ON Accessibility for Jimi (required)\n" +
                    "2. Allow Microphone for voice\n" +
                    "3. Add free Groq/Gemini key for smart chat\n\n" +
                    "Tip: tap the chips under the chat (Time, Torch, Chrome...).",
            color = JimiTextDim,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenAccessibility) {
                Text("Enable control", color = JimiPrimary, fontSize = 13.sp)
            }
            TextButton(onClick = onOpenSettings) {
                Text("API keys", color = JimiPrimary, fontSize = 13.sp)
            }
        }
    }
}
