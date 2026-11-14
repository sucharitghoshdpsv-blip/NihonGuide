package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ThumbUp
import com.example.ui.components.JapaneseThemeSelectorStrip
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.theme.LocalTabiThemeTokens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.signOutUser
import com.example.data.local.ThemeMode
import com.example.data.model.GuideContent
import com.example.ui.viewmodel.SyncState
import com.example.ui.viewmodel.TabiMainViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.hypot

@Composable
fun HubSafetyVaultScreen(
    mainViewModel: TabiMainViewModel,
    currentUserId: String,
    initialSubTab: Int = 0
) {
    val haptics = rememberTabiHaptics()
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LocalTabiThemeTokens.current.glassSurfaceElevated
        ) {
            Tab(selected = selectedTab == 0, onClick = { haptics.tap(); selectedTab = 0 }, text = { Text("Vault", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { haptics.tap(); selectedTab = 1 }, text = { Text("SOS", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { haptics.tap(); selectedTab = 2 }, text = { Text("Connect", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 3, onClick = { haptics.tap(); selectedTab = 3 }, text = { Text("Settings", fontWeight = FontWeight.Bold) })
        }

        when (selectedTab) {
            0 -> VaultSecurityTab(mainViewModel = mainViewModel)
            1 -> EmergencySosTab(mainViewModel = mainViewModel)
            2 -> NearbyCommunityTab(mainViewModel = mainViewModel, currentUserId = currentUserId)
            else -> SettingsSyncTab(mainViewModel = mainViewModel)
        }
    }
}

@Composable
private fun EmergencySosTab(mainViewModel: TabiMainViewModel) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()

    var bloodType by remember(userProfile) { mutableStateOf(userProfile?.bloodType ?: "O+") }
    var allergies by remember(userProfile) { mutableStateOf(userProfile?.allergies ?: "None") }
    var medicalJa by remember(userProfile) {
        mutableStateOf(
            userProfile?.emergencyMedicalSummaryJa?.ifBlank {
                "私は旅行者です。アレルギー：$allergies。血液型：$bloodType。英語の通訳をお願いします。"
            } ?: "私は旅行者です。アレルギー：$allergies。血液型：$bloodType。英語の通訳をお願いします。"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Emergency Contacts & Medical Aid", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "One-tap calling & Japanese medical card available offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(GuideContent.emergencyContacts, key = { it.id }) { contact ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (contact.isUrgent) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    contentColor = if (contact.isUrgent) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = contact.japaneseTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (contact.isUrgent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${contact.title} (${contact.number})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(contact.description, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Languages: ${contact.availableLanguages}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Button(
                        onClick = {
                            haptics.confirm()
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(contact.number)}"))
                            runCatching { context.startActivity(dialIntent) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (contact.isUrgent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("dial_${contact.id}")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call ${contact.number}", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dial", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

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
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Japanese Emergency Medical Card", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Show this card to paramedics or hospital staff in Japan:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bloodType,
                            onValueChange = { bloodType = it },
                            label = { Text("Blood Type") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = allergies,
                            onValueChange = { allergies = it },
                            label = { Text("Allergies / Meds") },
                            modifier = Modifier.weight(2f)
                        )
                    }
                    OutlinedTextField(
                        value = medicalJa,
                        onValueChange = { medicalJa = it },
                        label = { Text("Japanese Card Text (医療機関提示用)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            haptics.confirm()
                            mainViewModel.saveMedicalEmergencyCard(bloodType, allergies, medicalJa)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Sync Medical Card", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NearbyCommunityTab(
    mainViewModel: TabiMainViewModel,
    currentUserId: String
) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val postsState by mainViewModel.communityPostsState.collectAsStateWithLifecycle()
    val posts = when (val s = postsState) {
        is SyncState.Success -> s.data
        else -> emptyList()
    }

    var selectedCityFilter by remember { mutableStateOf("All") }
    var showNewPostDialog by remember { mutableStateOf(false) }

    val detectNearestCity: () -> Unit = {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = runCatching {
            lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()
        if (loc != null) {
            val nearest = GuideContent.offlineCityMaps.minByOrNull {
                hypot(loc.latitude - it.latCenter, loc.longitude - it.lonCenter)
            }
            if (nearest != null) {
                selectedCityFilter = nearest.name
                mainViewModel.showMessage("Nearest Japan hub detected: ${nearest.name}")
            }
        } else {
            selectedCityFilter = "Tokyo"
            mainViewModel.showMessage("Location unavailable on emulator; filtering by Tokyo.")
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) detectNearestCity()
        else mainViewModel.showMessage("Location permission declined. Select a city chip manually.")
    }

    val filteredPosts = remember(posts, selectedCityFilter) {
        if (selectedCityFilter == "All") posts
        else posts.filter { it.city.contains(selectedCityFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nearby Travelers", style = MaterialTheme.typography.headlineSmall)
                    Text("Meetups, dining buddies & live transit tips", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        haptics.tap()
                        showNewPostDialog = true
                    },
                    modifier = Modifier.testTag("create_post_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    listOf("All", "Tokyo", "Kyoto", "Osaka").forEach { city ->
                        FilterChip(
                            selected = selectedCityFilter == city,
                            onClick = {
                                haptics.tap()
                                selectedCityFilter = city
                            },
                            label = { Text(city) }
                        )
                    }
                }
                IconButton(
                    onClick = {
                        haptics.tap()
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) detectNearestCity()
                        else locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Detect nearby city", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (filteredPosts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("No posts in $selectedCityFilter yet. Share a ramen meetup or tip!", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(filteredPosts, key = { it.id }) { post ->
                val isInterested = currentUserId in post.interestedUids
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${post.city} • ${post.topic}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (post.authorId == currentUserId) {
                                IconButton(onClick = {
                                    haptics.toggle()
                                    mainViewModel.deleteCommunityPost(post.id)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete my post")
                                }
                            }
                        }
                        Text(post.message, style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "By ${post.authorName}" + if (post.meetupTime.isNotBlank()) " • ${post.meetupTime}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = {
                                haptics.confirm()
                                mainViewModel.togglePostRsvp(post)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = if (isInterested) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isInterested) "Joined (${post.interestedUids.size})" else "Join (${post.interestedUids.size})")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewPostDialog) {
        CreateCommunityPostDialog(
            onDismiss = {
                haptics.tap()
                showNewPostDialog = false
            },
            onSave = { city, topic, msg, time ->
                haptics.confirm()
                mainViewModel.createCommunityPost(city, topic, msg, time)
                showNewPostDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSyncTab(mainViewModel: TabiMainViewModel) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val prefs by mainViewModel.appPreferences.collectAsStateWithLifecycle()
    val profile by mainViewModel.userProfile.collectAsStateWithLifecycle()

    var homeCity by remember(profile) { mutableStateOf(profile?.homeCity ?: "Tokyo") }
    var budgetStr by remember(profile) { mutableStateOf((profile?.dailyBudgetJpy ?: 18000L).toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LiquidGlassCard(elevated = true, contentPadding = 16.dp) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Japanese UI Themes & Appearance", style = MaterialTheme.typography.titleMedium)
                    }
                    JapaneseThemeSelectorStrip(
                        selectedTheme = prefs.themeMode,
                        onSelectTheme = { mainViewModel.setThemeMode(it) }
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Flight & Itinerary Push Notifications", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = prefs.notificationsEnabled,
                            onCheckedChange = {
                                haptics.toggle()
                                mainViewModel.setNotificationsEnabled(it)
                            }
                        )
                    }
                    val syncText = if (prefs.lastSyncEpochMillis > 0L) {
                        "Last cloud sync: ${DateFormat.getDateTimeInstance().format(Date(prefs.lastSyncEpochMillis))}"
                    } else {
                        "Realtime Cloud Firestore & Room offline cache active"
                    }
                    Text(syncText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

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
                    Text("Travel Preferences", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = homeCity,
                        onValueChange = { homeCity = it },
                        label = { Text("Current Hub City in Japan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Daily Budget Target (¥ JPY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            haptics.confirm()
                            mainViewModel.updateTravelProfile(
                                homeCity = homeCity.ifBlank { "Tokyo" },
                                dailyBudgetJpy = budgetStr.toLongOrNull() ?: 18000L,
                                interests = profile?.interests ?: listOf("Food & Ramen"),
                                visitedRegions = profile?.visitedRegions ?: emptyList()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Preferences")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCommunityPostDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var city by remember { mutableStateOf("Tokyo - Shibuya") }
    var topic by remember { mutableStateOf("Meetup") }
    var message by remember { mutableStateOf("") }
    var meetupTime by remember { mutableStateOf("Today 19:00") }

    val haptics = rememberTabiHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect with Nearby Travelers", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Tokyo - Shibuya", "Kyoto - Gion", "Osaka - Namba", "Shinjuku").forEach { c ->
                        FilterChip(selected = city == c, onClick = { haptics.tap(); city = c }, label = { Text(c) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Meetup", "Food Spot", "Transit Tip", "Hidden Gem").forEach { t ->
                        FilterChip(selected = topic == t, onClick = { haptics.tap(); topic = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, minLines = 2)
                OutlinedTextField(value = meetupTime, onValueChange = { meetupTime = it }, label = { Text("Time (optional)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        haptics.confirm()
                        onSave(city, topic, message, meetupTime)
                    } else {
                        haptics.error()
                    }
                }
            ) { Text("Post", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { haptics.tap(); onDismiss() }) { Text("Cancel", fontWeight = FontWeight.Bold) } }
    )
}
