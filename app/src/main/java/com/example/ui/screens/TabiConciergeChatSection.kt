package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GuideContent
import com.example.data.remote.ChatModelTier
import com.example.data.remote.GroundingMode
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.theme.LocalTabiThemeTokens
import com.example.ui.viewmodel.ConciergeMessage
import com.example.ui.viewmodel.TabiAiViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabiConciergeChatSection(
    aiViewModel: TabiAiViewModel,
    homeCity: String
) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val messages by aiViewModel.chatMessages.collectAsStateWithLifecycle()
    val modelTier by aiViewModel.selectedModelTier.collectAsStateWithLifecycle()
    val groundingMode by aiViewModel.selectedGroundingMode.collectAsStateWithLifecycle()
    val isSending by aiViewModel.isChatSending.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf("") }
    val cityData = remember(homeCity) {
        GuideContent.offlineCityMaps.firstOrNull { it.name.equals(homeCity, ignoreCase = true) }
            ?: GuideContent.offlineCityMaps.first()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LiquidGlassCard(elevated = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("TabiSensei Multi-Turn Concierge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Model: ${modelTier.label} • Hub: ${cityData.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = {
                    haptics.toggle()
                    aiViewModel.clearChatHistory()
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Reset chat")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Reasoning Tier", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChatModelTier.entries.forEach { tier ->
                    FilterChip(
                        selected = modelTier == tier,
                        onClick = {
                            haptics.tap()
                            aiViewModel.setChatModelTier(tier)
                        },
                        label = { Text("${tier.badge} ${tier.label}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Live Grounding Tool", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GroundingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = groundingMode == mode,
                        onClick = {
                            haptics.tap()
                            aiViewModel.setGroundingMode(mode)
                        },
                        leadingIcon = when (mode) {
                            GroundingMode.GOOGLE_SEARCH -> { { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) } }
                            GroundingMode.GOOGLE_MAPS -> { { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) } }
                            GroundingMode.NONE -> null
                        },
                        label = { Text(mode.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val quickPrompts = listOf(
                    "Best late-night ramen open now near ${cityData.name} Station?",
                    "Latest 2026 cherry blossom or seasonal events in ${cityData.name}?",
                    "Compare JR Pass vs individual Shinkansen tickets for a 7-day trip"
                )
                quickPrompts.forEach { q ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        modifier = Modifier.clickable(enabled = !isSending) {
                            haptics.tap()
                            aiViewModel.sendConciergeMessage(q, cityData.latCenter, cityData.lonCenter)
                        }
                    ) {
                        Text(
                            text = q,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Scrollable chat thread items rendered natively inside parent LazyColumn / Column
        messages.forEach { msg ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) { it / 4 }
            ) {
                ChatBubbleCard(
                    message = msg,
                    onOpenUri = { uri ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                        }
                    }
                )
            }
        }

        LiquidGlassCard(contentPadding = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Ask TabiSensei in English or 日本語...") },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("concierge_chat_input")
                )
                IconButton(
                    onClick = {
                        haptics.confirm()
                        val toSend = draft
                        draft = ""
                        aiViewModel.sendConciergeMessage(toSend, cityData.latCenter, cityData.lonCenter)
                    },
                    enabled = draft.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("concierge_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatBubbleCard(
    message: ConciergeMessage,
    onOpenUri: (String) -> Unit
) {
    val tokens = LocalTabiThemeTokens.current
    val bgColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    } else {
        tokens.glassSurfaceElevated
    }
    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 6.dp,
                bottomEnd = if (message.isUser) 6.dp else 20.dp
            ),
            border = BorderStroke(1.dp, tokens.glassBorderHighlight),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (message.isUser) "You" else "旅先生 TabiSensei",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (message.text.isBlank() && message.isStreaming) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Thinking & grounding...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                }

                if (message.sources.isNotEmpty()) {
                    val hasMaps = message.sources.any { it.isGoogleMaps }
                    if (hasMaps) {
                        Text(
                            text = "Google Maps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.sources.take(6).forEach { src ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier.clickable(enabled = src.uri.isNotBlank()) {
                                    onOpenUri(src.uri)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (src.isGoogleMaps) Icons.Default.LocationOn else Icons.Default.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = src.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (!message.searchEntryPointHtml.isNullOrBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                loadDataWithBaseURL(null, message.searchEntryPointHtml, "text/html", "utf-8", null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }
    }
}
