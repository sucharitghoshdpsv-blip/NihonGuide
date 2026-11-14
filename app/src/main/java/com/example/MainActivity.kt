package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.data.local.AppPreferences
import com.example.data.local.TabiDatabase
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.TabiFirestoreRepository
import com.example.notifications.TravelNotificationHelper
import com.example.security.configureAppCheck
import com.example.ui.components.LiquidAuroraBackground
import com.example.ui.components.LocationEtiquetteOverlay
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreTranslatorScreen
import com.example.ui.screens.HubSafetyVaultScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.OfflineMapScreen
import com.example.ui.screens.PlanBudgetScreen
import com.example.ui.theme.LocalTabiThemeTokens
import com.example.ui.theme.TabiGuideTheme
import com.example.ui.viewmodel.TabiAiViewModel
import com.example.ui.viewmodel.TabiMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

private enum class TopDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    LEARN("Learn", Icons.Filled.School, Icons.Outlined.School, "nav_learn"),
    MAP("Map", Icons.Filled.Map, Icons.Outlined.Map, "nav_map"),
    PLAN("Plan", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_plan"),
    HUB("Hub", Icons.Filled.Shield, Icons.Outlined.Shield, "nav_hub")
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/ViewModels
        configureAppCheck(this, intent)
        TravelNotificationHelper.ensureChannel(this)
        tts = TextToSpeech(this, this)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { UserPreferencesRepository(context) }
            val appPrefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())

            TabiGuideTheme(themeMode = appPrefs.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    LiquidAuroraBackground {
                        TabiRootApp(
                            prefsRepo = prefsRepo,
                            currentTheme = appPrefs.themeMode,
                            onSpeak = { text, isJapanese -> speakText(text, isJapanese) }
                        )
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        ttsReady = (status == TextToSpeech.SUCCESS)
    }

    private fun speakText(text: String, isJapanese: Boolean) {
        if (!ttsReady || text.isBlank()) return
        tts?.language = if (isJapanese) Locale.JAPANESE else Locale.US
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tabi_tts_${System.currentTimeMillis()}")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun TabiRootApp(
    prefsRepo: UserPreferencesRepository,
    currentTheme: ThemeMode,
    onSpeak: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = this
        )
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedTabiShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Traveler",
                prefsRepo = prefsRepo,
                currentTheme = currentTheme,
                onSpeak = onSpeak
            )
        }
    }
}

@Composable
private fun AuthenticatedTabiShell(
    currentUserId: String,
    displayName: String,
    prefsRepo: UserPreferencesRepository,
    currentTheme: ThemeMode,
    onSpeak: (String, Boolean) -> Unit
) {
    val mainViewModel: TabiMainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val roomDb = TabiDatabase.getInstance(app)
                TabiMainViewModel(
                    firestoreRepo = TabiFirestoreRepository(db),
                    offlineDao = roomDb.offlineDao(),
                    prefsRepo = prefsRepo,
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val aiViewModel: TabiAiViewModel = viewModel(key = "ai_$currentUserId")
    val context = LocalContext.current
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val statusBanner by mainViewModel.statusBanner.collectAsStateWithLifecycle()
    val activeEtiquetteTip by mainViewModel.activeEtiquetteTip.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tokens = LocalTabiThemeTokens.current
    val haptics = rememberTabiHaptics()

    var selectedDest by remember { mutableIntStateOf(0) }
    var hubInitialSubTab by remember { mutableIntStateOf(0) }

    // If the user has already granted coarse location permission, check device coordinates for a Japanese district
    LaunchedEffect(currentUserId) {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCoarse) {
            runCatching {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val loc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) {
                    mainViewModel.triggerEtiquetteFromCoordinates(loc.latitude, loc.longitude)
                }
            }
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearBanner()
        }
    }

    if (selectedDest != 0) {
        BackHandler {
            haptics.tap()
            selectedDest = 0
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isExpandedScreen) {
                    val navShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    Box(
                        modifier = Modifier
                            .clip(navShape)
                            .background(tokens.glassSurfaceElevated)
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(tokens.glassBorderHighlight, tokens.glassBorderShadow, tokens.glassBorderHighlight)
                                ),
                                shape = navShape
                            )
                    ) {
                        NavigationBar(containerColor = Color.Transparent) {
                            TopDestination.entries.forEachIndexed { index, dest ->
                                NavigationBarItem(
                                    selected = selectedDest == index,
                                    onClick = {
                                        haptics.tap()
                                        selectedDest = index
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedDest == index) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(containerColor = tokens.glassSurfaceElevated) {
                        TopDestination.entries.forEachIndexed { index, dest ->
                            NavigationRailItem(
                                selected = selectedDest == index,
                                onClick = {
                                    haptics.tap()
                                    selectedDest = index
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedDest == index) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, fontWeight = FontWeight.Bold) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = TopDestination.entries[selectedDest],
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))) togetherWith
                            fadeOut(spring(stiffness = Spring.StiffnessMedium))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "root_nav_transition"
                ) { dest ->
                    when (dest) {
                        TopDestination.EXPLORE -> ExploreTranslatorScreen(
                            aiViewModel = aiViewModel,
                            userName = userProfile?.displayName?.ifBlank { displayName } ?: displayName,
                            homeCity = userProfile?.homeCity ?: "Tokyo",
                            currentTheme = currentTheme,
                            onSelectTheme = { mainViewModel.setThemeMode(it) },
                            onSpeakText = onSpeak,
                            onNavigateToEmergency = {
                                hubInitialSubTab = 1
                                selectedDest = TopDestination.HUB.ordinal
                            },
                            onNavigateToVault = {
                                hubInitialSubTab = 0
                                selectedDest = TopDestination.HUB.ordinal
                            },
                            isExpandedScreen = isExpandedScreen
                        )
                        TopDestination.LEARN -> LearnScreen(
                            mainViewModel = mainViewModel,
                            aiViewModel = aiViewModel,
                            onSpeakJapanese = { text -> onSpeak(text, true) }
                        )
                        TopDestination.MAP -> OfflineMapScreen(
                            mainViewModel = mainViewModel,
                            aiViewModel = aiViewModel,
                            isExpandedScreen = isExpandedScreen
                        )
                        TopDestination.PLAN -> PlanBudgetScreen(
                            mainViewModel = mainViewModel,
                            aiViewModel = aiViewModel
                        )
                        TopDestination.HUB -> HubSafetyVaultScreen(
                            mainViewModel = mainViewModel,
                            currentUserId = currentUserId,
                            initialSubTab = hubInitialSubTab
                        )
                    }
                }
            }
        }

        LocationEtiquetteOverlay(
            tip = activeEtiquetteTip,
            onDismiss = { mainViewModel.dismissEtiquetteOverlay() },
            onSpeakPhrase = { phrase -> onSpeak(phrase, true) },
            onOpenFullGuide = {
                mainViewModel.dismissEtiquetteOverlay()
                selectedDest = TopDestination.EXPLORE.ordinal
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
