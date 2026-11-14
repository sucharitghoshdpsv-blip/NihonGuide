package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.ThemeMode
import com.example.data.model.CulturalInsight
import com.example.data.model.GuideContent
import com.example.ui.components.InteractiveJapanese3DShowcase
import com.example.ui.components.JapaneseThemeSelectorStrip
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.TabiAiViewModel

private enum class ExploreStudioMode(val label: String, val icon: ImageVector, val tag: String) {
    GUIDE_TRANSLATE("Guide & Translate", Icons.Default.Translate, "mode_translate"),
    LANDMARK_3D("3D Landmarks", Icons.Default.ViewInAr, "mode_3d"),
    SENSEI_CHAT("Sensei AI", Icons.Default.Forum, "mode_chat"),
    VOICE_TRANSCRIBE("Live & Audio", Icons.Default.GraphicEq, "mode_voice"),
    MOTION_REEL("Photo Reel", Icons.Default.Movie, "mode_reel")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreTranslatorScreen(
    aiViewModel: TabiAiViewModel,
    userName: String,
    homeCity: String,
    currentTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onSpeakText: (String, Boolean) -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToVault: () -> Unit,
    isExpandedScreen: Boolean
) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val translationState by aiViewModel.translationState.collectAsStateWithLifecycle()

    var studioMode by remember { mutableStateOf(ExploreStudioMode.GUIDE_TRANSLATE) }
    var inputText by remember { mutableStateOf("") }
    var jaToEn by remember { mutableStateOf(true) }
    var registerPref by remember { mutableStateOf("Polite Travel (Teineigo)") }
    var expandedInsightId by remember { mutableStateOf<String?>(GuideContent.culturalInsights.first().id) }
    var speechError by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                inputText = recognized
                aiViewModel.translate(recognized, jaToEn, registerPref)
            }
        }
    }

    val launchSpeech = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val lang = if (jaToEn) "ja-JP" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (jaToEn) "Speak in Japanese..." else "Speak in English...")
        }
        runCatching { speechLauncher.launch(intent) }.onFailure {
            speechError = "Speech recognition service unavailable on this device. Type text above."
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchSpeech()
        else speechError = "Microphone permission denied. Enable it in Settings to use voice input."
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner with Japanese Liquid Glass overlay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpandedScreen) 210.dp else 184.dp)
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_japan),
                    contentDescription = "Japanese lantern avenue",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Black.copy(alpha = 0.18f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassPill(
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                    ) {
                        Text(
                            text = "旅ガイド • ${homeCity.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "ようこそ, $userName",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Liquid Glass Japanese Guide • 3D Landmarks • Grounded AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // Japanese UI Theme Selector Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "和のテーマ • Japanese UI Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                JapaneseThemeSelectorStrip(
                    selectedTheme = currentTheme,
                    onSelectTheme = onSelectTheme
                )
            }
        }

        // Studio Mode Switcher
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ExploreStudioMode.entries, key = { it.name }) { mode ->
                    FilterChip(
                        selected = studioMode == mode,
                        onClick = {
                            haptics.tap()
                            studioMode = mode
                        },
                        leadingIcon = {
                            Icon(mode.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag(mode.tag)
                    )
                }
            }
        }

        // Animated Mode Content
        item {
            AnimatedContent(
                targetState = studioMode,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                },
                label = "explore_studio_transition"
            ) { mode ->
                when (mode) {
                    ExploreStudioMode.LANDMARK_3D -> {
                        InteractiveJapanese3DShowcase()
                    }
                    ExploreStudioMode.SENSEI_CHAT -> {
                        TabiConciergeChatSection(
                            aiViewModel = aiViewModel,
                            homeCity = homeCity
                        )
                    }
                    ExploreStudioMode.VOICE_TRANSCRIBE -> {
                        LiveVoiceAndTranscribeSection(
                            aiViewModel = aiViewModel,
                            onSpeakText = onSpeakText
                        )
                    }
                    ExploreStudioMode.MOTION_REEL -> {
                        MotionReelStudioSection(aiViewModel = aiViewModel)
                    }
                    ExploreStudioMode.GUIDE_TRANSLATE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Quick SOS & Vault chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AssistChip(
                                    onClick = {
                                        haptics.tap()
                                        onNavigateToEmergency()
                                    },
                                    label = { Text("SOS 110 / 119") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_sos_chip")
                                )
                                AssistChip(
                                    onClick = {
                                        haptics.tap()
                                        onNavigateToVault()
                                    },
                                    label = { Text("E2EE Vault") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_vault_chip")
                                )
                            }

                            // Liquid Glass Real-Time Nuance Translator Card
                            LiquidGlassCard(elevated = true) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Translate,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Nuance AI Translator",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .clickable {
                                                    haptics.toggle()
                                                    jaToEn = !jaToEn
                                                }
                                                .testTag("swap_language_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (jaToEn) "日本語 → EN" else "EN → 日本語",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.SwapHoriz,
                                                    contentDescription = "Swap translation direction",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Polite Travel (Teineigo)", "Keigo (Formal)", "Casual Friends").forEach { modeOpt ->
                                            FilterChip(
                                                selected = registerPref == modeOpt,
                                                onClick = {
                                                    haptics.tap()
                                                    registerPref = modeOpt
                                                },
                                                label = { Text(modeOpt) }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        label = {
                                            Text(
                                                if (jaToEn) "Enter or speak Japanese (e.g. せっかくですが、遠慮しておきます)"
                                                else "Enter English to translate with Japanese nuance"
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    haptics.tap()
                                                    speechError = null
                                                    val granted = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.RECORD_AUDIO
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (granted) launchSpeech()
                                                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                },
                                                modifier = Modifier.testTag("translator_mic_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Voice input",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        minLines = 2,
                                        maxLines = 4,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("translator_input_field")
                                    )

                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val samples = if (jaToEn) {
                                            listOf("ちょっと厳しいかも", "お手数ですが確認していただけますか", "お先に失礼します")
                                        } else {
                                            listOf("Could I leave my luggage after checkout?", "I have a peanut allergy.")
                                        }
                                        samples.forEach { sample ->
                                            AssistChip(
                                                onClick = {
                                                    haptics.tap()
                                                    inputText = sample
                                                    aiViewModel.translate(sample, jaToEn, registerPref)
                                                },
                                                label = { Text(sample, maxLines = 1) }
                                            )
                                        }
                                    }

                                    if (speechError != null) {
                                        Text(
                                            text = speechError.orEmpty(),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            haptics.confirm()
                                            aiViewModel.translate(inputText, jaToEn, registerPref)
                                        },
                                        enabled = inputText.isNotBlank() && translationState !is AiUiState.Loading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("translate_submit_button"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Analyze Nuance & Translate", fontWeight = FontWeight.Bold)
                                    }

                                    when (val state = translationState) {
                                        is AiUiState.Idle -> Unit
                                        is AiUiState.Loading -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Analyzing particles, register & cultural subtext...")
                                            }
                                        }
                                        is AiUiState.Error -> {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = state.message,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                        }
                                        is AiUiState.Success -> {
                                            val res = state.data
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                                shape = RoundedCornerShape(16.dp)
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
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text(
                                                                text = res.formalityLevel,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                haptics.tap()
                                                                onSpeakText(res.translatedText, !jaToEn)
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                                contentDescription = "Speak translation"
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = res.translatedText,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (res.reading.isNotBlank() || res.romaji.isNotBlank()) {
                                                        Text(
                                                            text = "${res.reading} • ${res.romaji}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    HorizontalDivider()
                                                    Text(
                                                        text = "Grammatical Nuances",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(text = res.grammaticalNuances, style = MaterialTheme.typography.bodyMedium)
                                                    Text(
                                                        text = "Cultural Subtext",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Text(text = res.culturalContext, style = MaterialTheme.typography.bodyMedium)
                                                    if (res.politeAlternative.isNotBlank()) {
                                                        Text(
                                                            text = "Polite: ${res.politeAlternative}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (res.casualAlternative.isNotBlank()) {
                                                        Text(
                                                            text = "Casual: ${res.casualAlternative}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (studioMode == ExploreStudioMode.GUIDE_TRANSLATE) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "日本の文化 • Cultural Insights & Etiquette",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cultural_kyoto),
                            contentDescription = "Kyoto temple garden",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Image(
                            painter = painterResource(id = R.drawable.img_culinary_tokyo),
                            contentDescription = "Tokyo ramen and izakaya alley",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                }
            }

            items(GuideContent.culturalInsights, key = { it.id }) { insight ->
                CulturalInsightCard(
                    insight = insight,
                    expanded = expandedInsightId == insight.id,
                    onToggle = {
                        expandedInsightId = if (expandedInsightId == insight.id) null else insight.id
                    }
                )
            }
        }
    }
}

@Composable
private fun CulturalInsightCard(
    insight: CulturalInsight,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${insight.category.uppercase()} • ${insight.japaneseTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = if (expanded) "Hide" else "Read",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider()
                Text(
                    text = "Do's",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                insight.doList.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    text = "Avoid",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                insight.dontList.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Insider Tip: ${insight.insiderTip}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
