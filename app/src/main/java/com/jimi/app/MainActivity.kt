package com.jimi.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jimi.app.agent.Agent
import com.jimi.app.agent.ModelRouter
import com.jimi.app.agent.OrbStateManager
import com.jimi.app.agent.PendingAction
import com.jimi.app.agent.PendingActionBus
import com.jimi.app.prefs.AppPrefs
import com.jimi.app.service.FloatingOrbService
import com.jimi.app.skills.LearnedSkillStore
import com.jimi.app.tools.ScreenTools
import com.jimi.app.ui.ConfirmDialog
import com.jimi.app.ui.JimiOrb
import com.jimi.app.ui.OnboardingCard
import com.jimi.app.ui.OrbState
import com.jimi.app.ui.SettingsScreen
import com.jimi.app.ui.theme.*
import com.jimi.app.voice.VoiceInput
import com.jimi.app.voice.VoiceOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar

class MainActivity : ComponentActivity() {

    companion object {
        val pendingVoiceStart = MutableStateFlow(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LearnedSkillStore.loadIntoRegistry(applicationContext)
        handleOrbIntent(intent)
        enableEdgeToEdge()
        setContent { JimiTheme { JimiApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOrbIntent(intent)
    }

    private fun handleOrbIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(FloatingOrbService.EXTRA_START_VOICE, false) == true) {
            pendingVoiceStart.value = true
            intent.removeExtra(FloatingOrbService.EXTRA_START_VOICE)
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun JimiApp() {
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val router = remember { ModelRouter(context) }
    if (showSettings) {
        SettingsScreen(router = router, onBack = { showSettings = false })
    } else {
        JimiScreen(onOpenSettings = { showSettings = true })
    }
}

@Composable
fun JimiScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val appPrefs = remember { AppPrefs(context) }
    val router = remember { ModelRouter(context) }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    var inputText by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    "I'm Jimi — your phone AI. Control apps, macros, screen, and research.\n" +
                            "Try a suggestion below, or just talk to me.",
                    false
                )
            )
        )
    }
    var statusText by remember { mutableStateOf("Ready") }
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    var showOnboarding by remember { mutableStateOf(!appPrefs.onboardingDismissed) }
    var a11yOn by remember { mutableStateOf(ScreenTools.isReady()) }
    var hasApiKey by remember { mutableStateOf(router.availableProviders().isNotEmpty()) }
    val listState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                a11yOn = ScreenTools.isReady()
                hasApiKey = router.availableProviders().isNotEmpty()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val orbState by OrbStateManager.state.collectAsState()
    val isBusy = orbState != OrbState.IDLE

    val tts = remember { VoiceOutput(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    DisposableEffect(Unit) {
        PendingActionBus.setListener { pending = it }
        onDispose { PendingActionBus.setListener { } }
    }

    val agent = remember {
        Agent(
            context = context,
            onMessage = { text ->
                messages = messages + ChatMessage(text, false)
                tts.enabled = appPrefs.ttsEnabled
                if (appPrefs.ttsEnabled) tts.speak(text)
            },
            onStatus = { status -> statusText = status }
        )
    }

    fun submit(text: String) {
        val t = text.trim()
        if (t.isBlank()) return
        tts.stop()
        messages = messages + ChatMessage(t, true)
        inputText = ""
        agent.run(t)
    }

    val voice = remember {
        VoiceInput(
            context = context,
            onResult = { text -> submit(text) },
            onError = { err ->
                messages = messages + ChatMessage(err, false)
                OrbStateManager.idle()
                statusText = "Ready"
            },
            onListening = {
                tts.stop()
                OrbStateManager.listening()
                statusText = "Listening…"
            }
        )
    }
    DisposableEffect(Unit) { onDispose { voice.stop() } }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) voice.start()
        else messages = messages + ChatMessage("Microphone permission required.", false)
    }

    fun startVoice() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) voice.start() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val wantVoice by MainActivity.pendingVoiceStart.collectAsState()
    LaunchedEffect(wantVoice) {
        if (wantVoice) {
            MainActivity.pendingVoiceStart.value = false
            delay(450)
            startVoice()
        }
    }

    fun stopAll() {
        agent.stop()
        voice.stop()
        tts.stop()
        PendingActionBus.cancel()
        OrbStateManager.idle()
        statusText = "Ready"
        messages = messages + ChatMessage("Stopped.", false)
    }

    fun openAccessibility() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    if (pending != null) {
        ConfirmDialog(
            title = pending!!.title,
            message = pending!!.message,
            onConfirm = {
                val result = PendingActionBus.confirm()
                if (result != null) {
                    messages = messages + ChatMessage(result, false)
                    if (appPrefs.ttsEnabled) tts.speak(result)
                }
                OrbStateManager.idle()
                statusText = "Ready"
            },
            onDismiss = {
                PendingActionBus.cancel()
                messages = messages + ChatMessage("Cancelled.", false)
                OrbStateManager.idle()
                statusText = "Ready"
            }
        )
    }

    val suggestions = listOf(
        "demo" to "Demo",
        "open YouTube" to "YouTube",
        "open Gmail" to "Gmail",
        "battery" to "Battery",
        "show orb" to "Orb",
        "run macro fc daily" to "FC daily",
        "read screen" to "Screen",
        "network status" to "Network"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JimiBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar — ChatAI style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$greeting",
                    color = JimiText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isBusy) statusText else "How can Jimi help you?",
                    color = JimiTextDim,
                    fontSize = 13.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBusy) {
                    IconButton(onClick = { stopAll() }) {
                        Icon(Icons.Default.Stop, "Stop", tint = JimiError)
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, "Settings", tint = JimiTextDim)
                }
            }
        }

        // Status row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusDot(
                label = if (a11yOn) "Control" else "Control off",
                ok = a11yOn,
                onClick = { if (!a11yOn) openAccessibility() }
            )
            StatusDot(
                label = if (hasApiKey) "AI" else "Add key",
                ok = hasApiKey,
                onClick = { if (!hasApiKey) onOpenSettings() }
            )
        }

        if (showOnboarding) {
            OnboardingCard(
                onOpenSettings = onOpenSettings,
                onOpenAccessibility = { openAccessibility() },
                onDismiss = {
                    appPrefs.onboardingDismissed = true
                    showOnboarding = false
                }
            )
        }

        // Compact hero orb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft glow backdrop
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(JimiPrimary.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            JimiOrb(
                state = orbState,
                size = 140.dp,
                modifier = Modifier.clickable { startVoice() }
            )
        }

        // Suggestion chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { (cmd, label) ->
                SuggestionChip(
                    label = label,
                    enabled = !isBusy,
                    onClick = { submit(cmd) }
                )
            }
        }

        // Chat — Jarvis / Lumen style
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                if (msg.isUser) UserBubble(msg.text) else AiBubble(msg.text)
            }
        }

        // Glass composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            JimiPrimary.copy(alpha = 0.35f),
                            JimiSecondary.copy(alpha = 0.25f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .background(JimiCard, RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { startVoice() }) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(JimiPrimary.copy(alpha = 0.25f), JimiSecondary.copy(alpha = 0.2f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, "Voice", tint = JimiPrimary)
                }
            }
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textStyle = TextStyle(color = JimiText, fontSize = 15.sp),
                cursorBrush = SolidColor(JimiPrimary),
                decorationBox = { inner ->
                    if (inputText.isEmpty()) {
                        Text("Message Jimi…", color = JimiTextDim, fontSize = 15.sp)
                    }
                    inner()
                }
            )
            IconButton(onClick = { submit(inputText) }) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank())
                                Brush.linearGradient(listOf(JimiPrimary, JimiSecondary))
                            else
                                Brush.linearGradient(listOf(JimiCardElevated, JimiCardElevated))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        "Send",
                        tint = if (inputText.isNotBlank()) Color.Black else JimiTextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(label: String, ok: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(JimiCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (ok) JimiPrimary else JimiError)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = JimiTextDim, fontSize = 11.sp)
    }
}

@Composable
private fun SuggestionChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(JimiCard)
            .border(1.dp, JimiPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, color = JimiText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                .background(JimiUserBubble)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(text, color = JimiText, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun AiBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // Aurora seam
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp))
                .background(JimiCard)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            JimiPrimary.copy(alpha = 0.45f),
                            JimiSecondary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp)
                )
                .padding(start = 4.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(JimiPrimary, JimiSecondary))
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "JIMI",
                        color = JimiPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text, color = JimiText, fontSize = 15.sp, lineHeight = 21.sp)
            }
        }
    }
}
