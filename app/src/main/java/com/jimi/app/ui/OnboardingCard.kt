package com.jimi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
            .border(1.dp, JimiPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .background(JimiCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ENGINE SETUP",
                    color = JimiPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Unlock full control",
                    color = JimiText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = JimiTextDim)
            }
        }
        Text(
            text = "1. Accessibility → screen taps, macros, FC Mobile\n" +
                    "2. Microphone → voice from orb\n" +
                    "3. Gemini key → smart reasoning\n" +
                    "4. Floating orb → always-on Jarvis",
            color = JimiTextDim,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenAccessibility) {
                Text("ENABLE CONTROL", color = JimiPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenSettings) {
                Text("API + ORB", color = JimiAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
