package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GuideContent
import com.example.data.model.LearningModule
import com.example.data.model.LessonProgress
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.theme.LocalTabiThemeTokens
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.SyncState
import com.example.ui.viewmodel.TabiAiViewModel
import com.example.ui.viewmodel.TabiMainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnScreen(
    mainViewModel: TabiMainViewModel,
    aiViewModel: TabiAiViewModel,
    onSpeakJapanese: (String) -> Unit
) {
    val progressState by mainViewModel.lessonProgressState.collectAsStateWithLifecycle()
    val conversationState by aiViewModel.conversationState.collectAsStateWithLifecycle()
    val haptics = rememberTabiHaptics()

    val completedMap = remember(progressState) {
        when (val s = progressState) {
            is SyncState.Success -> s.data.associateBy { it.lessonId }
            else -> emptyMap()
        }
    }

    var selectedModule by remember { mutableStateOf(GuideContent.learningModules.first()) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Phrases, 1: Quiz, 2: Write, 3: Speak AI

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("日本語学院 • Japanese Academy", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "3D flip flashcards, stroke writing, N2/N1 nuances & live voice pronunciation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Module selector chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GuideContent.learningModules.forEach { module ->
                    val prog = completedMap[module.id]
                    FilterChip(
                        selected = selectedModule.id == module.id,
                        onClick = {
                            haptics.tap()
                            selectedModule = module
                            aiViewModel.resetConversationState()
                        },
                        leadingIcon = if (prog?.completed == true) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = { Text(module.level) }
                    )
                }
            }
        }

        // Selected Module Header Card
        item {
            val prog: LessonProgress? = completedMap[selectedModule.id]
            LiquidGlassCard(elevated = true, contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${selectedModule.level.uppercase()} • ${selectedModule.japaneseTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(selectedModule.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        selectedModule.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (prog != null && prog.completed) {
                        LinearProgressIndicator(
                            progress = { (prog.score / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "Best Score: ${prog.score}% • ${prog.masteredPhrases} phrases mastered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Exercise Mode Tabs
        item {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = LocalTabiThemeTokens.current.glassSurfaceElevated,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(selected = activeTab == 0, onClick = { haptics.tap(); activeTab = 0 }, text = { Text("3D Cards") })
                Tab(selected = activeTab == 1, onClick = { haptics.tap(); activeTab = 1 }, text = { Text("Quiz") })
                Tab(selected = activeTab == 2, onClick = { haptics.tap(); activeTab = 2 }, text = { Text("Write") })
                Tab(selected = activeTab == 3, onClick = { haptics.tap(); activeTab = 3 }, text = { Text("Speak AI") })
            }
        }

        item {
            AnimatedContent(
                targetState = Pair(activeTab, selectedModule.id),
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                },
                label = "learn_tab_transition"
            ) { (tab, _) ->
                when (tab) {
                    0 -> PhrasesPracticeSection(
                        module = selectedModule,
                        onSpeakJapanese = onSpeakJapanese,
                        onMarkMastered = {
                            haptics.celebrateModule()
                            mainViewModel.recordLessonCompletion(
                                lessonId = selectedModule.id,
                                score = 100L,
                                masteredPhrases = selectedModule.phrases.size.toLong()
                            )
                        }
                    )
                    1 -> QuizSection(
                        module = selectedModule,
                        onQuizComplete = { score ->
                            haptics.celebrateModule()
                            mainViewModel.recordLessonCompletion(
                                lessonId = selectedModule.id,
                                score = score.toLong(),
                                masteredPhrases = selectedModule.phrases.size.toLong()
                            )
                        }
                    )
                    2 -> WritingPracticeCanvasSection(module = selectedModule)
                    else -> ConversationCoachSection(
                        module = selectedModule,
                        conversationState = conversationState,
                        onSpeakJapanese = onSpeakJapanese,
                        onSubmitResponse = { spoken, viaMic ->
                            aiViewModel.evaluateConversation(
                                scenarioTitle = selectedModule.conversationScenario,
                                targetPhraseHint = selectedModule.conversationPrompt,
                                userSpokenJapanese = spoken,
                                viaVoiceRecognition = viaMic
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhrasesPracticeSection(
    module: LearningModule,
    onSpeakJapanese: (String) -> Unit,
    onMarkMastered: () -> Unit
) {
    val haptics = rememberTabiHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        module.phrases.forEach { phrase ->
            var flipped by remember(phrase.id) { mutableStateOf(false) }
            val rotationY by animateFloatAsState(
                targetValue = if (flipped) 180f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                label = "flashcard_flip_${phrase.id}"
            )
            val showBack = rotationY > 90f

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.rotationY = rotationY
                        cameraDistance = 12f * density
                    },
                elevated = flipped,
                onClick = {
                    haptics.toggle()
                    flipped = !flipped
                },
                contentPadding = 16.dp
            ) {
                if (!showBack) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = phrase.japanese,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    onSpeakJapanese(phrase.japanese)
                                },
                                modifier = Modifier.testTag("speak_phrase_${phrase.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen to pronunciation",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "${phrase.reading} (${phrase.romaji})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap card for 3D flip -> meaning & nuance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.graphicsLayer { this.rotationY = 180f },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = phrase.english,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Nuance: ${phrase.nuanceNote}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Text("Tap to flip back", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        Button(
            onClick = onMarkMastered,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("complete_module_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mark Module Mastered & Sync Progress")
        }
    }
}

@Composable
private fun QuizSection(
    module: LearningModule,
    onQuizComplete: (Int) -> Unit
) {
    val haptics = rememberTabiHaptics()
    var questionIndex by remember(module.id) { mutableIntStateOf(0) }
    var selectedOption by remember(module.id, questionIndex) { mutableStateOf<Int?>(null) }
    var correctCount by remember(module.id) { mutableIntStateOf(0) }
    var finished by remember(module.id) { mutableStateOf(false) }

    val questions = module.quiz
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Interactive Comprehension Check", style = MaterialTheme.typography.titleMedium)
            }
            if (finished) {
                val score = ((correctCount.toFloat() / questions.size.coerceAtLeast(1)) * 100).toInt()
                Text(
                    text = "Quiz Complete! Score: $score% ($correctCount/${questions.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Button(
                    onClick = { onQuizComplete(score) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Score to Cloud Profile")
                }
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        questionIndex = 0
                        correctCount = 0
                        selectedOption = null
                        finished = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry Quiz")
                }
            } else {
                val q = questions[questionIndex]
                Text("Question ${questionIndex + 1} of ${questions.size}", style = MaterialTheme.typography.labelSmall)
                Text(q.prompt, style = MaterialTheme.typography.titleMedium)

                q.options.forEachIndexed { idx, opt ->
                    val isChosen = selectedOption == idx
                    val isCorrect = idx == q.correctIndex
                    val bgColor = when {
                        selectedOption == null -> MaterialTheme.colorScheme.surfaceVariant
                        isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                        isChosen -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = selectedOption == null) {
                                selectedOption = idx
                                if (idx == q.correctIndex) {
                                    correctCount++
                                    haptics.confirm()
                                } else {
                                    haptics.error()
                                }
                            }
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (selectedOption != null) {
                    Text(
                        text = q.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(
                        onClick = {
                            if (questionIndex < questions.lastIndex) {
                                haptics.tap()
                                questionIndex++
                                selectedOption = null
                            } else {
                                haptics.celebrateModule()
                                finished = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (questionIndex < questions.lastIndex) "Next Question" else "Finish Quiz")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WritingPracticeCanvasSection(module: LearningModule) {
    val haptics = rememberTabiHaptics()
    val chars = remember(module.id) {
        module.phrases.mapNotNull { it.writingTargetChar.takeIf { c -> c.isNotBlank() } }.distinct()
            .ifEmpty { listOf("あ", "か", "和") }
    }
    var targetChar by remember(module.id) { mutableStateOf(chars.first()) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var feedbackText by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Brush, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Kana & Kanji Writing Pad", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chars.forEach { ch ->
                    FilterChip(
                        selected = targetChar == ch,
                        onClick = {
                            haptics.tap()
                            targetChar = ch
                            strokes.clear()
                            feedbackText = null
                        },
                        label = { Text(ch, fontSize = 18.sp) }
                    )
                }
            }
            val inkColor = MaterialTheme.colorScheme.primary
            val guideColor = MaterialTheme.colorScheme.outline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, guideColor, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = targetChar,
                    fontSize = 150.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(targetChar) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke = currentStroke + change.position
                                },
                                onDragEnd = {
                                    if (currentStroke.size > 1) {
                                        strokes.add(currentStroke)
                                    }
                                    currentStroke = emptyList()
                                }
                            )
                        }
                ) {
                    val midX = size.width / 2f
                    val midY = size.height / 2f
                    drawLine(guideColor.copy(alpha = 0.4f), Offset(midX, 0f), Offset(midX, size.height), strokeWidth = 2f)
                    drawLine(guideColor.copy(alpha = 0.4f), Offset(0f, midY), Offset(size.width, midY), strokeWidth = 2f)

                    val allStrokes = strokes + listOf(currentStroke)
                    allStrokes.forEach { pts ->
                        if (pts.size > 1) {
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = inkColor,
                                style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        strokes.clear()
                        currentStroke = emptyList()
                        feedbackText = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Pad")
                }
                Button(
                    onClick = {
                        if (strokes.isEmpty()) {
                            haptics.error()
                            feedbackText = "Draw at least one stroke inside the square first."
                        } else {
                            haptics.celebrateModule()
                            feedbackText = "Nice balance! Drawn ${strokes.size} stroke(s) for '$targetChar'. Remember top-to-bottom, left-to-right stroke order."
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Check Strokes", fontWeight = FontWeight.Bold)
                }
            }
            if (feedbackText != null) {
                Text(
                    text = feedbackText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun ConversationCoachSection(
    module: LearningModule,
    conversationState: AiUiState<com.example.data.remote.ConversationFeedbackResult>,
    onSpeakJapanese: (String) -> Unit,
    onSubmitResponse: (String, Boolean) -> Unit
) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    var userInput by remember(module.id) { mutableStateOf("") }
    var micNotice by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                userInput = recognized
                onSubmitResponse(recognized, true)
            }
        }
    }

    val launchJapaneseSpeech = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Japanese for pronunciation feedback...")
        }
        runCatching { speechLauncher.launch(intent) }.onFailure {
            micNotice = "Voice recognizer unavailable here. Type your Japanese response below to practice!"
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchJapaneseSpeech()
        else micNotice = "Microphone permission denied."
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Real-Time Conversation & Pronunciation Coach", style = MaterialTheme.typography.titleMedium)
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("SCENARIO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(module.conversationScenario, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Try saying: ${module.conversationPrompt}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            haptics.tap()
                            onSpeakJapanese(module.conversationPrompt)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear model phrase")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    haptics.tap()
                    micNotice = null
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) launchJapaneseSpeech()
                    else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("speak_japanese_mic_button")
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Speak Japanese (Voice Recognition)", fontWeight = FontWeight.Bold)
            }

            if (micNotice != null) {
                Text(micNotice.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Or type your Japanese reply") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = {
                    haptics.confirm()
                    onSubmitResponse(userInput, false)
                },
                enabled = userInput.isNotBlank() && conversationState !is AiUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get AI Pronunciation & Grammar Feedback")
            }

            when (conversationState) {
                is AiUiState.Idle -> Unit
                is AiUiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Evaluating pitch accent, grammar & naturalness...")
                    }
                }
                is AiUiState.Error -> {
                    Text(conversationState.message, color = MaterialTheme.colorScheme.error)
                }
                is AiUiState.Success -> {
                    val fb = conversationState.data
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Accuracy Score: ${fb.grammarScore}/100", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                                IconButton(onClick = {
                                    haptics.tap()
                                    onSpeakJapanese(fb.japaneseReply)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear reply")
                                }
                            }
                            Text(fb.japaneseReply, style = MaterialTheme.typography.titleMedium)
                            Text("${fb.romajiReply} — \"${fb.englishMeaning}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider()
                            Text("Pronunciation & Feedback", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(fb.pronunciationFeedback, style = MaterialTheme.typography.bodyMedium)
                            Text("Naturalness Tip: ${fb.naturalnessTip}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
