package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.MotionVideoExporter
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.theme.LocalTabiThemeTokens
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.TabiAiViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MotionReelStudioSection(
    aiViewModel: TabiAiViewModel
) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalTabiThemeTokens.current

    val directionState by aiViewModel.reelDirectionState.collectAsStateWithLifecycle()
    val restyleState by aiViewModel.restyleBitmapState.collectAsStateWithLifecycle()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLandscape16x9 by remember { mutableStateOf(true) }
    var motionPreset by remember { mutableStateOf("Sakura Drift") }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    val activeBitmap = when (val r = restyleState) {
        is AiUiState.Success -> r.data
        else -> sourceBitmap
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val loaded = runCatching {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val maxSide = 1280f
                val ratio = minOf(1f, maxSide / maxOf(bmp.width, bmp.height))
                if (ratio < 1f) {
                    Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                } else bmp
            }.getOrNull()
            if (loaded != null) {
                sourceBitmap = loaded
                aiViewModel.resetRestyleState()
                exportMessage = null
                aiViewModel.directPhotoReel(loaded, motionPreset, if (isLandscape16x9) "16:9 Landscape" else "9:16 Portrait")
            }
        }
    }

    // Smooth 60fps preview animation clock
    var animTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(activeBitmap != null) {
        if (activeBitmap != null) {
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    last = now
                    animTime += dt
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LiquidGlassCard(elevated = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Photo-to-Video Japanese Motion Reel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Pick any travel photo, choose 16:9 or 9:16, direct with Gemini & export MP4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Video Aspect Ratio", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isLandscape16x9,
                    onClick = {
                        haptics.tap()
                        isLandscape16x9 = true
                    },
                    label = { Text("16:9 Landscape") }
                )
                FilterChip(
                    selected = !isLandscape16x9,
                    onClick = {
                        haptics.tap()
                        isLandscape16x9 = false
                    },
                    label = { Text("9:16 Portrait") }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("3D Camera Motion Style", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sakura Drift", "Shinkansen Dolly", "Kyoto Tilt").forEach { preset ->
                    FilterChip(
                        selected = motionPreset == preset,
                        onClick = {
                            haptics.tap()
                            motionPreset = preset
                        },
                        label = { Text(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    haptics.tap()
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pick_photo_for_video_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (sourceBitmap == null) "Select Photo to Animate" else "Choose Another Photo", fontWeight = FontWeight.Bold)
            }
        }

        if (activeBitmap != null) {
            val direction = (directionState as? AiUiState.Success)?.data
            val jaTitle = direction?.japaneseTitle ?: "日本の旅"
            val enSub = direction?.englishSubtitle ?: "Animated Japan Travel Memory"

            LiquidGlassCard(elevated = true, contentPadding = 14.dp) {
                val aspect = if (isLandscape16x9) 16f / 9f else 9f / 16f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(1.dp, tokens.glassBorderHighlight, RoundedCornerShape(18.dp))
                ) {
                    val scaleMotion = when (motionPreset) {
                        "Shinkansen Dolly" -> 1.06f + (sin(animTime * 0.8f) + 1f) * 0.08f
                        "Kyoto Tilt" -> 1.12f - sin(animTime * 0.6f) * 0.04f
                        else -> 1.08f + sin(animTime * 0.7f) * 0.05f
                    }
                    val rotY = sin(animTime * 0.7f) * 4.5f
                    val rotX = cos(animTime * 0.5f) * 2.5f
                    val transX = sin(animTime * 0.55f) * 18f

                    Image(
                        bitmap = activeBitmap.asImageBitmap(),
                        contentDescription = "Animated travel photo preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scaleMotion
                                scaleY = scaleMotion
                                rotationY = rotY
                                rotationX = rotX
                                translationX = transX
                                cameraDistance = 14f * density
                            }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                )
                            )
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 0 until 16) {
                            val px = ((i * 73f + animTime * 68f + sin(animTime + i) * 26f) % size.width)
                            val py = ((i * 59f + animTime * 82f) % size.height)
                            drawCircle(
                                color = Color(0xFFFFB7C5).copy(alpha = 0.85f),
                                radius = 4.dp.toPx() + (i % 3) * 1.5f,
                                center = Offset(px, py)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = jaTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = enSub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (directionState is AiUiState.Loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Gemini is composing Japanese title & caption...")
                    }
                } else if (direction != null) {
                    Text(
                        text = "\"${direction.haikuCaption}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            sourceBitmap?.let { bmp ->
                                aiViewModel.restylePhoto(bmp, "Ukiyo-e Sakura Twilight")
                            }
                        },
                        enabled = restyleState !is AiUiState.Loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (restyleState is AiUiState.Loading) "Styling..." else "Anime Restyle")
                    }

                    Button(
                        onClick = {
                            if (!isExporting) {
                                haptics.confirm()
                                isExporting = true
                                exportMessage = null
                                scope.launch {
                                    runCatching {
                                        val mp4 = MotionVideoExporter.exportPhotoMotionMp4(
                                            context = context,
                                            sourceBitmap = activeBitmap,
                                            isLandscape16x9 = isLandscape16x9,
                                            japaneseTitle = jaTitle,
                                            englishSubtitle = enSub,
                                            motionPreset = motionPreset
                                        )
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            mp4
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Animated Japan Reel"))
                                        "Exported MP4 (${mp4.length() / 1024} KB)!"
                                    }.onSuccess { msg ->
                                        exportMessage = msg
                                    }.onFailure { e ->
                                        exportMessage = "MP4 export error: ${e.localizedMessage}"
                                    }
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_motion_mp4_button")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rendering...")
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export MP4")
                        }
                    }
                }

                if (restyleState is AiUiState.Error) {
                    Text(
                        text = (restyleState as AiUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (exportMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = exportMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
