package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.AudioRecorderHelper
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.theme.LocalTabiThemeTokens
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.LiveVoiceState
import com.example.ui.viewmodel.TabiAiViewModel
import kotlinx.coroutines.delay

@Composable
fun LiveVoiceAndTranscribeSection(
    aiViewModel: TabiAiViewModel,
    onSpeakText: (String, Boolean) -> Unit
) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val liveState by aiViewModel.liveVoiceState.collectAsStateWithLifecycle()
    val liveTranscripts by aiViewModel.liveTranscripts.collectAsStateWithLifecycle()
    val transcriptionState by aiViewModel.transcriptionState.collectAsStateWithLifecycle()

    val recorder = remember(context) { AudioRecorderHelper(context) }
    var isRecordingMemo by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var micError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recorder.stopSilently()
        }
    }

    LaunchedEffect(isRecordingMemo) {
        if (isRecordingMemo) {
            recordSeconds = 0
            while (isRecordingMemo) {
                delay(1000L)
                recordSeconds++
            }
        }
    }

    val startMemoRecording = {
        runCatching {
            recorder.startRecording()
            isRecordingMemo = true
            micError = null
        }.onFailure { e ->
            micError = e.localizedMessage ?: "Unable to start microphone recorder."
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingAction) {
                "live" -> aiViewModel.startLiveVoiceConversation()
                "memo" -> startMemoRecording()
            }
        } else {
            micError = "Microphone permission is required for Live Voice & Audio Transcription."
        }
        pendingAction = null
    }

    val ensureMicThen: (String, () -> Unit) -> Unit = { actionKey, onGranted ->
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onGranted()
        else {
            pendingAction = actionKey
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val tokens = LocalTabiThemeTokens.current
    val orbTransition = rememberInfiniteTransition(label = "live_orb")
    val orbScale by orbTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Gemini Live API Real-time Bidirectional Audio Conversation
        LiquidGlassCard(elevated = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Live Voice Conversation (Gemini Live API)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Real-time low-latency Japanese & English speech session", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val activeScale = if (liveState == LiveVoiceState.Active) orbScale else 1f
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = activeScale
                            scaleY = activeScale
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    tokens.auroraPrimary,
                                    tokens.auroraSecondary.copy(alpha = 0.72f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (liveState == LiveVoiceState.Connecting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Live voice orb",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (val s = liveState) {
                LiveVoiceState.Idle -> {
                    Button(
                        onClick = {
                            haptics.confirm()
                            ensureMicThen("live") { aiViewModel.startLiveVoiceConversation() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_live_voice_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Real-Time Voice Conversation", fontWeight = FontWeight.Bold)
                    }
                }
                LiveVoiceState.Connecting -> {
                    Text(
                        text = "Connecting bidirectional Gemini Live stream...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                LiveVoiceState.Active -> {
                    Button(
                        onClick = {
                            haptics.toggle()
                            aiViewModel.stopLiveVoiceConversation()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stop_live_voice_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End Live Conversation", fontWeight = FontWeight.Bold)
                    }
                }
                is LiveVoiceState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            haptics.tap()
                            ensureMicThen("live") { aiViewModel.startLiveVoiceConversation() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Retry Live Voice", fontWeight = FontWeight.Bold) }
                }
            }

            if (liveTranscripts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Live Transcript", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        liveTranscripts.takeLast(8).forEach { line ->
                            Text(
                                text = "${line.speaker}: ${line.text}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // 2. Microphone Audio Recorder + Gemini Transcription
        LiquidGlassCard(elevated = false) {
            Text("Audio Memo & Japanese Transcriber", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Record a train announcement, menu conversation, or spoken phrase—Gemini transcribes & translates with nuance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (micError != null) {
                Text(micError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (isRecordingMemo) {
                Button(
                    onClick = {
                        haptics.confirm()
                        isRecordingMemo = false
                        val bytes = recorder.stopAndReadBytes()
                        if (bytes != null) {
                            aiViewModel.transcribeRecordedAudio(bytes)
                        } else {
                            micError = "Captured audio clip was empty. Try speaking closer to the mic."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("stop_audio_record_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop & Transcribe (${recordSeconds}s)", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        haptics.tap()
                        ensureMicThen("memo") { startMemoRecording() }
                    },
                    enabled = transcriptionState !is AiUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_audio_record_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record Audio for Transcription", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (val st = transcriptionState) {
                AiUiState.Idle -> Unit
                AiUiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Transcribing Japanese & English audio...")
                    }
                }
                is AiUiState.Error -> {
                    Text(st.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                is AiUiState.Success -> {
                    val res = st.data
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Detected: ${res.detectedLanguage}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = {
                                    haptics.tap()
                                    onSpeakText(res.japaneseTranscript, true)
                                }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak transcript")
                                }
                            }
                            Text(res.japaneseTranscript, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(res.readingOrRomaji, style = MaterialTheme.typography.labelSmall)
                            HorizontalDivider()
                            Text(res.englishTranslation, style = MaterialTheme.typography.bodyLarge)
                            Text("Nuance: ${res.nuanceNote}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
