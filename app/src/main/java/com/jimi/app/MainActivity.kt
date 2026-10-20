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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimi.app.ui.JimiOrb
import com.jimi.app.ui.OrbState
import com.jimi.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JimiTheme {
                JimiScreen()
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
fun JimiScreen() {
    var orbState by remember { mutableStateOf(OrbState.IDLE) }
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    val listState = rememberLazyListState()

    // Auto scroll when new message
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
        // Top status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
            Text(
                text = when (orbState) {
                    OrbState.IDLE -> "Ready"
                    OrbState.LISTENING -> "Listening..."
                    OrbState.THINKING -> "Thinking..."
                    OrbState.SPEAKING -> "Speaking..."
                    OrbState.ACTING -> "Working..."
                    OrbState.ERROR -> "Error"
                },
                color = JimiTextDim,
                fontSize = 13.sp
            )
        }

        // Orb section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f),
            contentAlignment = Alignment.Center
        ) {
            JimiOrb(state = orbState)
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(JimiCard, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                // TODO: Voice input
                orbState = OrbState.LISTENING
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
                textStyle = TextStyle(
                    color = JimiText,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(JimiPrimary),
                decorationBox = { inner ->
                    if (inputText.isEmpty()) {
                        Text(
                            "Talk to Jimi...",
                            color = JimiTextDim,
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userMsg = ChatMessage(inputText.trim(), true)
                        messages = messages + userMsg
                        inputText = ""

                        // Simulate thinking then reply (will be replaced by real agent)
                        orbState = OrbState.THINKING
                        // Temporary mock response
                        messages = messages + ChatMessage(
                            "I'm here. Full agent intelligence is being connected next.",
                            false
                        )
                        orbState = OrbState.IDLE
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
