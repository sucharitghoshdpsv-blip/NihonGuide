package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode
import com.example.ui.theme.LocalTabiThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient Japanese Liquid Aurora & Seigaiha (青海波) wave watermark background.
 * Drives the refractive look behind Liquid Glass cards across all 6 Japanese themes.
 */
@Composable
fun LiquidAuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalTabiThemeTokens.current
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "aurora_drift")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left luminous orb
            val c1 = Offset(
                x = w * (0.22f + 0.10f * cos(phase)),
                y = h * (0.16f + 0.06f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tokens.auroraPrimary.copy(alpha = 0.24f), Color.Transparent),
                    center = c1,
                    radius = w * 0.65f * pulse
                ),
                radius = w * 0.65f * pulse,
                center = c1
            )

            // Mid-right luminous orb
            val c2 = Offset(
                x = w * (0.82f + 0.08f * sin(phase * 0.8f)),
                y = h * (0.44f + 0.08f * cos(phase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tokens.auroraSecondary.copy(alpha = 0.22f), Color.Transparent),
                    center = c2,
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = c2
            )

            // Bottom-left golden kintsugi / sakura accent orb
            val c3 = Offset(
                x = w * (0.30f + 0.12f * sin(phase + 1.4f)),
                y = h * (0.84f + 0.05f * cos(phase + 1.4f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tokens.auroraAccent.copy(alpha = 0.18f), Color.Transparent),
                    center = c3,
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = c3
            )

            // Traditional Japanese Seigaiha (青海波) concentric wave crest lattice
            val waveR = 46.dp.toPx()
            val rows = 4
            val cols = (w / (waveR * 1.8f)).toInt() + 2
            val startY = h - waveR * 2.2f
            for (r in 0..rows) {
                val offsetX = if (r % 2 == 0) 0f else waveR * 0.9f
                val cy = startY + r * waveR * 0.55f
                for (c in -1..cols) {
                    val cx = c * waveR * 1.8f + offsetX
                    for (ring in 1..3) {
                        drawCircle(
                            color = tokens.patternTint,
                            radius = waveR * (ring / 3f),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Translucent Liquid Glass container with specular diagonal edge refraction,
 * subtle inner sheen, and optional spring press feedback.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalTabiThemeTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glass_press_scale"
    )

    val fillBrush = Brush.linearGradient(
        colors = listOf(
            if (elevated) tokens.glassSurfaceElevated else tokens.glassSurface,
            tokens.glassSurface.copy(alpha = (tokens.glassSurface.alpha * 0.84f))
        )
    )
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            tokens.glassBorderHighlight,
            tokens.glassBorderShadow,
            tokens.glassBorderHighlight.copy(alpha = 0.45f)
        )
    )

    val haptics = rememberTabiHaptics()
    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            onClick = {
                haptics.tap()
                onClick()
            }
        )
    } else Modifier

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (elevated) 10.dp else 4.dp,
                    shape = shape,
                    ambientColor = tokens.auroraPrimary.copy(alpha = 0.16f),
                    spotColor = tokens.auroraSecondary.copy(alpha = 0.22f)
                )
                .clip(shape)
                .background(fillBrush)
                .border(width = 1.1.dp, brush = rimBrush, shape = shape)
                .then(clickMod)
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalTabiThemeTokens.current
    val shape = RoundedCornerShape(50)
    val bg = tint ?: tokens.glassSurfaceElevated
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(bg)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(tokens.glassBorderHighlight, tokens.glassBorderShadow)
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun JapaneseThemeSelectorStrip(
    selectedTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val swatches = remember {
        mapOf(
            ThemeMode.SYSTEM to listOf(Color(0xFF64748B), Color(0xFF94A3B8)),
            ThemeMode.LIGHT to listOf(Color(0xFFB91C1C), Color(0xFFFAF6F0)),
            ThemeMode.DARK to listOf(Color(0xFFFF897D), Color(0xFF101318)),
            ThemeMode.SAKURA to listOf(Color(0xFFC94A6A), Color(0xFFFFD9E2)),
            ThemeMode.MATCHA to listOf(Color(0xFF2E6B40), Color(0xFFF3F6EE)),
            ThemeMode.CYBER_TOKYO to listOf(Color(0xFF4CE5FF), Color(0xFFFF6EC7))
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(ThemeMode.entries, key = { it.name }) { mode ->
            val selected = selectedTheme == mode
            val colors = swatches[mode] ?: listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface)
            val tokens = LocalTabiThemeTokens.current
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.04f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "theme_chip_scale"
            )
            val haptics = rememberTabiHaptics()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f) else tokens.glassSurface,
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else tokens.glassBorderHighlight
                ),
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable {
                        haptics.tap()
                        onSelectTheme(mode)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(colors))
                            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    )
                    Column {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mode.japaneseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
