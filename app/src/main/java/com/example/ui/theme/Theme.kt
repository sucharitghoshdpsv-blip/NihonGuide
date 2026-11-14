package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode

@Immutable
data class TabiThemeTokens(
    val isDark: Boolean = false,
    val auroraPrimary: Color = ToriiCrimson,
    val auroraSecondary: Color = AiIndigo,
    val auroraAccent: Color = AmberKintsugi,
    val glassSurface: Color = Color.White.copy(alpha = 0.62f),
    val glassSurfaceElevated: Color = Color.White.copy(alpha = 0.78f),
    val glassBorderHighlight: Color = Color.White.copy(alpha = 0.85f),
    val glassBorderShadow: Color = WashiOutline.copy(alpha = 0.45f),
    val patternTint: Color = ToriiCrimson.copy(alpha = 0.06f)
)

val LocalTabiThemeTokens = staticCompositionLocalOf { TabiThemeTokens() }

private val LightColorScheme = lightColorScheme(
    primary = ToriiCrimson,
    onPrimary = Color.White,
    primaryContainer = ToriiLightContainer,
    onPrimaryContainer = OnToriiLightContainer,
    secondary = AiIndigo,
    onSecondary = Color.White,
    secondaryContainer = AiIndigoContainer,
    onSecondaryContainer = OnAiIndigoContainer,
    tertiary = MatchaMoss,
    onTertiary = Color.White,
    tertiaryContainer = MatchaContainer,
    onTertiaryContainer = OnMatchaContainer,
    background = WashiPaper,
    onBackground = SumiText,
    surface = WashiSurface,
    onSurface = SumiText,
    surfaceVariant = WashiContainer,
    onSurfaceVariant = SumiMuted,
    surfaceContainerLow = WashiContainer,
    surfaceContainer = WashiContainer,
    surfaceContainerHigh = WashiContainerHigh,
    outline = WashiOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = KintsugiGoldBright,
    onPrimary = Color(0xFF2B1E00),
    primaryContainer = LanternCoralContainer,
    onPrimaryContainer = GoldOnContainer,
    secondary = NightIndigo,
    onSecondary = Color(0xFF002F65),
    secondaryContainer = NightIndigoContainer,
    onSecondaryContainer = GoldOnContainer,
    tertiary = BambooJade,
    onTertiary = Color(0xFF00391D),
    tertiaryContainer = BambooJadeContainer,
    onTertiaryContainer = GoldOnContainer,
    background = SumiInk,
    onBackground = KintsugiGoldBright,
    surface = SumiSurface,
    onSurface = KintsugiGoldBright,
    surfaceVariant = SumiContainer,
    onSurfaceVariant = KintsugiGoldSoft,
    surfaceContainerLow = SumiContainer,
    surfaceContainer = SumiContainer,
    surfaceContainerHigh = SumiContainerHigh,
    outline = SumiOutline
)

private val SakuraColorScheme = lightColorScheme(
    primary = SakuraPetal,
    onPrimary = Color.White,
    primaryContainer = SakuraPetalContainer,
    onPrimaryContainer = OnSakuraPetalContainer,
    secondary = SakuraPlum,
    onSecondary = Color.White,
    secondaryContainer = SakuraPlumContainer,
    onSecondaryContainer = OnSakuraPlumContainer,
    tertiary = SakuraLeaf,
    onTertiary = Color.White,
    tertiaryContainer = SakuraLeafContainer,
    onTertiaryContainer = OnSakuraLeafContainer,
    background = SakuraBg,
    onBackground = SakuraText,
    surface = SakuraSurface,
    onSurface = SakuraText,
    surfaceVariant = SakuraContainer,
    onSurfaceVariant = SakuraMuted,
    surfaceContainerLow = SakuraContainer,
    surfaceContainer = SakuraContainer,
    surfaceContainerHigh = SakuraContainerHigh,
    outline = SakuraOutline
)

private val MatchaColorScheme = lightColorScheme(
    primary = UjiMatcha,
    onPrimary = Color.White,
    primaryContainer = UjiMatchaContainer,
    onPrimaryContainer = OnUjiMatchaContainer,
    secondary = CedarBark,
    onSecondary = Color.White,
    secondaryContainer = CedarContainer,
    onSecondaryContainer = OnCedarContainer,
    tertiary = HinokiGold,
    onTertiary = Color.White,
    tertiaryContainer = HinokiContainer,
    onTertiaryContainer = OnHinokiContainer,
    background = TatamiBg,
    onBackground = TatamiText,
    surface = TatamiSurface,
    onSurface = TatamiText,
    surfaceVariant = TatamiContainer,
    onSurfaceVariant = TatamiMuted,
    surfaceContainerLow = TatamiContainer,
    surfaceContainer = TatamiContainer,
    surfaceContainerHigh = TatamiContainerHigh,
    outline = TatamiOutline
)

private val CyberTokyoColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF002730),
    primaryContainer = NeonCyanContainer,
    onPrimaryContainer = OnNeonCyanContainer,
    secondary = NeonMagenta,
    onSecondary = Color(0xFF3E0027),
    secondaryContainer = NeonMagentaContainer,
    onSecondaryContainer = OnNeonMagentaContainer,
    tertiary = NeonAmber,
    onTertiary = Color(0xFF352000),
    tertiaryContainer = NeonAmberContainer,
    onTertiaryContainer = OnNeonAmberContainer,
    background = ShinjukuNight,
    onBackground = ShinjukuText,
    surface = ShinjukuSurface,
    onSurface = ShinjukuText,
    surfaceVariant = ShinjukuContainer,
    onSurfaceVariant = ShinjukuMuted,
    surfaceContainerLow = ShinjukuContainer,
    surfaceContainer = ShinjukuContainer,
    surfaceContainerHigh = ShinjukuContainerHigh,
    outline = ShinjukuOutline
)

val TabiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    val animSpec = tween<Color>(durationMillis = 420)
    return target.copy(
        primary = animateColorAsState(target.primary, animSpec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, animSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, animSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, animSpec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, animSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(target.tertiary, animSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, animSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, animSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, animSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(target.background, animSpec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, animSpec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, animSpec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, animSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
        outline = animateColorAsState(target.outline, animSpec, label = "outline").value
    )
}

@Composable
fun TabiGuideTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveMode = when (themeMode) {
        ThemeMode.SYSTEM -> if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
        else -> themeMode
    }
    val isDark = effectiveMode == ThemeMode.DARK || effectiveMode == ThemeMode.CYBER_TOKYO

    val rawColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode == ThemeMode.SYSTEM -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveMode == ThemeMode.DARK -> DarkColorScheme
        effectiveMode == ThemeMode.SAKURA -> SakuraColorScheme
        effectiveMode == ThemeMode.MATCHA -> MatchaColorScheme
        effectiveMode == ThemeMode.CYBER_TOKYO -> CyberTokyoColorScheme
        else -> LightColorScheme
    }

    val rawTokens = when (effectiveMode) {
        ThemeMode.DARK -> TabiThemeTokens(
            isDark = true,
            auroraPrimary = AmberKintsugi,
            auroraSecondary = LanternCoral,
            auroraAccent = KintsugiGoldBright,
            glassSurface = Color(0xFF1B202C).copy(alpha = 0.72f),
            glassSurfaceElevated = Color(0xFF232938).copy(alpha = 0.86f),
            glassBorderHighlight = KintsugiGoldBright.copy(alpha = 0.36f),
            glassBorderShadow = AmberKintsugi.copy(alpha = 0.20f),
            patternTint = KintsugiGoldBright.copy(alpha = 0.08f)
        )
        ThemeMode.SAKURA -> TabiThemeTokens(
            isDark = false,
            auroraPrimary = Color(0xFFFF8DAA),
            auroraSecondary = Color(0xFFB388EB),
            auroraAccent = Color(0xFFFFC2D4),
            glassSurface = Color.White.copy(alpha = 0.64f),
            glassSurfaceElevated = Color.White.copy(alpha = 0.80f),
            glassBorderHighlight = Color.White.copy(alpha = 0.92f),
            glassBorderShadow = SakuraPetal.copy(alpha = 0.22f),
            patternTint = SakuraPetal.copy(alpha = 0.07f)
        )
        ThemeMode.MATCHA -> TabiThemeTokens(
            isDark = false,
            auroraPrimary = Color(0xFF56A86E),
            auroraSecondary = Color(0xFFD4A359),
            auroraAccent = Color(0xFF9AD9AE),
            glassSurface = Color(0xFFFAFDF6).copy(alpha = 0.66f),
            glassSurfaceElevated = Color(0xFFFFFFFF).copy(alpha = 0.82f),
            glassBorderHighlight = Color.White.copy(alpha = 0.90f),
            glassBorderShadow = UjiMatcha.copy(alpha = 0.20f),
            patternTint = UjiMatcha.copy(alpha = 0.06f)
        )
        ThemeMode.CYBER_TOKYO -> TabiThemeTokens(
            isDark = true,
            auroraPrimary = NeonCyan,
            auroraSecondary = NeonMagenta,
            auroraAccent = NeonAmber,
            glassSurface = Color(0xFF12182B).copy(alpha = 0.62f),
            glassSurfaceElevated = Color(0xFF1B2440).copy(alpha = 0.78f),
            glassBorderHighlight = NeonCyan.copy(alpha = 0.42f),
            glassBorderShadow = NeonMagenta.copy(alpha = 0.28f),
            patternTint = NeonCyan.copy(alpha = 0.08f)
        )
        else -> TabiThemeTokens(
            isDark = false,
            auroraPrimary = ToriiCrimson,
            auroraSecondary = AiIndigo,
            auroraAccent = AmberKintsugi,
            glassSurface = Color(0xFFFFFDF9).copy(alpha = 0.65f),
            glassSurfaceElevated = Color.White.copy(alpha = 0.82f),
            glassBorderHighlight = Color.White.copy(alpha = 0.92f),
            glassBorderShadow = ToriiCrimson.copy(alpha = 0.16f),
            patternTint = ToriiCrimson.copy(alpha = 0.055f)
        )
    }

    val animatedScheme = animateScheme(rawColorScheme)
    val animSpec = tween<Color>(durationMillis = 420)
    val animatedTokens = rawTokens.copy(
        auroraPrimary = animateColorAsState(rawTokens.auroraPrimary, animSpec, label = "ap").value,
        auroraSecondary = animateColorAsState(rawTokens.auroraSecondary, animSpec, label = "as").value,
        auroraAccent = animateColorAsState(rawTokens.auroraAccent, animSpec, label = "aa").value,
        glassSurface = animateColorAsState(rawTokens.glassSurface, animSpec, label = "gs").value,
        glassSurfaceElevated = animateColorAsState(rawTokens.glassSurfaceElevated, animSpec, label = "gse").value,
        glassBorderHighlight = animateColorAsState(rawTokens.glassBorderHighlight, animSpec, label = "gbh").value,
        glassBorderShadow = animateColorAsState(rawTokens.glassBorderShadow, animSpec, label = "gbs").value,
        patternTint = animateColorAsState(rawTokens.patternTint, animSpec, label = "pt").value
    )

    CompositionLocalProvider(
        LocalTabiThemeTokens provides animatedTokens,
        LocalContentColor provides animatedScheme.onBackground
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = TabiTypography,
            shapes = TabiShapes,
            content = content
        )
    }
}
