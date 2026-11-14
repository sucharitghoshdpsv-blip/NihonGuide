package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GuideContent
import com.example.data.model.ItineraryItem
import com.example.data.model.MapPoi
import com.example.data.model.OfflineCityMapData
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.theme.LocalTabiThemeTokens
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.TabiAiViewModel
import com.example.ui.viewmodel.TabiMainViewModel
import kotlin.math.hypot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineMapScreen(
    mainViewModel: TabiMainViewModel,
    aiViewModel: TabiAiViewModel,
    isExpandedScreen: Boolean
) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val regions by mainViewModel.mapRegions.collectAsStateWithLifecycle()
    val mapsDiscoveryState by aiViewModel.mapsDiscoveryState.collectAsStateWithLifecycle()

    var selectedCityId by remember { mutableStateOf("tokyo") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var mapsQuery by remember { mutableStateOf("") }
    var selectedPoi by remember { mutableStateOf<MapPoi?>(null) }
    var is3DTiltEnabled by remember { mutableStateOf(true) }

    val cityData = remember(selectedCityId) {
        GuideContent.offlineCityMaps.firstOrNull { it.regionId == selectedCityId }
            ?: GuideContent.offlineCityMaps.first()
    }
    val regionStatus = regions.firstOrNull { it.regionId == cityData.regionId }
    val isDownloaded = regionStatus?.isDownloaded ?: (cityData.regionId == "tokyo")

    val filteredPois = remember(cityData, selectedCategory, searchQuery) {
        cityData.pois.filter { poi ->
            val catOk = selectedCategory == "All" || poi.category == selectedCategory
            val queryOk = searchQuery.isBlank() ||
                poi.name.contains(searchQuery, ignoreCase = true) ||
                poi.japaneseName.contains(searchQuery, ignoreCase = true) ||
                poi.description.contains(searchQuery, ignoreCase = true)
            catOk && queryOk
        }
    }

    val openInGoogleMaps: (String) -> Unit = { queryOrUri ->
        val uri = if (queryOrUri.startsWith("http") || queryOrUri.startsWith("geo:")) {
            Uri.parse(queryOrUri)
        } else {
            Uri.parse("geo:${cityData.latCenter},${cityData.lonCenter}?q=${Uri.encode(queryOrUri)}")
        }
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    val googleMapsDiscoveryBlock: @Composable () -> Unit = {
        GoogleMapsDiscoveryCard(
            cityData = cityData,
            query = mapsQuery,
            onQueryChange = { mapsQuery = it },
            state = mapsDiscoveryState,
            onSearchMaps = { q ->
                aiViewModel.discoverPlacesWithGoogleMaps(q, cityData.name, cityData.latCenter, cityData.lonCenter)
            },
            onOpenPlaceUri = openInGoogleMaps,
            onOpenCityInMaps = {
                openInGoogleMaps("geo:${cityData.latCenter},${cityData.lonCenter}?q=${Uri.encode(cityData.name + " Japan")}")
            }
        )
    }

    if (isExpandedScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CitySelectorHeader(
                    cityData = cityData,
                    isDownloaded = isDownloaded,
                    is3DTilt = is3DTiltEnabled,
                    onToggle3DTilt = {
                        haptics.toggle()
                        is3DTiltEnabled = !is3DTiltEnabled
                    },
                    onSelectCity = {
                        haptics.tap()
                        selectedCityId = it
                        selectedPoi = null
                        val nextCity = GuideContent.offlineCityMaps.firstOrNull { c -> c.regionId == it }
                        if (nextCity != null) {
                            mainViewModel.triggerLocationEtiquette(
                                city = nextCity.name,
                                sourceLabel = "Map City"
                            )
                        }
                    },
                    onToggleDownload = {
                        haptics.confirm()
                        mainViewModel.toggleOfflineMapRegion(cityData.regionId, isDownloaded)
                    }
                )
                InteractiveOfflineMapCanvas(
                    cityData = cityData,
                    pois = filteredPois,
                    selectedPoi = selectedPoi,
                    is3DTilt = is3DTiltEnabled,
                    onSelectPoi = { poi ->
                        selectedPoi = poi
                        mainViewModel.triggerLocationEtiquette(
                            city = cityData.name,
                            districtOrTitle = poi.name,
                            notes = poi.description,
                            sourceLabel = "Map District"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { googleMapsDiscoveryBlock() }
                item {
                    MapFiltersAndSearch(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )
                }
                if (selectedPoi != null) {
                    item {
                        SelectedPoiDetailCard(
                            poi = selectedPoi!!,
                            cityName = cityData.name,
                            onOpenInGoogleMaps = { openInGoogleMaps("${selectedPoi!!.name} ${cityData.name} Japan") },
                            onAddToItinerary = {
                                haptics.confirm()
                                mainViewModel.addItineraryItem(
                                    ItineraryItem(
                                        dayNumber = 1L,
                                        timeSlot = "11:00",
                                        title = selectedPoi!!.name,
                                        city = cityData.name,
                                        category = selectedPoi!!.category,
                                        notes = "${selectedPoi!!.transitNote} — ${selectedPoi!!.description}",
                                        estimatedCostJpy = 1000L
                                    )
                                )
                            }
                        )
                    }
                }
                items(filteredPois, key = { it.id }) { poi ->
                    PoiListCard(
                        poi = poi,
                        selected = selectedPoi?.id == poi.id,
                        onClick = {
                            selectedPoi = poi
                            mainViewModel.triggerLocationEtiquette(
                                city = cityData.name,
                                districtOrTitle = poi.name,
                                notes = poi.description,
                                sourceLabel = "Map District"
                            )
                        }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CitySelectorHeader(
                    cityData = cityData,
                    isDownloaded = isDownloaded,
                    is3DTilt = is3DTiltEnabled,
                    onToggle3DTilt = {
                        haptics.toggle()
                        is3DTiltEnabled = !is3DTiltEnabled
                    },
                    onSelectCity = {
                        haptics.tap()
                        selectedCityId = it
                        selectedPoi = null
                        val nextCity = GuideContent.offlineCityMaps.firstOrNull { c -> c.regionId == it }
                        if (nextCity != null) {
                            mainViewModel.triggerLocationEtiquette(
                                city = nextCity.name,
                                sourceLabel = "Map City"
                            )
                        }
                    },
                    onToggleDownload = {
                        haptics.confirm()
                        mainViewModel.toggleOfflineMapRegion(cityData.regionId, isDownloaded)
                    }
                )
            }
            item {
                InteractiveOfflineMapCanvas(
                    cityData = cityData,
                    pois = filteredPois,
                    selectedPoi = selectedPoi,
                    is3DTilt = is3DTiltEnabled,
                    onSelectPoi = { poi ->
                        selectedPoi = poi
                        mainViewModel.triggerLocationEtiquette(
                            city = cityData.name,
                            districtOrTitle = poi.name,
                            notes = poi.description,
                            sourceLabel = "Map District"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(315.dp)
                )
            }
            item { googleMapsDiscoveryBlock() }
            item {
                MapFiltersAndSearch(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it }
                )
            }
            if (selectedPoi != null) {
                item {
                    SelectedPoiDetailCard(
                        poi = selectedPoi!!,
                        cityName = cityData.name,
                        onOpenInGoogleMaps = { openInGoogleMaps("${selectedPoi!!.name} ${cityData.name} Japan") },
                        onAddToItinerary = {
                            haptics.confirm()
                            mainViewModel.addItineraryItem(
                                ItineraryItem(
                                    dayNumber = 1L,
                                    timeSlot = "11:00",
                                    title = selectedPoi!!.name,
                                    city = cityData.name,
                                    category = selectedPoi!!.category,
                                    notes = "${selectedPoi!!.transitNote} — ${selectedPoi!!.description}",
                                    estimatedCostJpy = 1000L
                                )
                            )
                        }
                    )
                }
            }
            items(filteredPois, key = { it.id }) { poi ->
                PoiListCard(
                    poi = poi,
                    selected = selectedPoi?.id == poi.id,
                    onClick = {
                        selectedPoi = poi
                        mainViewModel.triggerLocationEtiquette(
                            city = cityData.name,
                            districtOrTitle = poi.name,
                            notes = poi.description,
                            sourceLabel = "Map District"
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoogleMapsDiscoveryCard(
    cityData: OfflineCityMapData,
    query: String,
    onQueryChange: (String) -> Unit,
    state: AiUiState<com.example.data.remote.MapsDiscoveryResult>,
    onSearchMaps: (String) -> Unit,
    onOpenPlaceUri: (String) -> Unit,
    onOpenCityInMaps: () -> Unit
) {
    val haptics = rememberTabiHaptics()
    LiquidGlassCard(elevated = true, contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Live Google Maps Discovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Google Maps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = {
                haptics.tap()
                onOpenCityInMaps()
            }) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Maps App")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Live Google Maps search in ${cityData.name}") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("google_maps_grounding_input")
            )
            Button(
                onClick = {
                    haptics.tap()
                    onSearchMaps(query.ifBlank { "best ramen and historic shrines" })
                },
                enabled = state !is AiUiState.Loading,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("google_maps_grounding_button")
            ) {
                Text("Go", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("24h ramen near station", "English-friendly clinics", "Hidden shrines open now").forEach { preset ->
                FilterChip(
                    selected = query == preset,
                    onClick = {
                        haptics.tap()
                        onQueryChange(preset)
                        onSearchMaps(preset)
                    },
                    label = { Text(preset) }
                )
            }
        }

        when (state) {
            AiUiState.Idle -> Unit
            AiUiState.Loading -> {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Fetching live place data from Google Maps...")
                }
            }
            is AiUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
            }
            is AiUiState.Success -> {
                val res = state.data
                Spacer(modifier = Modifier.height(8.dp))
                Text(res.summary, style = MaterialTheme.typography.bodyMedium)
                if (res.places.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Google Maps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        res.places.forEach { place ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable(enabled = place.uri.isNotBlank()) {
                                    onOpenPlaceUri(place.uri)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(place.title, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CitySelectorHeader(
    cityData: OfflineCityMapData,
    isDownloaded: Boolean,
    is3DTilt: Boolean,
    onToggle3DTilt: () -> Unit,
    onSelectCity: (String) -> Unit,
    onToggleDownload: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("3D Offline Maps & Live Google Maps", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${cityData.name} (${cityData.japaneseName}) • ${cityData.sizeMb} MB pack",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onToggle3DTilt) {
                    Icon(Icons.Default.ViewInAr, contentDescription = "Toggle 3D tilt", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (is3DTilt) "3D" else "2D")
                }
                OutlinedButton(
                    onClick = onToggleDownload,
                    modifier = Modifier.testTag("toggle_offline_pack_button")
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (isDownloaded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isDownloaded) "Ready" else "Cache")
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GuideContent.offlineCityMaps.forEach { city ->
                FilterChip(
                    selected = city.regionId == cityData.regionId,
                    onClick = { onSelectCity(city.regionId) },
                    label = { Text("${city.name} ${city.japaneseName}") }
                )
            }
        }
    }
}

@Composable
private fun InteractiveOfflineMapCanvas(
    cityData: OfflineCityMapData,
    pois: List<MapPoi>,
    selectedPoi: MapPoi?,
    is3DTilt: Boolean,
    onSelectPoi: (MapPoi) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember(cityData.regionId) { mutableFloatStateOf(1f) }
    var pan by remember(cityData.regionId) { mutableStateOf(Offset.Zero) }

    val tiltX by animateFloatAsState(
        targetValue = if (is3DTilt) 26f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "map_3d_tilt"
    )

    val trainTransition = rememberInfiniteTransition(label = "train_anim")
    val trainProgress by trainTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Restart),
        label = "train_progress"
    )

    val tokens = LocalTabiThemeTokens.current
    val bgColor = tokens.glassSurface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(1.1.dp, tokens.glassBorderHighlight, RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = tiltX
                    cameraDistance = 16f * density
                }
                .pointerInput(cityData.regionId) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.85f, 3.0f)
                        pan = Offset(
                            (pan.x + panChange.x).coerceIn(-size.width * 0.6f, size.width * 0.6f),
                            (pan.y + panChange.y).coerceIn(-size.height * 0.6f, size.height * 0.6f)
                        )
                    }
                }
                .pointerInput(pois, zoom, pan) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val cx = w / 2f
                        val cy = h / 2f
                        val nearest = pois.minByOrNull { poi ->
                            val px = cx + (poi.normX * w - cx) * zoom + pan.x
                            val py = cy + (poi.normY * h - cy) * zoom + pan.y
                            hypot(tapOffset.x - px, tapOffset.y - py)
                        }
                        if (nearest != null) {
                            val px = cx + (nearest.normX * w - cx) * zoom + pan.x
                            val py = cy + (nearest.normY * h - cy) * zoom + pan.y
                            if (hypot(tapOffset.x - px, tapOffset.y - py) <= 68f) {
                                onSelectPoi(nearest)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            fun transform(nx: Float, ny: Float): Offset =
                Offset(cx + (nx * w - cx) * zoom + pan.x, cy + (ny * h - cy) * zoom + pan.y)

            for (i in 1..7) {
                val step = i / 8f
                drawLine(gridColor, transform(0f, step), transform(1f, step), strokeWidth = 1.5f)
                drawLine(gridColor, transform(step, 0f), transform(step, 1f), strokeWidth = 1.5f)
            }

            // Transit lines + animated moving trains
            cityData.transitLines.forEachIndexed { lineIdx, line ->
                if (line.points.size >= 2) {
                    val path = Path().apply {
                        val first = transform(line.points.first().first, line.points.first().second)
                        moveTo(first.x, first.y)
                        line.points.drop(1).forEach { (x, y) ->
                            val pt = transform(x, y)
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(line.colorHex),
                        style = Stroke(width = 8f * zoom.coerceAtMost(1.5f), cap = StrokeCap.Round)
                    )

                    // Interpolate train dot along segments
                    val segCount = line.points.size - 1
                    val tTotal = ((trainProgress + lineIdx * 0.37f) % 1f) * segCount
                    val segIdx = tTotal.toInt().coerceIn(0, segCount - 1)
                    val segFrac = tTotal - segIdx
                    val a = line.points[segIdx]
                    val b = line.points[segIdx + 1]
                    val trainPos = transform(
                        a.first + (b.first - a.first) * segFrac,
                        a.second + (b.second - a.second) * segFrac
                    )
                    drawCircle(color = Color.White, radius = 9f, center = trainPos)
                    drawCircle(color = Color(line.colorHex), radius = 6f, center = trainPos)
                }
            }

            // POI markers
            pois.forEach { poi ->
                val center = transform(poi.normX, poi.normY)
                val isSelected = selectedPoi?.id == poi.id
                val pinColor = when (poi.category) {
                    "Hospital", "Police" -> primaryColor
                    "Station" -> secondaryColor
                    else -> tertiaryColor
                }
                if (isSelected) {
                    drawCircle(
                        color = pinColor.copy(alpha = 0.25f),
                        radius = 30f,
                        center = center
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 16f else 12f,
                    center = center
                )
                drawCircle(
                    color = pinColor,
                    radius = if (isSelected) 12f else 9f,
                    center = center
                )
            }
        }

        Surface(
            color = tokens.glassSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = "${cityData.name} ${if (is3DTilt) "3D" else "2D"} • Live Rail • Tap Pin",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MapOverlayControlButton(Icons.Default.ZoomIn, "Zoom in") {
                zoom = (zoom + 0.35f).coerceAtMost(3.0f)
            }
            MapOverlayControlButton(Icons.Default.ZoomOut, "Zoom out") {
                zoom = (zoom - 0.35f).coerceAtLeast(0.85f)
            }
            MapOverlayControlButton(Icons.Default.MyLocation, "Reset map center") {
                zoom = 1f
                pan = Offset.Zero
            }
        }
    }
}

@Composable
private fun MapOverlayControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val haptics = rememberTabiHaptics()
    Surface(
        shape = CircleShape,
        color = LocalTabiThemeTokens.current.glassSurfaceElevated,
        shadowElevation = 2.dp
    ) {
        IconButton(onClick = { haptics.tap(); onClick() }, modifier = Modifier.size(40.dp)) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapFiltersAndSearch(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    val haptics = rememberTabiHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search offline stations, shrines, hospitals") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("map_search_input")
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Landmark", "Station", "Hospital", "Police", "Food").forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        haptics.tap()
                        onSelectCategory(cat)
                    },
                    label = { Text(cat) }
                )
            }
        }
    }
}

@Composable
private fun SelectedPoiDetailCard(
    poi: MapPoi,
    cityName: String,
    onOpenInGoogleMaps: () -> Unit,
    onAddToItinerary: () -> Unit
) {
    LiquidGlassCard(elevated = true, contentPadding = 16.dp) {
        Text(
            text = "${poi.category.uppercase()} • ${poi.japaneseName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(poi.name, style = MaterialTheme.typography.titleLarge)
        Text(poi.description, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Transit: ${poi.transitNote}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onOpenInGoogleMaps,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Google Maps")
            }
            Button(
                onClick = onAddToItinerary,
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("poi_add_to_itinerary_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add to $cityName")
            }
        }
    }
}

@Composable
private fun PoiListCard(
    poi: MapPoi,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (poi.category) {
        "Station" -> Icons.Default.Train
        "Hospital" -> Icons.Default.LocalHospital
        "Police" -> Icons.Default.LocalPolice
        "Food" -> Icons.Default.Restaurant
        else -> Icons.Default.LocationOn
    }
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = selected,
        onClick = onClick,
        contentPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("${poi.name} (${poi.japaneseName})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(poi.transitNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
