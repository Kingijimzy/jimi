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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

data class EngineAction(
    val label: String,
    val subtitle: String,
    val command: String
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
                    "Engine online. Orb is your control core — not a chat box.\n" +
                            "Use modules below or speak a command.",
                    false
                )
            )
        )
    }
    var statusText by remember { mutableStateOf("STANDBY") }
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
            onStatus = { status -> statusText = status.uppercase() }
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
                statusText = "STANDBY"
            },
            onListening = {
                tts.stop()
                OrbStateManager.listening()
                statusText = "LISTENING"
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
        statusText = "STANDBY"
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
                statusText = "STANDBY"
            },
            onDismiss = {
                PendingActionBus.cancel()
                messages = messages + ChatMessage("Cancelled.", false)
                OrbStateManager.idle()
                statusText = "STANDBY"
            }
        )
    }

    val engineActions = listOf(
        EngineAction("Demo", "Prove control", "demo"),
        EngineAction("Torch", "Flashlight", "flashlight on"),
        EngineAction("Battery", "Power status", "battery"),
        EngineAction("Network", "Radar", "network status"),
        EngineAction("YouTube", "Open / search", "open YouTube"),
        EngineAction("Gmail", "Inbox", "open Gmail"),
        EngineAction("FC Daily", "Game macro", "run macro fc daily"),
        EngineAction("Screen", "Read UI", "read screen"),
        EngineAction("Orb", "Float on top", "show orb"),
        EngineAction("Skills", "Registry", "list skills"),
        EngineAction("Teach", "Record macro", "start teaching custom"),
        EngineAction("Home", "Go home", "go home")
    )

    val stateLabel = when (orbState) {
        OrbState.IDLE -> "READY"
        OrbState.LISTENING -> "LISTENING"
        OrbState.THINKING -> "THINKING"
        OrbState.SPEAKING -> "SPEAKING"
        OrbState.ACTING -> "ACTING"
        OrbState.ERROR -> "ERROR"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(JimiBlack, Color(0xFF0A0A18), JimiBlack)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "JIMI",
                        color = JimiPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Text(
                        "ENGINE  ·  v0.4.0",
                        color = JimiTextDim,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudPill(
                    text = if (a11yOn) "CONTROL ON" else "CONTROL OFF",
                    ok = a11yOn,
                    onClick = { if (!a11yOn) openAccessibility() }
                )
                HudPill(
                    text = if (hasApiKey) "AI LINKED" else "NO AI KEY",
                    ok = hasApiKey,
                    onClick = { if (!hasApiKey) onOpenSettings() }
                )
                HudPill(text = stateLabel, ok = orbState != OrbState.ERROR, onClick = {})
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
                    .weight(0.38f),
                contentAlignment = Alignment.Center
            ) {
                JimiOrb(
                    state = orbState,
                    modifier = Modifier
                        .size(260.dp)
                        .clickable { startVoice() }
                )
            }

            Text(
                text = statusText,
                color = JimiPrimary.copy(alpha = 0.9f),
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text(
                "MODULES",
                color = JimiTextDim,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.28f)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = true
            ) {
                items(engineActions) { action ->
                    ModuleTile(
                        title = action.label,
                        subtitle = action.subtitle,
                        enabled = !isBusy,
                        onClick = { submit(action.command) }
                    )
                }
            }

            Text(
                "ACTIVITY",
                color = JimiTextDim,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(0.18f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages.takeLast(12)) { msg ->
                    ActivityLine(msg)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .border(1.dp, JimiPrimary.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                    .background(JimiCard, RoundedCornerShape(28.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { startVoice() }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(JimiPrimary.copy(alpha = 0.15f)),
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
                            Text("Command the engine…", color = JimiTextDim, fontSize = 15.sp)
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
}

@Composable
private fun HudPill(text: String, ok: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (ok) JimiPrimary.copy(alpha = 0.12f) else JimiError.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (ok) JimiPrimary else JimiError,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ModuleTile(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JimiCard)
            .border(1.dp, JimiPrimary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            title,
            color = JimiText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            subtitle,
            color = JimiTextDim,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivityLine(msg: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (msg.isUser) JimiPrimary.copy(alpha = 0.08f) else JimiCard,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (msg.isUser) "YOU  " else "JIMI  ",
            color = if (msg.isUser) JimiPrimary else JimiAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = msg.text,
            color = JimiText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
