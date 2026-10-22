package com.jimi.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jimi.app.agent.ModelRouter
import com.jimi.app.prefs.AppPrefs
import com.jimi.app.service.FloatingOrbService
import com.jimi.app.skills.MacroStore
import com.jimi.app.ui.theme.*

@Composable
fun SettingsScreen(
    router: ModelRouter,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember { AppPrefs(context) }

    val providers = listOf(
        "gemini" to "Gemini — best free",
        "groq" to "Groq — fast free",
        "openrouter" to "OpenRouter — free (limited)",
        "deepseek" to "DeepSeek — paid later"
    )

    var keys by remember {
        mutableStateOf(providers.associate { (id, _) -> id to router.getKey(id) })
    }
    var ttsEnabled by remember { mutableStateOf(appPrefs.ttsEnabled) }
    var confirmSensitive by remember { mutableStateOf(appPrefs.confirmSensitive) }
    var macros by remember { mutableStateOf(MacroStore.list(context)) }
    var orbOn by remember { mutableStateOf(FloatingOrbService.isRunning) }
    var canOverlay by remember { mutableStateOf(FloatingOrbService.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                canOverlay = FloatingOrbService.canDrawOverlays(context)
                orbOn = FloatingOrbService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JimiBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = JimiPrimary)
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "SYSTEM",
                    color = JimiPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Engine settings",
                    color = JimiText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionLabel("ORB")
            ToggleRow(
                title = "Floating orb",
                subtitle = if (canOverlay) "Tap open · long-press voice" else "Needs overlay permission",
                checked = orbOn && canOverlay,
                onCheckedChange = { enable ->
                    if (enable) {
                        if (!FloatingOrbService.canDrawOverlays(context)) {
                            try {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                            }
                        } else {
                            FloatingOrbService.start(context)
                            orbOn = true
                        }
                    } else {
                        FloatingOrbService.stop(context)
                        orbOn = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("VOICE")
            ToggleRow(
                title = "Speak replies",
                subtitle = "TTS for engine responses",
                checked = ttsEnabled,
                onCheckedChange = {
                    ttsEnabled = it
                    appPrefs.ttsEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("SAFETY")
            ToggleRow(
                title = "Confirm sensitive actions",
                subtitle = "Calls and irreversible steps",
                checked = confirmSensitive,
                onCheckedChange = {
                    confirmSensitive = it
                    appPrefs.confirmSensitive = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("MACROS")
            if (macros.isEmpty()) {
                Text(
                    "No macros yet. Teach from the engine or run FC daily.",
                    color = JimiTextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                macros.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(1.dp, JimiPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .background(JimiCard, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, color = JimiText, fontSize = 14.sp)
                        IconButton(onClick = {
                            MacroStore.delete(context, name)
                            macros = MacroStore.list(context)
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = JimiError)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SectionLabel("AI LINK")
            Text(
                text = "Gemini: aistudio.google.com/apikey",
                color = JimiTextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            providers.forEach { (id, label) ->
                KeyField(
                    label = label,
                    value = keys[id] ?: "",
                    onValueChange = { new ->
                        keys = keys + (id to new)
                        router.setKey(id, new)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            TextButton(onClick = { appPrefs.onboardingDismissed = false }) {
                Text("Show engine setup again", color = JimiPrimary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = JimiPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JimiPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .background(JimiCard, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = JimiText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = JimiTextDim, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JimiPrimary,
                checkedTrackColor = JimiPrimary.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
private fun KeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = JimiText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JimiPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .background(JimiCard, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = JimiText, fontSize = 14.sp),
                cursorBrush = SolidColor(JimiPrimary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Paste key…", color = JimiTextDim, fontSize = 14.sp)
                    inner()
                }
            )
        }
    }
}
