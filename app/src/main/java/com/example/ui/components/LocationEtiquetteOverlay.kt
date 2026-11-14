package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.LocationEtiquetteTip
import com.example.ui.theme.LocalTabiThemeTokens
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Floating Liquid Glass overlay that triggers smoothly when the user enters a new
 * city or district in their itinerary, surfacing concise cultural etiquette tips
 * and an instant spoken local phrase.
 */
@Composable
fun LocationEtiquetteOverlay(
    tip: LocationEtiquetteTip?,
    onDismiss: () -> Unit,
    onSpeakPhrase: (String) -> Unit,
    onOpenFullGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTabiThemeTokens.current
    val haptics = rememberTabiHaptics()
    var isExpanded by remember(tip?.zoneKey) { mutableStateOf(false) }
    var dragOffsetY by remember(tip?.zoneKey) { mutableFloatStateOf(0f) }
    var autoProgress by remember(tip?.zoneKey) { mutableFloatStateOf(1f) }

    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "etiquette_drag_offset"
    )

    // Gentle auto-dismiss countdown (14s) paused whenever the user expands the phrase drawer
    LaunchedEffect(tip?.zoneKey, tip?.sourceLabel, isExpanded) {
        if (tip != null && !isExpanded) {
            val steps = 70
            for (step in steps downTo 0) {
                autoProgress = step / steps.toFloat()
                delay(200L)
            }
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = tip != null,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { -it - 40 } + fadeIn(),
            exit = slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) { -it - 40 } + fadeOut()
        ) {
            if (tip != null) {
                val shape = RoundedCornerShape(24.dp)
                val fillBrush = Brush.linearGradient(
                    colors = listOf(
                        tokens.glassSurfaceElevated.copy(alpha = 0.96f),
                        tokens.glassSurface.copy(alpha = 0.92f)
                    )
                )
                val rimBrush = Brush.linearGradient(
                    colors = listOf(
                        tokens.glassBorderHighlight,
                        tokens.auroraPrimary.copy(alpha = 0.45f),
                        tokens.glassBorderShadow
                    )
                )

                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 580.dp)
                            .fillMaxWidth()
                            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                            .pointerInput(tip.zoneKey) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragOffsetY < -65f) {
                                            haptics.tap()
                                            onDismiss()
                                        } else {
                                            dragOffsetY = 0f
                                        }
                                    },
                                    onDragCancel = { dragOffsetY = 0f },
                                    onVerticalDrag = { _, dragAmount ->
                                        dragOffsetY = (dragOffsetY + dragAmount).coerceAtMost(24f)
                                    }
                                )
                            }
                            .shadow(
                                elevation = 16.dp,
                                shape = shape,
                                ambientColor = tokens.auroraPrimary.copy(alpha = 0.25f),
                                spotColor = tokens.auroraSecondary.copy(alpha = 0.3f)
                            )
                            .clip(shape)
                            .background(fillBrush)
                            .border(width = 1.25.dp, brush = rimBrush, shape = shape)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("location_etiquette_overlay")
                    ) {
                        // Eyebrow arrival row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = tip.sourceLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
                                ) {
                                    Text(
                                        text = tip.categoryBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dismiss_etiquette_overlay_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss etiquette overlay",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                    // District / City Header with Japanese Hanko stamp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "礼儀",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${tip.districtName} • ${tip.japaneseName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${tip.cityName} Cultural Etiquette",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brief, scannable 2-bullet cultural etiquette tips
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        tip.briefTips.take(2).forEach { bullet ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Expandable local polite phrase & action row
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, tokens.glassBorderHighlight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "LOCAL POLITE PHRASE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = tip.localPhraseJa,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tip.localPhraseRomaji} — \"${tip.localPhraseEn}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            haptics.tap()
                                            onSpeakPhrase(tip.localPhraseJa)
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("speak_etiquette_phrase_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak local phrase",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptics.toggle()
                                    isExpanded = !isExpanded
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .testTag("expand_etiquette_phrase_button")
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isExpanded) "Hide Phrase" else "Local Phrase",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    haptics.tap()
                                    onOpenFullGuide()
                                },
                                modifier = Modifier.testTag("open_full_etiquette_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Full Guide", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!isExpanded) {
                        LinearProgressIndicator(
                            progress = { autoProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            trackColor = Color.Transparent
                        )
                    }
                }
                }
            }
        }
    }
}
