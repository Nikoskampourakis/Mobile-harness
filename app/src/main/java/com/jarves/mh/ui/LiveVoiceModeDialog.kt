package com.jarves.mh.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.jarves.mh.model.AgentKind
import com.jarves.mh.runtime.TtsManager
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import java.util.Locale

enum class CameraState {
    DISABLED,
    FRONT,
    REAR,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveVoiceModeDialog(
    agentKind: AgentKind,
    agentTitle: String,
    isRunning: Boolean,
    latestAssistantMessage: String?,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSteerMessage: (String) -> Unit,
    ttsManager: TtsManager,
) {
    val context = LocalContext.current
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var captionsEnabled by rememberSaveable { mutableStateOf(true) }
    var cameraState by rememberSaveable { mutableStateOf(CameraState.DISABLED) }
    var showKeyboardInput by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var textInput by rememberSaveable { mutableStateOf("") }
    var liveTranscript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val hasMicPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    var micGranted by remember { mutableStateOf(hasMicPermission) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required for voice mode", Toast.LENGTH_SHORT).show()
            isMuted = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraState = CameraState.FRONT
        } else {
            cameraState = CameraState.DISABLED
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Continuous STT Speech Recognizer
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val startListening = {
        if (!isMuted && micGranted && SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching {
                speechRecognizer?.cancel()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizer?.startListening(intent)
                isListening = true
            }
        }
    }

    DisposableEffect(context, micGranted, isMuted) {
        if (micGranted && !isMuted && SpeechRecognizer.isRecognitionAvailable(context)) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) { isListening = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false }
                    override fun onError(error: Int) {
                        isListening = false
                        // Automatically restart listening if still in voice mode and not muted
                        if (!isMuted) {
                            startListening()
                        }
                    }
                    override fun onResults(results: android.os.Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            liveTranscript = text
                            if (isRunning) {
                                onSteerMessage(text)
                            } else {
                                onSendMessage(text)
                            }
                        }
                        if (!isMuted) startListening()
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { liveTranscript = it }
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
            }
            speechRecognizer = recognizer
            startListening()

            onDispose {
                recognizer.cancel()
                recognizer.destroy()
                speechRecognizer = null
            }
        } else {
            onDispose {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
        }
    }

    // Auto-speak new assistant messages using independent local TTS
    LaunchedEffect(latestAssistantMessage) {
        if (!latestAssistantMessage.isNullOrBlank()) {
            ttsManager.speak(latestAssistantMessage, force = true)
        }
    }

    // Interactive Pulsing Animation for Ambient Voice Orb
    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isRunning || isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseScale",
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "RingRotation",
    )

    Dialog(
        onDismissRequest = {
            ttsManager.instantInterrupt()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.Escape -> {
                            ttsManager.instantInterrupt()
                            onDismiss()
                            true
                        }
                        Key.Spacebar -> {
                            isMuted = !isMuted
                            true
                        }
                        Key.I -> {
                            ttsManager.instantInterrupt()
                            true
                        }
                        Key.C -> {
                            captionsEnabled = !captionsEnabled
                            true
                        }
                        else -> false
                    }
                }
                .focusable(),
            color = Color(0xFF0F172A), // Deep obsidian midnight
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Top Bar: Agent Info + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isRunning) PocketOrange else if (isListening) PocketGreen else Color(0xFF38BDF8)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    agentTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            Text(
                                when {
                                    isMuted -> "Microphone muted"
                                    isRunning -> "Agent is working & listening for steering…"
                                    isListening -> "Listening to your voice…"
                                    else -> "Live Voice Mode active"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(Icons.Default.Settings, "Voice Settings", tint = Color.White)
                            }
                            IconButton(onClick = {
                                ttsManager.instantInterrupt()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, "Close Voice Mode", tint = Color.White)
                            }
                        }
                    }

                    // Center Area: Interactive Soundwave Orb & Optional Camera Viewfinder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        // Camera viewfinder preview when camera is active
                        if (cameraState != CameraState.DISABLED) {
                            Box(
                                modifier = Modifier
                                    .size(width = 240.dp, height = 160.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black)
                                    .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CameraPreview(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraState = cameraState,
                                )
                                
                                // Overlay info
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (cameraState == CameraState.FRONT) "FRONT" else "REAR",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // Central Animated Orb
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(pulseScale)
                                .graphicsLayer { rotationZ = ringRotation },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                when {
                                                    isRunning -> PocketOrange.copy(alpha = 0.6f)
                                                    isListening -> PocketGreen.copy(alpha = 0.6f)
                                                    else -> Color(0xFF38BDF8).copy(alpha = 0.5f)
                                                },
                                                Color.Transparent,
                                            ),
                                        ),
                                    ),
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                when {
                                                    isRunning -> PocketOrange
                                                    isListening -> PocketGreen
                                                    else -> Color(0xFF38BDF8)
                                                },
                                                Color(0xFF818CF8),
                                            ),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    when {
                                        isMuted -> Icons.Default.MicOff
                                        isRunning -> Icons.Default.GraphicEq
                                        else -> Icons.Default.Mic
                                    },
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Live Captions / Subtitles
                        if (captionsEnabled) {
                            Surface(
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    if (liveTranscript.isNotBlank()) {
                                        Text(
                                            "You: $liveTranscript",
                                            fontSize = 13.sp,
                                            color = Color(0xFFE2E8F0),
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                    if (!latestAssistantMessage.isNullOrBlank()) {
                                        if (liveTranscript.isNotBlank()) Spacer(Modifier.height(6.dp))
                                        Text(
                                            "$agentTitle: ${latestAssistantMessage.take(240)}…",
                                            fontSize = 13.sp,
                                            color = Color(0xFF38BDF8),
                                            lineHeight = 18.sp,
                                        )
                                    }
                                    if (liveTranscript.isBlank() && latestAssistantMessage.isNullOrBlank()) {
                                        Text(
                                            "Speak naturally. Agent answers via local streaming speech.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Keyboard input bar when user toggles keyboard mode
                    AnimatedVisibility(visible = showKeyboardInput) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .imePadding(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = { Text("Type instruction or steer agent…", fontSize = 13.sp, color = Color(0xFF64748B)) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (textInput.isNotBlank()) {
                                            val msg = textInput.trim()
                                            textInput = ""
                                            if (isRunning) onSteerMessage(msg) else onSendMessage(msg)
                                        }
                                    }),
                                    singleLine = true,
                                )
                                IconButton(
                                    onClick = {
                                        if (textInput.isNotBlank()) {
                                            val msg = textInput.trim()
                                            textInput = ""
                                            if (isRunning) onSteerMessage(msg) else onSendMessage(msg)
                                        }
                                    },
                                    enabled = textInput.isNotBlank(),
                                ) {
                                    Icon(Icons.Default.Send, "Send text", tint = if (textInput.isNotBlank()) Color(0xFF38BDF8) else Color(0xFF475569))
                                }
                            }
                        }
                    }

                    // Bottom Voice Controls
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Mic Toggle
                            IconButton(
                                onClick = {
                                    if (!micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        isMuted = !isMuted
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isMuted) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF334155),
                                ),
                            ) {
                                Icon(
                                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    "Toggle Mute",
                                    tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                                )
                            }

                            // Instant Interrupt Button (<100ms cutoff)
                            IconButton(
                                onClick = { ttsManager.instantInterrupt() },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF334155)),
                            ) {
                                Icon(Icons.Default.Stop, "Interrupt speech", tint = Color(0xFFF59E0B))
                            }

                            // Captions Toggle
                            IconButton(
                                onClick = { captionsEnabled = !captionsEnabled },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (captionsEnabled) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF334155),
                                ),
                            ) {
                                Icon(
                                    if (captionsEnabled) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                                    "Toggle Captions",
                                    tint = if (captionsEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                )
                            }

                            // Camera Switch / Toggle
                            IconButton(
                                onClick = {
                                    when (cameraState) {
                                        CameraState.DISABLED -> {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                cameraState = CameraState.FRONT
                                            } else {
                                                cameraLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                        CameraState.FRONT -> cameraState = CameraState.REAR
                                        CameraState.REAR -> cameraState = CameraState.DISABLED
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (cameraState != CameraState.DISABLED) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF334155),
                                ),
                            ) {
                                Icon(
                                    when (cameraState) {
                                        CameraState.DISABLED -> Icons.Default.VideocamOff
                                        CameraState.FRONT -> Icons.Default.Cameraswitch
                                        CameraState.REAR -> Icons.Default.Videocam
                                    },
                                    "Camera toggle",
                                    tint = if (cameraState != CameraState.DISABLED) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                )
                            }

                            // Keyboard Input Toggle
                            IconButton(
                                onClick = { showKeyboardInput = !showKeyboardInput },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (showKeyboardInput) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF334155),
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Keyboard,
                                    "Keyboard input",
                                    tint = if (showKeyboardInput) Color(0xFF38BDF8) else Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Voice Settings Bottom Sheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Voice & Speech Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // Speaking Rate
                var speechRate by remember { mutableFloatStateOf(1.0f) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Speaking rate", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(String.format(Locale.US, "%.1fx", speechRate), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        ttsManager.updateSpeechRate(it)
                    },
                    valueRange = 0.5f..2.0f,
                )

                Spacer(Modifier.height(12.dp))

                // Pitch
                var speechPitch by remember { mutableFloatStateOf(1.0f) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Voice pitch", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(String.format(Locale.US, "%.1fx", speechPitch), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = speechPitch,
                    onValueChange = {
                        speechPitch = it
                        ttsManager.updatePitch(it)
                    },
                    valueRange = 0.5f..1.8f,
                )

                Spacer(Modifier.height(12.dp))

                // Test voice
                OutlinedButton(
                    onClick = {
                        ttsManager.speak("Hello! Local streaming speech synthesis is ready.", force = true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test voice synthesis")
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraState: CameraState,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val previewView = remember { PreviewView(context) }
    val cameraSelector = remember(cameraState) {
        if (cameraState == CameraState.FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
