package com.jimi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.jimi.app.agent.Agent
import com.jimi.app.agent.ModelRouter
import com.jimi.app.agent.OrbStateManager
import com.jimi.app.ui.JimiOrb
import com.jimi.app.ui.SettingsScreen
import com.jimi.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JimiTheme {
                JimiApp()
            }
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
        SettingsScreen(
            router = router,
            onBack = { showSettings = false }
        )
    } else {
        JimiScreen(
            onOpenSettings = { showSettings = true }
        )
    }
}

@Composable
fun JimiScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var statusText by remember { mutableStateOf("Ready") }
    val listState = rememberLazyListState()

    val orbState by OrbStateManager.state.collectAsState()

    val agent = remember {
        Agent(
            context = context,
            onMessage = { text ->
                messages = messages + ChatMessage(text, false)
            },
            onStatus = { status ->
                statusText = status
            }
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
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
                    modifier = Modifier.padding(end = 12.dp)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = JimiTextDim
                    )
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
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(JimiCard, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                OrbStateManager.listening()
                statusText = "Listening..."
            }) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = JimiPrimary
                )
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

            IconButton(
                onClick = {
                    val text = inputText.trim()
                    if (text.isNotBlank()) {
                        messages = messages + ChatMessage(text, true)
                        inputText = ""
                        agent.run(text)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
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
            Text(
                text = msg.text,
                color = JimiText,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}
