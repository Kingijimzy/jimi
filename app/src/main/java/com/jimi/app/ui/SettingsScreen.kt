package com.jimi.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimi.app.agent.ModelRouter
import com.jimi.app.prefs.AppPrefs
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
        "gemini" to "Gemini (Google AI Studio) — best free",
        "groq" to "Groq — fast free",
        "openrouter" to "OpenRouter (free models; often limited)",
        "deepseek" to "DeepSeek (paid later)"
    )

    var keys by remember {
        mutableStateOf(providers.associate { (id, _) -> id to router.getKey(id) })
    }
    var ttsEnabled by remember { mutableStateOf(appPrefs.ttsEnabled) }
    var confirmSensitive by remember { mutableStateOf(appPrefs.confirmSensitive) }
    var macros by remember { mutableStateOf(MacroStore.list(context)) }

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JimiPrimary
                )
            }
            Text(
                text = "Settings",
                color = JimiText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text("Voice", color = JimiPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            ToggleRow(
                title = "Speak replies (TTS)",
                checked = ttsEnabled,
                onCheckedChange = {
                    ttsEnabled = it
                    appPrefs.ttsEnabled = it
                }
            )
            Text(
                text = "Mic permission + Google speech. On Redmi: App info → Permissions → Microphone.",
                color = JimiTextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Text("Safety", color = JimiPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            ToggleRow(
                title = "Confirm sensitive actions",
                checked = confirmSensitive,
                onCheckedChange = {
                    confirmSensitive = it
                    appPrefs.confirmSensitive = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Macros", color = JimiPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            if (macros.isEmpty()) {
                Text("No macros yet. Say: teach daily rewards", color = JimiTextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            } else {
                macros.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
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

            Text("API Keys", color = JimiPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            Text(
                text = "Recommended: Gemini from Google AI Studio\n" +
                        "https://aistudio.google.com/apikey\n\n" +
                        "Paste the key only (usually starts with AIza).\n" +
                        "OpenRouter free quota runs out often — use Gemini or Groq.",
                color = JimiTextDim,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
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
                Spacer(modifier = Modifier.height(14.dp))
            }

            TextButton(onClick = { appPrefs.onboardingDismissed = false }) {
                Text("Show setup checklist again", color = JimiPrimary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JimiCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = JimiText, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JimiPrimary,
                checkedTrackColor = JimiPrimary.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun KeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = JimiText, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                    if (value.isEmpty()) Text("Paste API key here...", color = JimiTextDim, fontSize = 14.sp)
                    inner()
                }
            )
        }
    }
}
