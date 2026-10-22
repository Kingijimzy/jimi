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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
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

class MainActivity : ComponentActivity() {

    companion object {
        /** Set when floating orb long-press requests voice. */
        val pendingVoiceStart = MutableStateFlow(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LearnedSkillStore.loadIntoRegistry(applicationContext)
        handleOrbIntent(intent)
        enableEdgeToEdge()
        setContent {
            JimiTheme { JimiApp() }
        }
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

    var inputText by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "I am Jimi v0.3.1 — hybrid Jarvis on your phone.\n\n" +
                            "Floating orb: tap to open · long-press for voice.\n" +
                            "Chips work offline. Say: show orb | demo | open YouTube\n" +
                            "Accessibility unlocks macros & screen control.\n" +
                            "Add Gemini key in Settings for smart chat.",
                    isUser = false
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
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

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
                statusText = "Listening..."
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { voice.stop() }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) voice.start()
        else messages = messages + ChatMessage("Microphone permission is required for voice.", false)
    }

    fun startVoice() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) voice.start() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Long-press floating orb → open app + listen
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
            messages = messages + ChatMessage("Could not open Accessibility settings.", false)
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

    val quickActions = listOf(
        "demo" to "Demo",
        "time" to "Time",
        "battery" to "Battery",
        "flashlight on" to "Torch",
        "open YouTube" to "YouTube",
        "open Gmail" to "Gmail",
        "open Chrome" to "Chrome",
        "show orb" to "Orb",
        "network status" to "Network",
        "run macro fc daily" to "FC daily",
        "read screen" to "Screen",
        "list skills" to "Skills",
        "help" to "Help"
    )

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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JIMI",
                color = JimiPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = statusText,
                    color = JimiTextDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusChip(
                label = if (a11yOn) "Control ON" else "Control OFF",
                ok = a11yOn,
                onClick = { if (!a11yOn) openAccessibility() }
            )
            StatusChip(
                label = if (hasApiKey) "AI ready" else "Add API key",
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.32f),
            contentAlignment = Alignment.Center
        ) {
            JimiOrb(state = orbState)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(0.40f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { MessageBubble(it) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickActions.forEach { (cmd, label) ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = JimiCard,
                    modifier = Modifier.clickable(enabled = !isBusy) { submit(cmd) }
                ) {
                    Text(
                        text = label,
                        color = JimiPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .background(JimiCard, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { startVoice() }) {
                Icon(Icons.Default.Mic, "Voice", tint = JimiPrimary)
            }

            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textStyle = TextStyle(color = JimiText, fontSize = 16.sp),
                cursorBrush = SolidColor(JimiPrimary),
                decorationBox = { inner ->
                    if (inputText.isEmpty()) {
                        Text("Command or ask Jimi...", color = JimiTextDim, fontSize = 16.sp)
                    }
                    inner()
                }
            )

            IconButton(onClick = { submit(inputText) }) {
                Icon(
                    Icons.Default.Send,
                    "Send",
                    tint = if (inputText.isNotBlank()) JimiPrimary else JimiTextDim
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, ok: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (ok) JimiAccent.copy(alpha = 0.15f) else JimiError.copy(alpha = 0.15f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (ok) JimiAccent else JimiError,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (msg.isUser) JimiPrimary.copy(alpha = 0.15f) else JimiCard,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (msg.isUser) 16.dp else 4.dp,
                        bottomEnd = if (msg.isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(msg.text, color = JimiText, fontSize = 15.sp, lineHeight = 20.sp)
        }
    }
}
