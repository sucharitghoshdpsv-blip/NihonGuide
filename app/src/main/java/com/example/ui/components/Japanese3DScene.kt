package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTabiThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

enum class Japanese3DLandmark(val title: String, val japanese: String, val subtitle: String) {
    TORII_SHRINE("Fushimi Torii", "伏見稲荷鳥居", "10-part sculpted Torii gate & swaying lanterns"),
    KYOTO_PAGODA("Gojunoto & Fuji", "五重塔と富士山", "5-tier wooden pagoda, golden sorin & Mt. Fuji"),
    SHINKANSEN("N700S Shinkansen", "新幹線高架橋", "Streamlined bullet train gliding on elevated viaduct")
}

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class Face3D(
    val p0: Vec3,
    val p1: Vec3,
    val p2: Vec3,
    val p3: Vec3?,
    val baseColor: Color,
    val metallic: Float = 0.08f
)

private data class ProjectedFace(
    val pts: List<Offset>,
    val avgDepth: Float,
    val shadedColor: Color
)

private data class SakuraPetal3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val speed: Float,
    val phase: Float
)

/**
 * Interactive 60fps 3D perspective engine rendered natively in Jetpack Compose Canvas.
 * Supports damped touch-drag orbit, pinch zoom, painter's-algorithm depth sorting,
 * directional key + hemisphere fill lighting, contact shadows, and 3D sakura particles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveJapanese3DShowcase(
    modifier: Modifier = Modifier
) {
    val haptics = rememberTabiHaptics()
    var selectedLandmark by remember { mutableStateOf(Japanese3DLandmark.TORII_SHRINE) }
    var targetYaw by remember { mutableFloatStateOf(0.45f) }
    var targetPitch by remember { mutableFloatStateOf(0.26f) }
    var targetZoom by remember { mutableFloatStateOf(1.05f) }

    var yaw by remember { mutableFloatStateOf(0.45f) }
    var pitch by remember { mutableFloatStateOf(0.26f) }
    var zoom by remember { mutableFloatStateOf(1.05f) }
    var animTime by remember { mutableFloatStateOf(0f) }

    val tokens = LocalTabiThemeTokens.current

    val petals = remember {
        List(26) { i ->
            SakuraPetal3D(
                x = ((i * 37 % 100) / 50f - 1f) * 2.4f,
                y = ((i * 53 % 100) / 50f - 1f) * 1.4f,
                z = ((i * 71 % 100) / 50f - 1f) * 2.2f,
                speed = 0.35f + (i % 5) * 0.12f,
                phase = i * 0.6f
            )
        }
    }

    // Damped 60fps animation loop (1 - exp(-k * dt))
    LaunchedEffect(Unit) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = now
                animTime += dt
                val factor = 1f - exp(-9f * dt)
                yaw += (targetYaw + sin(animTime * 0.45f) * 0.12f - yaw) * factor
                pitch += (targetPitch - pitch) * factor
                zoom += (targetZoom - zoom) * factor
            }
        }
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("3D Japan Landmark Diorama", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${selectedLandmark.japanese} • Drag to orbit, pinch to zoom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = {
                    haptics.tap()
                    targetYaw = 0.45f
                    targetPitch = 0.26f
                    targetZoom = 1.05f
                },
                modifier = Modifier.testTag("reset_3d_camera_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset 3D camera")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val skyTop = if (tokens.isDark) Color(0xFF0A0E1B) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        val skyBottom = if (tokens.isDark) tokens.auroraPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(skyTop, skyBottom)))
                .border(1.dp, tokens.glassBorderHighlight, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        targetYaw += pan.x * 0.0085f
                        targetPitch = (targetPitch + pan.y * 0.0065f).coerceIn(-0.45f, 0.95f)
                        targetZoom = (targetZoom * gestureZoom).coerceIn(0.65f, 1.85f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h * 0.56f
                val scale = minOf(w, h) * 0.36f * zoom
                val fov = 5.4f

                val faces = buildLandmarkFaces(selectedLandmark, animTime, tokens.isDark)
                val cosY = cos(yaw)
                val sinY = sin(yaw)
                val cosP = cos(pitch)
                val sinP = sin(pitch)

                fun rotate(v: Vec3): Vec3 {
                    val x1 = v.x * cosY - v.z * sinY
                    val z1 = v.x * sinY + v.z * cosY
                    val y2 = v.y * cosP - z1 * sinP
                    val z2 = v.y * sinP + z1 * cosP
                    return Vec3(x1, y2, z2)
                }

                fun project(v: Vec3): Offset {
                    val d = (fov + v.z).coerceAtLeast(0.8f)
                    val p = fov / d
                    return Offset(cx + v.x * scale * p, cy - v.y * scale * p)
                }

                // Soft radial ground contact shadow
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(cx, cy + scale * 0.56f),
                        radius = scale * 1.4f
                    ),
                    topLeft = Offset(cx - scale * 1.3f, cy + scale * 0.25f),
                    size = androidx.compose.ui.geometry.Size(scale * 2.6f, scale * 0.72f)
                )

                val lightDir = Vec3(0.45f, 0.75f, -0.48f)

                val projected = faces.mapNotNull { face ->
                    val r0 = rotate(face.p0)
                    val r1 = rotate(face.p1)
                    val r2 = rotate(face.p2)
                    val r3 = face.p3?.let { rotate(it) }

                    val avgZ = if (r3 != null) (r0.z + r1.z + r2.z + r3.z) / 4f else (r0.z + r1.z + r2.z) / 3f

                    // Surface normal for directional + hemisphere lighting
                    val ux = r1.x - r0.x
                    val uy = r1.y - r0.y
                    val uz = r1.z - r0.z
                    val vx = r2.x - r0.x
                    val vy = r2.y - r0.y
                    val vz = r2.z - r0.z
                    val nx = uy * vz - uz * vy
                    val ny = uz * vx - ux * vz
                    val nz = ux * vy - uy * vx
                    val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
                    val dot = ((nx / len) * lightDir.x + (ny / len) * lightDir.y + (nz / len) * lightDir.z)
                    val brightness = (0.58f + kotlin.math.abs(dot) * (0.42f + face.metallic * 0.22f)).coerceIn(0.42f, 1.15f)

                    val shaded = Color(
                        red = (face.baseColor.red * brightness).coerceIn(0f, 1f),
                        green = (face.baseColor.green * brightness).coerceIn(0f, 1f),
                        blue = (face.baseColor.blue * brightness).coerceIn(0f, 1f),
                        alpha = face.baseColor.alpha
                    )

                    val pts = buildList {
                        add(project(r0))
                        add(project(r1))
                        add(project(r2))
                        if (r3 != null) add(project(r3))
                    }
                    ProjectedFace(pts = pts, avgDepth = avgZ, shadedColor = shaded)
                }.sortedByDescending { it.avgDepth }

                projected.forEach { pf ->
                    val path = Path().apply {
                        moveTo(pf.pts[0].x, pf.pts[0].y)
                        for (i in 1 until pf.pts.size) lineTo(pf.pts[i].x, pf.pts[i].y)
                        close()
                    }
                    drawPath(path = path, color = pf.shadedColor)
                }

                // Drifting 3D sakura petals / embers
                petals.forEach { petal ->
                    val py = 1.4f - ((animTime * petal.speed + petal.phase) % 2.6f)
                    val px = petal.x + sin(animTime * 1.4f + petal.phase) * 0.18f
                    val pz = petal.z + cos(animTime * 1.1f + petal.phase) * 0.14f
                    val rot = rotate(Vec3(px, py, pz))
                    val screen = project(rot)
                    val radius = (5.5f / (fov + rot.z).coerceAtLeast(1f)) * 4.5f
                    drawCircle(
                        color = if (selectedLandmark == Japanese3DLandmark.SHINKANSEN && tokens.isDark)
                            Color(0xFF4CE5FF).copy(alpha = 0.72f)
                        else Color(0xFFFFB7C5).copy(alpha = 0.85f),
                        radius = radius.coerceIn(2f, 8f),
                        center = screen
                    )
                }
            }

            // Floating Liquid Glass caption inside 3D viewport
            Surface(
                color = tokens.glassSurfaceElevated,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = selectedLandmark.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Japanese3DLandmark.entries.forEach { landmark ->
                FilterChip(
                    selected = selectedLandmark == landmark,
                    onClick = {
                        haptics.tap()
                        selectedLandmark = landmark
                    },
                    label = { Text(landmark.title) }
                )
            }
        }
    }
}

private fun addBoxFaces(
    out: MutableList<Face3D>,
    cx: Float,
    cy: Float,
    cz: Float,
    w: Float,
    h: Float,
    d: Float,
    color: Color,
    metallic: Float = 0.08f
) {
    val hw = w / 2f
    val hh = h / 2f
    val hd = d / 2f
    val v000 = Vec3(cx - hw, cy - hh, cz - hd)
    val v100 = Vec3(cx + hw, cy - hh, cz - hd)
    val v110 = Vec3(cx + hw, cy + hh, cz - hd)
    val v010 = Vec3(cx - hw, cy + hh, cz - hd)
    val v001 = Vec3(cx - hw, cy - hh, cz + hd)
    val v101 = Vec3(cx + hw, cy - hh, cz + hd)
    val v111 = Vec3(cx + hw, cy + hh, cz + hd)
    val v011 = Vec3(cx - hw, cy + hh, cz + hd)

    out.add(Face3D(v000, v100, v110, v010, color, metallic)) // front
    out.add(Face3D(v101, v001, v011, v111, color, metallic)) // back
    out.add(Face3D(v001, v000, v010, v011, color, metallic)) // left
    out.add(Face3D(v100, v101, v111, v110, color, metallic)) // right
    out.add(Face3D(v010, v110, v111, v011, color, metallic)) // top
    out.add(Face3D(v001, v101, v100, v000, color, metallic)) // bottom
}

private fun addPyramidFaces(
    out: MutableList<Face3D>,
    cx: Float,
    baseY: Float,
    cz: Float,
    baseW: Float,
    baseD: Float,
    height: Float,
    color: Color
) {
    val hw = baseW / 2f
    val hd = baseD / 2f
    val apex = Vec3(cx, baseY + height, cz)
    val b0 = Vec3(cx - hw, baseY, cz - hd)
    val b1 = Vec3(cx + hw, baseY, cz - hd)
    val b2 = Vec3(cx + hw, baseY, cz + hd)
    val b3 = Vec3(cx - hw, baseY, cz + hd)

    out.add(Face3D(b0, b1, apex, null, color))
    out.add(Face3D(b1, b2, apex, null, color))
    out.add(Face3D(b2, b3, apex, null, color))
    out.add(Face3D(b3, b0, apex, null, color))
}

private fun buildLandmarkFaces(
    landmark: Japanese3DLandmark,
    animTime: Float,
    isDark: Boolean
): List<Face3D> {
    val faces = mutableListOf<Face3D>()
    when (landmark) {
        Japanese3DLandmark.TORII_SHRINE -> {
            val vermilion = Color(0xFFD62828)
            val stone = Color(0xFF475569)
            val darkWood = Color(0xFF1E293B)
            val gold = Color(0xFFF59E0B)
            val lanternGlow = Color(0xFFFFB703)

            // 1. Cobblestone shrine platform
            addBoxFaces(faces, 0f, -0.78f, 0f, 2.3f, 0.12f, 1.3f, Color(0xFF64748B))
            // 2 & 3. Stone pedestals (kamebara)
            addBoxFaces(faces, -0.68f, -0.64f, 0f, 0.28f, 0.20f, 0.28f, stone)
            addBoxFaces(faces, 0.68f, -0.64f, 0f, 0.28f, 0.20f, 0.28f, stone)
            // 4 & 5. Twin vermilion pillars (hashira)
            addBoxFaces(faces, -0.68f, 0.05f, 0f, 0.18f, 1.24f, 0.18f, vermilion, metallic = 0.2f)
            addBoxFaces(faces, 0.68f, 0.05f, 0f, 0.18f, 1.24f, 0.18f, vermilion, metallic = 0.2f)
            // 6. Lower tie-beam (nuki)
            addBoxFaces(faces, 0f, 0.34f, 0f, 1.88f, 0.11f, 0.13f, vermilion)
            // 7. Central strut & sacred tablet (gakuzuka)
            addBoxFaces(faces, 0f, 0.54f, 0f, 0.22f, 0.26f, 0.14f, gold, metallic = 0.45f)
            // 8. Secondary lintel (shimaki)
            addBoxFaces(faces, 0f, 0.68f, 0f, 2.16f, 0.11f, 0.22f, vermilion)
            // 9. Curved dark-slate cap (kasagi) + upturned wing tips
            addBoxFaces(faces, 0f, 0.79f, 0f, 2.34f, 0.10f, 0.28f, darkWood, metallic = 0.3f)
            addBoxFaces(faces, -1.16f, 0.85f, 0f, 0.26f, 0.09f, 0.28f, darkWood)
            addBoxFaces(faces, 1.16f, 0.85f, 0f, 0.26f, 0.09f, 0.28f, darkWood)
            // 10 & 11. Swaying chochin paper lanterns
            val sway = sin(animTime * 2.2f) * 0.06f
            addBoxFaces(faces, -0.38f + sway, 0.14f, 0.14f, 0.16f, 0.24f, 0.16f, lanternGlow)
            addBoxFaces(faces, 0.38f - sway, 0.14f, 0.14f, 0.16f, 0.24f, 0.16f, lanternGlow)
        }
        Japanese3DLandmark.KYOTO_PAGODA -> {
            val timber = Color(0xFF9E2A2B)
            val roof = if (isDark) Color(0xFF2B3548) else Color(0xFF334155)
            val gold = Color(0xFFF59E0B)
            val fujiBlue = Color(0xFF3F5E8C)
            val snow = Color(0xFFF8FAFC)

            // Backdrop Mt. Fuji cone + snow cap
            addPyramidFaces(faces, -1.05f, -0.75f, 1.15f, 1.65f, 1.4f, 1.35f, fujiBlue)
            addPyramidFaces(faces, -1.05f, 0.15f, 1.15f, 0.62f, 0.54f, 0.45f, snow)

            // Stone pagoda plinth
            addBoxFaces(faces, 0.30f, -0.76f, -0.10f, 1.15f, 0.14f, 1.15f, Color(0xFF64748B))

            // 5 stacked diminishing pagoda tiers
            for (tier in 0 until 5) {
                val scale = 1f - tier * 0.12f
                val baseY = -0.60f + tier * 0.26f
                addBoxFaces(faces, 0.30f, baseY, -0.10f, 0.54f * scale, 0.18f, 0.54f * scale, timber)
                addPyramidFaces(faces, 0.30f, baseY + 0.06f, -0.10f, 0.98f * scale, 0.98f * scale, 0.17f, roof)
            }
            // Golden sorin spire
            addBoxFaces(faces, 0.30f, 0.72f, -0.10f, 0.06f, 0.34f, 0.06f, gold, metallic = 0.6f)
        }
        Japanese3DLandmark.SHINKANSEN -> {
            val concrete = Color(0xFF64748B)
            val trackBed = Color(0xFF334155)
            val trainWhite = Color(0xFFF8FAFC)
            val shinkansenBlue = Color(0xFF0055A4)
            val windowDark = Color(0xFF0F172A)

            // Viaduct deck & 4 support piers
            addBoxFaces(faces, 0f, -0.15f, 0f, 3.2f, 0.14f, 0.62f, trackBed)
            listOf(-1.1f, -0.35f, 0.4f, 1.15f).forEach { px ->
                addBoxFaces(faces, px, -0.52f, 0f, 0.20f, 0.62f, 0.32f, concrete)
            }

            // Animated bullet train gliding along X axis
            val trainX = ((animTime * 0.85f) % 3.6f) - 1.8f
            // Lead nose + 2 passenger cars + blue stripe + cabin glass
            addBoxFaces(faces, trainX + 0.48f, 0.04f, 0f, 0.38f, 0.16f, 0.26f, trainWhite, metallic = 0.35f)
            addBoxFaces(faces, trainX, 0.06f, 0f, 0.62f, 0.22f, 0.28f, trainWhite, metallic = 0.35f)
            addBoxFaces(faces, trainX - 0.68f, 0.06f, 0f, 0.62f, 0.22f, 0.28f, trainWhite, metallic = 0.35f)
            addBoxFaces(faces, trainX - 0.22f, 0.01f, 0f, 1.48f, 0.05f, 0.30f, shinkansenBlue)
            addBoxFaces(faces, trainX + 0.44f, 0.12f, 0f, 0.18f, 0.08f, 0.27f, windowDark, metallic = 0.5f)
        }
    }
    return faces
}
