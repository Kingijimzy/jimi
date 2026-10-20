package com.jimi.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jimi.app.ui.theme.JimiPrimary
import com.jimi.app.ui.theme.JimiText
import com.jimi.app.ui.theme.JimiTextDim

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Allow",
    dismissLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = JimiText) },
        text = { Text(message, color = JimiTextDim) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = JimiPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = JimiTextDim)
            }
        },
        containerColor = Color(0xFF1A1A24)
    )
}
