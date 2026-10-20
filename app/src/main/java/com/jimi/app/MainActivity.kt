package com.jimi.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jimi.app.agent.Agent
import com.jimi.app.agent.ModelRouter
import com.jimi.app.agent.OrbStateManager
import com.jimi.app.prefs.AppPrefs
import com.jimi.app.ui.JimiOrb
import com.jimi.app.ui.OrbState
import com.jimi.app.ui.SettingsScreen
import com.jimi.app.ui.theme.*
import com.jimi.app.voice.VoiceInput
import com.jimi.app.voice.VoiceOutput

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JimiTheme { JimiApp() }
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
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var statusText by remember { mutableStateOf("Ready") }
    val listState = rememberLazyListState()

    val orbState by OrbStateManager.state.collectAsState()
    val isBusy = orbState != OrbState.IDLE

    val tts = remember { VoiceOutput(context) }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
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

    fun stopAll() {
        agent.stop()
        voice.stop()
        tts.stop()
        OrbStateManager.idle()
        statusText = "Ready"
        messages = messages + ChatMessage("Stopped.", false)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f),
            contentAlignment = Alignment.Center
        ) {
            JimiOrb(state = orbState)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(0.43f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { MessageBubble(it) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        Text("Talk to Jimi...", color = JimiTextDim, fontSize = 16.sp)
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
