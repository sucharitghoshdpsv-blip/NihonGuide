package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BudgetExpense
import com.example.data.model.FlightAlert
import com.example.data.model.GuideContent
import com.example.data.model.ItineraryItem
import com.example.notifications.TravelNotificationHelper
import com.example.ui.components.BudgetCurrencyConverterCard
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.theme.LocalTabiThemeTokens
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.FxRateState
import com.example.ui.viewmodel.SyncState
import com.example.ui.viewmodel.TabiAiViewModel
import com.example.ui.viewmodel.TabiMainViewModel
import kotlin.math.roundToInt

@Composable
fun PlanBudgetScreen(
    mainViewModel: TabiMainViewModel,
    aiViewModel: TabiAiViewModel
) {
    val haptics = rememberTabiHaptics()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Itinerary & AI, 1: Budget, 2: Flights & Alerts

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LocalTabiThemeTokens.current.glassSurfaceElevated
        ) {
            Tab(selected = selectedTab == 0, onClick = { haptics.tap(); selectedTab = 0 }, text = { Text("Itinerary", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { haptics.tap(); selectedTab = 1 }, text = { Text("Budget", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { haptics.tap(); selectedTab = 2 }, text = { Text("Alerts", fontWeight = FontWeight.Bold) })
        }

        when (selectedTab) {
            0 -> ItineraryBuilderTab(mainViewModel = mainViewModel, aiViewModel = aiViewModel)
            1 -> BudgetTrackerTab(mainViewModel = mainViewModel)
            else -> FlightAlertsTab(mainViewModel = mainViewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItineraryBuilderTab(
    mainViewModel: TabiMainViewModel,
    aiViewModel: TabiAiViewModel
) {
    val haptics = rememberTabiHaptics()
    val itineraryState by mainViewModel.itineraryState.collectAsStateWithLifecycle()
    val offlineItinerary by mainViewModel.offlineItinerary.collectAsStateWithLifecycle()
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val planState by aiViewModel.planState.collectAsStateWithLifecycle()
    val fxState by mainViewModel.fxRateState.collectAsStateWithLifecycle()

    val selectedInterests = remember(userProfile) {
        mutableStateListOf<String>().apply {
            addAll(userProfile?.interests?.ifEmpty { listOf("Food & Ramen", "Temples & Shrines") }
                ?: listOf("Food & Ramen", "Temples & Shrines"))
        }
    }
    val visitedHistory = remember(userProfile) {
        mutableStateListOf<String>().apply {
            addAll(userProfile?.visitedRegions ?: emptyList())
        }
    }
    val context = LocalContext.current
    var tripDays by remember { mutableFloatStateOf(3f) }
    var showAddDialog by remember { mutableStateOf(false) }

    val detectNearbyDistrict: () -> Unit = {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = runCatching {
            lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()
        val matched = if (loc != null) {
            mainViewModel.triggerEtiquetteFromCoordinates(loc.latitude, loc.longitude, forceShow = true)
        } else {
            false
        }
        if (!matched) {
            val activeStop = when (val s = itineraryState) {
                is SyncState.Success -> s.data
                else -> emptyList()
            }.firstOrNull { !it.completed }
            if (activeStop != null) {
                mainViewModel.checkInItineraryStop(activeStop)
            } else {
                mainViewModel.showMessage("No active Japanese district detected near device coordinates yet.")
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            detectNearbyDistrict()
        } else {
            mainViewModel.showMessage("Location permission declined. Tap 'Arrive' on any stop for district etiquette.")
        }
    }

    val items = when (val s = itineraryState) {
        is SyncState.Success -> s.data
        else -> offlineItinerary.map {
            ItineraryItem(
                id = it.id,
                userId = it.userId,
                dayNumber = it.dayNumber,
                timeSlot = it.timeSlot,
                title = it.title,
                city = it.city,
                category = it.category,
                notes = it.notes,
                estimatedCostJpy = it.estimatedCostJpy,
                completed = it.completed
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("AI Personalized Planner & Recommendations", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Tailors destinations from your interests, trip length, and past Japan travel history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Trip Duration: ${tripDays.roundToInt()} Days", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = tripDays,
                        onValueChange = { tripDays = it },
                        valueRange = 1f..7f,
                        steps = 5
                    )

                    Text("Interests", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GuideContent.interestOptions.forEach { tag ->
                            val picked = tag in selectedInterests
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    haptics.tap()
                                    if (picked) selectedInterests.remove(tag) else selectedInterests.add(tag)
                                },
                                label = { Text(tag) }
                            )
                        }
                    }

                    Text("Past Travel History (for smarter next-stop picks)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GuideContent.japanRegions.forEach { region ->
                            val visited = region in visitedHistory
                            FilterChip(
                                selected = visited,
                                onClick = {
                                    haptics.tap()
                                    if (visited) visitedHistory.remove(region) else visitedHistory.add(region)
                                },
                                label = { Text(region) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            haptics.tap()
                            mainViewModel.updateTravelProfile(
                                homeCity = userProfile?.homeCity ?: "Tokyo",
                                dailyBudgetJpy = userProfile?.dailyBudgetJpy ?: 18000L,
                                interests = selectedInterests.toList(),
                                visitedRegions = visitedHistory.toList()
                            )
                            aiViewModel.generatePersonalizedPlan(
                                interests = selectedInterests.toList(),
                                tripDays = tripDays.roundToInt(),
                                visitedRegions = visitedHistory.toList(),
                                dailyBudgetJpy = userProfile?.dailyBudgetJpy ?: 18000L
                            )
                        },
                        enabled = planState !is AiUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_ai_itinerary_button")
                    ) {
                        Text("Generate Personalized Plan & Recommendations", fontWeight = FontWeight.Bold)
                    }

                    when (val state = planState) {
                        is AiUiState.Idle -> Unit
                        is AiUiState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text("Crafting personalized recommendations & schedule...")
                            }
                        }
                        is AiUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                        is AiUiState.Success -> {
                            val plan = state.data
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Recommended For You", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                plan.recommendations.forEach { rec ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("${rec.regionName} (${rec.japaneseName})", style = MaterialTheme.typography.titleMedium)
                                            Text(rec.matchReason, style = MaterialTheme.typography.bodyMedium)
                                            Text("Tip: ${rec.bestSeasonOrTip}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        haptics.confirm()
                                        mainViewModel.importGeneratedActivities(plan.activities)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Import ${plan.activities.size} Activities into Synced Itinerary")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Synced Day-by-Day Itinerary (${items.size})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_itinerary_stop_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Stop")
                    }
                }
                OutlinedButton(
                    onClick = {
                        haptics.confirm()
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) detectNearbyDistrict()
                        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detect_district_etiquette_button")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check Current District Etiquette (GPS / Active Stop)")
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        "No stops planned yet. Generate an AI itinerary above or add your own custom stop!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(items, key = { it.id }) { stop ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            haptics.toggle()
                            mainViewModel.toggleItineraryCompleted(stop)
                        }) {
                            Icon(
                                imageVector = if (stop.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle stop completed",
                                tint = if (stop.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DAY ${stop.dayNumber} • ${stop.timeSlot} • ${stop.city}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(stop.title, style = MaterialTheme.typography.titleMedium)
                            if (stop.notes.isNotBlank()) {
                                Text(stop.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val estConverted = if (stop.estimatedCostJpy > 0L) fxState.formatConvertedJpy(stop.estimatedCostJpy) else null
                            Text(
                                text = if (estConverted != null) "Est. ¥${stop.estimatedCostJpy} (≈ $estConverted)" else "Est. ¥${stop.estimatedCostJpy}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(
                            onClick = {
                                haptics.confirm()
                                mainViewModel.checkInItineraryStop(stop)
                            },
                            modifier = Modifier.testTag("arrive_itinerary_stop_${stop.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Arrive in ${stop.city} — view local etiquette",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            haptics.toggle()
                            mainViewModel.deleteItineraryItem(stop.id)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete stop")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItineraryStopDialog(
            onDismiss = {
                haptics.tap()
                showAddDialog = false
            },
            onSave = { item ->
                haptics.confirm()
                mainViewModel.addItineraryItem(item)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun BudgetTrackerTab(mainViewModel: TabiMainViewModel) {
    val haptics = rememberTabiHaptics()
    val expensesState by mainViewModel.expensesState.collectAsStateWithLifecycle()
    val offlineExpenses by mainViewModel.offlineExpenses.collectAsStateWithLifecycle()
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val fxState by mainViewModel.fxRateState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mainViewModel.refreshExchangeRates(force = false)
    }

    val expenses = when (val s = expensesState) {
        is SyncState.Success -> s.data
        else -> offlineExpenses.map {
            BudgetExpense(
                id = it.id,
                userId = it.userId,
                title = it.title,
                amountJpy = it.amountJpy,
                category = it.category,
                dayNumber = it.dayNumber,
                notes = it.notes
            )
        }
    }

    val dailyBudget = (userProfile?.dailyBudgetJpy ?: 18000L).coerceAtLeast(1000L)
    val totalSpent = expenses.sumOf { it.amountJpy }
    val convertedTotal = fxState.formatConvertedJpy(totalSpent)
    val convertedDaily = fxState.formatConvertedJpy(dailyBudget)
    var showExpenseDialog by remember { mutableStateOf(false) }
    var prefilledExpenseJpy by remember { mutableStateOf<String?>(null) }

    val byCategory = remember(expenses) {
        expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amountJpy } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TOTAL SPENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("¥$totalSpent", style = MaterialTheme.typography.displaySmall)
                            Text(
                                text = when {
                                    convertedTotal != null -> "≈ $convertedTotal (${fxState.homeCurrency})"
                                    fxState.isRefreshing -> "Fetching live ${fxState.homeCurrency} rate..."
                                    else -> "Tap Refresh below for live ${fxState.homeCurrency} rate"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("budget_total_converted_text")
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                haptics.tap()
                                prefilledExpenseJpy = null
                                showExpenseDialog = true
                            },
                            modifier = Modifier.testTag("log_expense_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log ¥", fontWeight = FontWeight.Bold)
                        }
                    }
                    val progress = (totalSpent.toFloat() / dailyBudget.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Text(
                        text = buildString {
                            append("Daily target: ¥$dailyBudget")
                            if (convertedDaily != null) append(" (≈ $convertedDaily)")
                            append(" • ${(progress * 100).toInt()}% used")
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        item(key = "budget_currency_converter") {
            BudgetCurrencyConverterCard(
                fxState = fxState,
                onSelectHomeCurrency = { code -> mainViewModel.setHomeCurrency(code) },
                onRefreshRates = { mainViewModel.refreshExchangeRates(force = true) },
                onLogConvertedAmount = { amountJpy ->
                    prefilledExpenseJpy = amountJpy.toString()
                    showExpenseDialog = true
                }
            )
        }

        if (byCategory.isNotEmpty()) {
            item {
                BudgetBreakdownChartCard(
                    byCategory = byCategory,
                    totalSpent = totalSpent,
                    fxState = fxState
                )
            }
        }

        items(expenses, key = { it.id }) { exp ->
            val convertedExpense = fxState.formatConvertedJpy(exp.amountJpy)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAY ${exp.dayNumber} • ${exp.category.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(exp.title, style = MaterialTheme.typography.titleMedium)
                        if (exp.notes.isNotBlank()) {
                            Text(exp.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "¥${exp.amountJpy}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (convertedExpense != null) {
                            Text(
                                text = "≈ $convertedExpense",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {
                        haptics.toggle()
                        mainViewModel.deleteExpense(exp.id)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete expense")
                    }
                }
            }
        }
    }

    if (showExpenseDialog) {
        AddExpenseDialog(
            fxState = fxState,
            initialAmountJpy = prefilledExpenseJpy ?: "",
            onDismiss = {
                haptics.tap()
                prefilledExpenseJpy = null
                showExpenseDialog = false
            },
            onSave = { title, amount, cat, day, notes ->
                haptics.confirm()
                mainViewModel.addExpense(title, amount, cat, day, notes)
                prefilledExpenseJpy = null
                showExpenseDialog = false
            }
        )
    }
}

@Composable
private fun BudgetBreakdownChartCard(
    byCategory: Map<String, Long>,
    totalSpent: Long,
    fxState: FxRateState
) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFF59E0B),
        Color(0xFF7B4B94)
    )
    val entries = byCategory.entries.toList()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    entries.forEachIndexed { idx, (_, amount) ->
                        val sweep = if (totalSpent > 0) (amount.toFloat() / totalSpent.toFloat()) * 360f else 0f
                        drawArc(
                            color = palette[idx % palette.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                Text("¥$totalSpent", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                entries.forEachIndexed { idx, (cat, amt) ->
                    val pct = if (totalSpent > 0) ((amt * 100) / totalSpent) else 0
                    val converted = fxState.formatConvertedJpy(amt)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = palette[idx % palette.size],
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Text(
                            text = if (converted != null) "$cat: ¥$amt (≈ $converted • $pct%)" else "$cat: ¥$amt ($pct%)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightAlertsTab(mainViewModel: TabiMainViewModel) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val alertsState by mainViewModel.alertsState.collectAsStateWithLifecycle()
    val alerts = when (val s = alertsState) {
        is SyncState.Success -> s.data
        else -> emptyList()
    }

    var flightCode by remember { mutableStateOf("JL005") }
    var title by remember { mutableStateOf("Haneda Terminal 3 Boarding") }
    var timeText by remember { mutableStateOf("Today 16:40 JST") }
    var gate by remember { mutableStateOf("Terminal 3 • Gate 112") }
    var delaySeconds by remember { mutableStateOf("10") }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val sec = delaySeconds.toLongOrNull()?.coerceAtLeast(3L) ?: 10L
            TravelNotificationHelper.scheduleAlert(
                context = context,
                title = "$flightCode: $title",
                body = "$timeText • $gate",
                delayMillis = sec * 1000L,
                requestCode = (System.currentTimeMillis() % 10000).toInt()
            )
            mainViewModel.showMessage("Notification scheduled in ${sec}s!")
        } else {
            mainViewModel.showMessage("Notification permission denied. Alert saved in-app.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Flight & Shinkansen Push Alerts", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = flightCode,
                        onValueChange = { flightCode = it },
                        label = { Text("Flight or Train Code (e.g. JL005 / Nozomi 23)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Reminder Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text("Departure Time") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gate,
                            onValueChange = { gate = it },
                            label = { Text("Gate / Platform") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = delaySeconds,
                        onValueChange = { delaySeconds = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Push Reminder Delay (seconds for demo/test)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            haptics.confirm()
                            val sec = delaySeconds.toLongOrNull()?.coerceAtLeast(3L) ?: 10L
                            val triggerAt = System.currentTimeMillis() + sec * 1000L
                            mainViewModel.addFlightAlert(flightCode, title, timeText, gate, triggerAt)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                TravelNotificationHelper.scheduleAlert(
                                    context = context,
                                    title = "$flightCode: $title",
                                    body = "$timeText • $gate",
                                    delayMillis = sec * 1000L,
                                    requestCode = (System.currentTimeMillis() % 10000).toInt()
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("schedule_flight_alert_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Schedule Push Alert & Sync", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(alerts, key = { it.id }) { alert ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${alert.flightNumber} • ${alert.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(alert.title, style = MaterialTheme.typography.titleMedium)
                        Text("${alert.departureTimeText} • ${alert.gateOrTerminal}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        haptics.toggle()
                        mainViewModel.deleteFlightAlert(alert.id)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete alert")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddItineraryStopDialog(
    onDismiss: () -> Unit,
    onSave: (ItineraryItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Tokyo") }
    var dayStr by remember { mutableStateOf("1") }
    var timeSlot by remember { mutableStateOf("10:00") }
    var category by remember { mutableStateOf("Culture") }
    var costStr by remember { mutableStateOf("1000") }
    var notes by remember { mutableStateOf("") }

    val haptics = rememberTabiHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Itinerary Stop", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Activity Title") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dayStr, onValueChange = { dayStr = it.filter { c -> c.isDigit() } }, label = { Text("Day") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = timeSlot, onValueChange = { timeSlot = it }, label = { Text("Time") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                OutlinedTextField(value = costStr, onValueChange = { costStr = it.filter { c -> c.isDigit() } }, label = { Text("Est. Cost (JPY)") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        haptics.confirm()
                        onSave(
                            ItineraryItem(
                                dayNumber = (dayStr.toLongOrNull() ?: 1L).coerceIn(1L, 30L),
                                timeSlot = timeSlot.ifBlank { "10:00" },
                                title = title,
                                city = city.ifBlank { "Tokyo" },
                                category = category.ifBlank { "Culture" },
                                notes = notes,
                                estimatedCostJpy = costStr.toLongOrNull() ?: 0L
                            )
                        )
                    } else {
                        haptics.error()
                    }
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { haptics.tap(); onDismiss() }) { Text("Cancel", fontWeight = FontWeight.Bold) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExpenseDialog(
    fxState: FxRateState,
    initialAmountJpy: String = "",
    onDismiss: () -> Unit,
    onSave: (String, Long, String, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember(initialAmountJpy) { mutableStateOf(initialAmountJpy) }
    var category by remember { mutableStateOf("Food") }
    var dayStr by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    val parsedAmount = amountStr.toLongOrNull() ?: 0L
    val liveEstimate = if (parsedAmount > 0L) fxState.formatConvertedJpy(parsedAmount) else null

    val haptics = rememberTabiHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Expense (JPY)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Description (e.g. Ramen, Suica)") })
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (¥ JPY)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (liveEstimate != null) {
                    Text(
                        text = "≈ $liveEstimate (${fxState.homeCurrency})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Food", "Transit", "Lodging", "Sightseeing", "Shopping").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                haptics.tap()
                                category = cat
                            },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = dayStr,
                    onValueChange = { dayStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Trip Day") }
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toLongOrNull() ?: 0L
                    if (title.isNotBlank() && amt > 0L) {
                        haptics.confirm()
                        onSave(title, amt, category, (dayStr.toLongOrNull() ?: 1L).coerceIn(1L, 30L), notes)
                    } else {
                        haptics.error()
                    }
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { haptics.tap(); onDismiss() }) { Text("Cancel", fontWeight = FontWeight.Bold) } }
    )
}
