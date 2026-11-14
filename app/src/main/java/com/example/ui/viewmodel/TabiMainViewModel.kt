package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.OfflineDocumentEntity
import com.example.data.local.OfflineExpenseEntity
import com.example.data.local.OfflineItineraryEntity
import com.example.data.local.OfflineMapRegionEntity
import com.example.data.local.TabiOfflineDao
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.BudgetExpense
import com.example.data.model.EncryptedTravelDoc
import com.example.data.model.FlightAlert
import com.example.data.model.GuideContent
import com.example.data.model.ItineraryItem
import com.example.data.model.LessonProgress
import com.example.data.model.LocationEtiquetteTip
import com.example.data.model.TravelerPost
import com.example.data.model.UserProfile
import com.example.data.remote.CurrencyRateRepository
import com.example.data.remote.GeneratedItineraryActivity
import com.example.data.remote.TabiFirestoreRepository
import com.example.security.EncryptedEnvelope
import com.example.security.TotpManager
import com.example.security.VaultCryptoManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface SyncState<out T> {
    data object Loading : SyncState<Nothing>
    data class Success<T>(val data: T) : SyncState<T>
    data class Error(val message: String) : SyncState<Nothing>
}

data class FxRateState(
    val homeCurrency: String = CurrencyRateRepository.resolveDefaultHomeCurrency(),
    val ratesPerJpy: Map<String, Double> = emptyMap(),
    val fetchedAtMillis: Long = 0L,
    val provider: String = "",
    val isRefreshing: Boolean = false,
    val isLiveSession: Boolean = false,
    val errorMessage: String? = null
) {
    val activeRatePerJpy: Double?
        get() = ratesPerJpy[homeCurrency]

    fun convertJpy(amountJpy: Long): Double? =
        activeRatePerJpy?.let { CurrencyRateRepository.convertJpyToHome(amountJpy, it) }

    fun formatConvertedJpy(amountJpy: Long): String? =
        convertJpy(amountJpy)?.let { CurrencyRateRepository.formatHomeCurrency(it, homeCurrency) }
}

class TabiMainViewModel(
    private val firestoreRepo: TabiFirestoreRepository,
    private val offlineDao: TabiOfflineDao,
    private val prefsRepo: UserPreferencesRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _activeEtiquetteTip = MutableStateFlow<LocationEtiquetteTip?>(null)
    val activeEtiquetteTip: StateFlow<LocationEtiquetteTip?> = _activeEtiquetteTip.asStateFlow()
    private var lastEtiquetteZoneKey: String? = null

    private val _vaultUnlocked = MutableStateFlow(false)
    val vaultUnlocked: StateFlow<Boolean> = _vaultUnlocked.asStateFlow()

    private val _activeTotpSecret = MutableStateFlow<String?>(null)
    val activeTotpSecret: StateFlow<String?> = _activeTotpSecret.asStateFlow()

    private val _fxRateState = MutableStateFlow(
        FxRateState(homeCurrency = CurrencyRateRepository.resolveDefaultHomeCurrency())
    )
    val fxRateState: StateFlow<FxRateState> = _fxRateState.asStateFlow()

    val appPreferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow
        .onEach { prefs ->
            val resolvedHome = prefs.homeCurrencyCode.ifBlank {
                CurrencyRateRepository.resolveDefaultHomeCurrency()
            }
            val current = _fxRateState.value
            val cachedMap = if (current.ratesPerJpy.isEmpty() && prefs.cachedFxRatesJson.isNotBlank()) {
                CurrencyRateRepository.parseCachedRatesJson(prefs.cachedFxRatesJson)
            } else {
                current.ratesPerJpy
            }
            _fxRateState.value = current.copy(
                homeCurrency = resolvedHome,
                ratesPerJpy = cachedMap,
                fetchedAtMillis = if (current.fetchedAtMillis == 0L) prefs.cachedFxFetchedAtMillis else current.fetchedAtMillis,
                provider = if (current.provider.isBlank()) prefs.cachedFxProvider else current.provider
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppPreferences())

    val mapRegions: StateFlow<List<OfflineMapRegionEntity>> = offlineDao.observeMapRegions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val offlineDocuments: StateFlow<List<OfflineDocumentEntity>> = offlineDao.observeOfflineDocuments(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val offlineItinerary: StateFlow<List<OfflineItineraryEntity>> = offlineDao.observeOfflineItinerary(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val offlineExpenses: StateFlow<List<OfflineExpenseEntity>> = offlineDao.observeOfflineExpenses(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userProfile: StateFlow<UserProfile?> = firestoreRepo.observeUserProfile(currentUserId)
        .onEach { profile ->
            if (profile != null && profile.totpSecretEncrypted.isNotBlank()) {
                runCatching {
                    val parts = profile.totpSecretEncrypted.split("|")
                    if (parts.size == 2) {
                        val decrypted = VaultCryptoManager.decrypt(
                            EncryptedEnvelope(parts[0], parts[1], "", "KEYSTORE")
                        )
                        _activeTotpSecret.value = decrypted
                    }
                }
            } else if (profile?.mfaEnabled == false) {
                _vaultUnlocked.value = true
            }
        }
        .catch { error ->
            Log.w(TAG, "Profile stream error", unwrapFirestoreError(error))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val itineraryState: StateFlow<SyncState<List<ItineraryItem>>> = firestoreRepo
        .observeItineraries(currentUserId)
        .onEach { items ->
            offlineDao.clearOfflineItineraryForUser(currentUserId)
            offlineDao.upsertOfflineItinerary(
                items.map {
                    OfflineItineraryEntity(
                        id = it.id,
                        userId = currentUserId,
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
            )
            prefsRepo.updateLastSync(System.currentTimeMillis())
            val nextActiveStop = items
                .sortedWith(compareBy<ItineraryItem> { it.dayNumber }.thenBy { it.timeSlot })
                .firstOrNull { !it.completed }
            if (nextActiveStop != null) {
                triggerLocationEtiquette(
                    city = nextActiveStop.city,
                    districtOrTitle = nextActiveStop.title,
                    notes = nextActiveStop.notes,
                    sourceLabel = "Itinerary • Day ${nextActiveStop.dayNumber}",
                    forceShow = false
                )
            }
        }
        .toSyncStateFlow("itineraries")

    val expensesState: StateFlow<SyncState<List<BudgetExpense>>> = firestoreRepo
        .observeExpenses(currentUserId)
        .onEach { items ->
            offlineDao.clearOfflineExpensesForUser(currentUserId)
            offlineDao.upsertOfflineExpenses(
                items.map {
                    OfflineExpenseEntity(
                        id = it.id,
                        userId = currentUserId,
                        title = it.title,
                        amountJpy = it.amountJpy,
                        category = it.category,
                        dayNumber = it.dayNumber,
                        notes = it.notes
                    )
                }
            )
        }
        .toSyncStateFlow("expenses")

    val documentsState: StateFlow<SyncState<List<EncryptedTravelDoc>>> = firestoreRepo
        .observeEncryptedDocuments(currentUserId)
        .onEach { docs ->
            val now = System.currentTimeMillis()
            offlineDao.upsertOfflineDocuments(
                docs.map {
                    OfflineDocumentEntity(
                        id = it.id,
                        userId = currentUserId,
                        title = it.title,
                        docType = it.docType,
                        encryptedPayload = it.encryptedPayload,
                        ivBase64 = it.ivBase64,
                        saltBase64 = it.saltBase64,
                        keyMode = it.keyMode,
                        cachedAtMillis = now
                    )
                }
            )
        }
        .toSyncStateFlow("documents")

    val lessonProgressState: StateFlow<SyncState<List<LessonProgress>>> = firestoreRepo
        .observeLessonProgress(currentUserId)
        .toSyncStateFlow("lesson_progress")

    val alertsState: StateFlow<SyncState<List<FlightAlert>>> = firestoreRepo
        .observeFlightAlerts(currentUserId)
        .toSyncStateFlow("alerts")

    val communityPostsState: StateFlow<SyncState<List<TravelerPost>>> = firestoreRepo
        .observeTravelerPosts()
        .toSyncStateFlow("traveler_posts")

    init {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.ensureUserProfile(initialDisplayName)
            }.onFailure { e ->
                Log.w(TAG, "ensureUserProfile error", e)
            }
            seedOfflineMapRegionsIfEmpty()
            refreshExchangeRates(force = false)
        }
    }

    private suspend fun seedOfflineMapRegionsIfEmpty() {
        val seed = GuideContent.offlineCityMaps.map { city ->
            OfflineMapRegionEntity(
                regionId = city.regionId,
                name = city.name,
                japaneseName = city.japaneseName,
                description = city.description,
                downloadedAtMillis = if (city.regionId == "tokyo") System.currentTimeMillis() else 0L,
                sizeMb = city.sizeMb,
                isDownloaded = city.regionId == "tokyo"
            )
        }
        offlineDao.upsertMapRegions(seed)
    }

    private fun <T> Flow<List<T>>.toSyncStateFlow(label: String): StateFlow<SyncState<List<T>>> =
        this.map<List<T>, SyncState<List<T>>> { SyncState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                val msg = (root ?: error).message ?: "Failed to sync $label"
                Log.w(TAG, "Error observing $label", root ?: error)
                emit(SyncState.Error(msg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SyncState.Loading)

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showMessage(message: String) {
        _statusBanner.value = message
    }

    fun triggerLocationEtiquette(
        city: String,
        districtOrTitle: String = "",
        notes: String = "",
        sourceLabel: String = "Itinerary",
        forceShow: Boolean = false
    ) {
        val resolved = GuideContent.resolveEtiquetteForLocation(
            city = city,
            districtOrTitle = districtOrTitle,
            notes = notes
        )
        if (forceShow || resolved.zoneKey != lastEtiquetteZoneKey) {
            lastEtiquetteZoneKey = resolved.zoneKey
            _activeEtiquetteTip.value = resolved.copy(sourceLabel = sourceLabel)
        }
    }

    fun checkInItineraryStop(stop: ItineraryItem) {
        triggerLocationEtiquette(
            city = stop.city,
            districtOrTitle = stop.title,
            notes = stop.notes,
            sourceLabel = "Arrived • Day ${stop.dayNumber}",
            forceShow = true
        )
    }

    fun triggerEtiquetteFromCoordinates(
        lat: Double,
        lon: Double,
        forceShow: Boolean = false
    ): Boolean {
        val matched = GuideContent.resolveEtiquetteForCoordinates(lat, lon) ?: return false
        if (forceShow || matched.zoneKey != lastEtiquetteZoneKey) {
            lastEtiquetteZoneKey = matched.zoneKey
            _activeEtiquetteTip.value = matched.copy(sourceLabel = "GPS • ${matched.districtName}")
        }
        return true
    }

    fun dismissEtiquetteOverlay() {
        _activeEtiquetteTip.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setNotificationsEnabled(enabled) }
    }

    fun setBiometricVaultLock(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBiometricVaultLock(enabled) }
    }

    fun setHomeCurrency(currencyCode: String) {
        val clean = currencyCode.trim().uppercase()
        if (clean.length != 3) return
        _fxRateState.value = _fxRateState.value.copy(homeCurrency = clean)
        viewModelScope.launch {
            prefsRepo.setHomeCurrencyCode(clean)
            if (_fxRateState.value.ratesPerJpy[clean] == null) {
                refreshExchangeRates(force = true)
            }
            _statusBanner.value = "Home currency set to $clean."
        }
    }

    fun refreshExchangeRates(force: Boolean = true) {
        val current = _fxRateState.value
        if (current.isRefreshing) return
        val staleMs = 5 * 60 * 1000L
        val isFresh = current.ratesPerJpy.isNotEmpty() &&
            (System.currentTimeMillis() - current.fetchedAtMillis) < staleMs
        if (!force && isFresh) return

        _fxRateState.value = current.copy(isRefreshing = true, errorMessage = null)
        viewModelScope.launch {
            val res = CurrencyRateRepository.fetchLatestJpyRates()
            res.onSuccess { snapshot ->
                _fxRateState.value = _fxRateState.value.copy(
                    ratesPerJpy = snapshot.rates,
                    fetchedAtMillis = snapshot.fetchedAtMillis,
                    provider = snapshot.provider,
                    isRefreshing = false,
                    isLiveSession = true,
                    errorMessage = null
                )
                prefsRepo.saveCachedFxRates(
                    ratesJson = snapshot.rawRatesJson,
                    fetchedAtMillis = snapshot.fetchedAtMillis,
                    provider = snapshot.provider
                )
                if (force) {
                    val code = _fxRateState.value.homeCurrency
                    val rate = snapshot.rates[code]
                    if (rate != null) {
                        _statusBanner.value = "Updated live FX: 1,000 JPY = ${CurrencyRateRepository.formatHomeCurrency(rate * 1000.0, code)}"
                    }
                }
            }.onFailure { err ->
                _fxRateState.value = _fxRateState.value.copy(
                    isRefreshing = false,
                    isLiveSession = false,
                    errorMessage = err.localizedMessage ?: "Unable to fetch live FX rates."
                )
            }
        }
    }

    fun toggleOfflineMapRegion(regionId: String, currentlyDownloaded: Boolean) {
        viewModelScope.launch {
            offlineDao.setRegionDownloaded(
                regionId = regionId,
                downloaded = !currentlyDownloaded,
                timestamp = if (!currentlyDownloaded) System.currentTimeMillis() else 0L
            )
            _statusBanner.value = if (!currentlyDownloaded) {
                "Offline map vector pack cached for offline use."
            } else {
                "Offline map pack removed from local storage."
            }
        }
    }

    // --- Profile & Travel History ---
    fun updateTravelProfile(
        homeCity: String,
        dailyBudgetJpy: Long,
        interests: List<String>,
        visitedRegions: List<String>
    ) {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.updateUserProfileFields(
                    mapOf(
                        "homeCity" to homeCity.take(60),
                        "dailyBudgetJpy" to dailyBudgetJpy.coerceIn(1000L, 10000000L),
                        "interests" to interests.take(10),
                        "visitedRegions" to visitedRegions.take(20)
                    )
                )
            }.onSuccess {
                _statusBanner.value = "Travel profile synced across devices."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    fun saveMedicalEmergencyCard(bloodType: String, allergies: String, japaneseSummary: String) {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.updateUserProfileFields(
                    mapOf(
                        "bloodType" to bloodType.take(20),
                        "allergies" to allergies.take(300),
                        "emergencyMedicalSummaryJa" to japaneseSummary.take(1000)
                    )
                )
            }.onSuccess {
                _statusBanner.value = "Japanese Medical Card saved & synced."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not save Medical Card."
            }
        }
    }

    // --- Itinerary ---
    fun addItineraryItem(item: ItineraryItem) {
        viewModelScope.launch {
            runCatching { firestoreRepo.addItineraryItem(item) }
                .onSuccess {
                    _statusBanner.value = "Added '${item.title}' to Day ${item.dayNumber}."
                    triggerLocationEtiquette(
                        city = item.city,
                        districtOrTitle = item.title,
                        notes = item.notes,
                        sourceLabel = "Itinerary • Day ${item.dayNumber}",
                        forceShow = false
                    )
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not add itinerary stop." }
        }
    }

    fun importGeneratedActivities(activities: List<GeneratedItineraryActivity>) {
        viewModelScope.launch {
            runCatching {
                for (act in activities) {
                    firestoreRepo.addItineraryItem(
                        ItineraryItem(
                            dayNumber = act.dayNumber,
                            timeSlot = act.timeSlot,
                            title = act.title,
                            city = act.city,
                            category = act.category,
                            notes = act.notes,
                            estimatedCostJpy = act.estimatedCostJpy
                        )
                    )
                }
            }.onSuccess {
                _statusBanner.value = "Imported ${activities.size} AI itinerary stops!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to import AI plan."
            }
        }
    }

    fun toggleItineraryCompleted(item: ItineraryItem) {
        viewModelScope.launch {
            runCatching { firestoreRepo.toggleItineraryCompleted(item) }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Update failed." }
        }
    }

    fun deleteItineraryItem(itemId: String) {
        viewModelScope.launch {
            runCatching { firestoreRepo.deleteItineraryItem(itemId) }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    // --- Budget ---
    fun addExpense(title: String, amountJpy: Long, category: String, dayNumber: Long, notes: String) {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.addExpense(
                    BudgetExpense(
                        title = title,
                        amountJpy = amountJpy,
                        category = category,
                        dayNumber = dayNumber,
                        notes = notes
                    )
                )
            }.onSuccess {
                _statusBanner.value = "Logged ¥$amountJpy expense."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not log expense."
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            runCatching { firestoreRepo.deleteExpense(expenseId) }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to remove expense." }
        }
    }

    // --- Lessons ---
    fun recordLessonCompletion(lessonId: String, score: Long, masteredPhrases: Long) {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.saveLessonProgress(lessonId, completed = true, score = score, masteredPhrases = masteredPhrases)
            }.onSuccess {
                _statusBanner.value = "Lesson progress saved ($score%)."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save lesson progress."
            }
        }
    }

    // --- E2EE Document Vault & MFA ---
    fun enableTotpMfa(): String {
        val secret = TotpManager.generateSecretBase32()
        _activeTotpSecret.value = secret
        viewModelScope.launch {
            runCatching {
                val env = VaultCryptoManager.encrypt(secret, passphrase = null)
                firestoreRepo.updateUserProfileFields(
                    mapOf(
                        "mfaEnabled" to true,
                        "totpSecretEncrypted" to "${env.ciphertextBase64}|${env.ivBase64}"
                    )
                )
                _vaultUnlocked.value = true
            }.onSuccess {
                _statusBanner.value = "2FA / TOTP Multi-Factor Security enabled."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save MFA configuration."
            }
        }
        return secret
    }

    fun disableTotpMfa() {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.updateUserProfileFields(
                    mapOf(
                        "mfaEnabled" to false,
                        "totpSecretEncrypted" to ""
                    )
                )
                _activeTotpSecret.value = null
                _vaultUnlocked.value = true
            }.onSuccess {
                _statusBanner.value = "2FA TOTP disabled."
            }
        }
    }

    fun verifyTotpAndUnlock(code: String): Boolean {
        val secret = _activeTotpSecret.value ?: return true
        val ok = TotpManager.verifyCode(secret, code)
        if (ok) {
            _vaultUnlocked.value = true
            _statusBanner.value = "MFA verified. Vault unlocked."
        } else {
            _statusBanner.value = "Invalid 6-digit authenticator code."
        }
        return ok
    }

    fun unlockVaultViaBiometric() {
        _vaultUnlocked.value = true
        _statusBanner.value = "Device biometric/credential verified."
    }

    fun lockVault() {
        _vaultUnlocked.value = false
    }

    fun saveEncryptedDocument(
        title: String,
        docType: String,
        plaintextSecretContent: String,
        passphrase: String?
    ) {
        viewModelScope.launch {
            runCatching {
                val envelope = VaultCryptoManager.encrypt(plaintextSecretContent, passphrase)
                firestoreRepo.saveEncryptedDocument(
                    EncryptedTravelDoc(
                        title = title,
                        docType = docType,
                        encryptedPayload = envelope.ciphertextBase64,
                        ivBase64 = envelope.ivBase64,
                        saltBase64 = envelope.saltBase64,
                        keyMode = envelope.keyMode
                    )
                )
            }.onSuccess {
                _statusBanner.value = "Document encrypted (AES-256-GCM) & synced."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Encryption failed."
            }
        }
    }

    fun decryptDocumentPayload(doc: EncryptedTravelDoc, passphrase: String?): Result<String> {
        return runCatching {
            VaultCryptoManager.decrypt(
                EncryptedEnvelope(
                    ciphertextBase64 = doc.encryptedPayload,
                    ivBase64 = doc.ivBase64,
                    saltBase64 = doc.saltBase64,
                    keyMode = doc.keyMode
                ),
                passphrase = passphrase
            )
        }
    }

    fun deleteEncryptedDocument(docId: String) {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.deleteEncryptedDocument(docId)
                offlineDao.deleteOfflineDocument(docId)
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Delete failed."
            }
        }
    }

    // --- Cloud Storage Export / Import JSON (Google Drive / iCloud Drive / SAF providers) ---
    fun buildBackupExportJson(): String {
        val root = JSONObject()
        root.put("app", "TabiGuide")
        root.put("exportedAtEpoch", System.currentTimeMillis())

        val itinArray = JSONArray()
        offlineItinerary.value.forEach { item ->
            itinArray.put(
                JSONObject().apply {
                    put("dayNumber", item.dayNumber)
                    put("timeSlot", item.timeSlot)
                    put("title", item.title)
                    put("city", item.city)
                    put("category", item.category)
                    put("notes", item.notes)
                    put("estimatedCostJpy", item.estimatedCostJpy)
                    put("completed", item.completed)
                }
            )
        }
        root.put("itinerary", itinArray)

        val docsArray = JSONArray()
        offlineDocuments.value.forEach { doc ->
            docsArray.put(
                JSONObject().apply {
                    put("title", doc.title)
                    put("docType", doc.docType)
                    put("encryptedPayload", doc.encryptedPayload)
                    put("ivBase64", doc.ivBase64)
                    put("saltBase64", doc.saltBase64)
                    put("keyMode", doc.keyMode)
                }
            )
        }
        root.put("encryptedDocuments", docsArray)
        return root.toString(2)
    }

    fun importBackupJson(jsonText: String) {
        viewModelScope.launch {
            runCatching {
                val root = JSONObject(jsonText)
                val itin = root.optJSONArray("itinerary")
                var importedCount = 0
                if (itin != null) {
                    for (i in 0 until itin.length()) {
                        val obj = itin.getJSONObject(i)
                        firestoreRepo.addItineraryItem(
                            ItineraryItem(
                                dayNumber = obj.optLong("dayNumber", 1L),
                                timeSlot = obj.optString("timeSlot", "09:00"),
                                title = obj.optString("title", "Imported Stop"),
                                city = obj.optString("city", "Tokyo"),
                                category = obj.optString("category", "Culture"),
                                notes = obj.optString("notes", ""),
                                estimatedCostJpy = obj.optLong("estimatedCostJpy", 0L),
                                completed = obj.optBoolean("completed", false)
                            )
                        )
                        importedCount++
                    }
                }
                val docs = root.optJSONArray("encryptedDocuments")
                if (docs != null) {
                    for (i in 0 until docs.length()) {
                        val obj = docs.getJSONObject(i)
                        firestoreRepo.saveEncryptedDocument(
                            EncryptedTravelDoc(
                                title = obj.optString("title", "Imported Doc"),
                                docType = obj.optString("docType", "Passport"),
                                encryptedPayload = obj.optString("encryptedPayload"),
                                ivBase64 = obj.optString("ivBase64"),
                                saltBase64 = obj.optString("saltBase64"),
                                keyMode = obj.optString("keyMode", "KEYSTORE")
                            )
                        )
                        importedCount++
                    }
                }
                importedCount
            }.onSuccess { count ->
                _statusBanner.value = "Restored $count items from cloud backup file."
            }.onFailure {
                _statusBanner.value = "Backup import failed: ${it.localizedMessage}"
            }
        }
    }

    // --- Flight Alerts ---
    fun addFlightAlert(
        flightNumber: String,
        title: String,
        departureTimeText: String,
        gateOrTerminal: String,
        triggerEpochMillis: Long
    ) {
        viewModelScope.launch {
            runCatching {
                firestoreRepo.addFlightAlert(
                    FlightAlert(
                        flightNumber = flightNumber,
                        title = title,
                        departureTimeText = departureTimeText,
                        triggerEpochMillis = triggerEpochMillis,
                        gateOrTerminal = gateOrTerminal,
                        status = "Scheduled"
                    )
                )
            }.onSuccess {
                _statusBanner.value = "Flight alert saved & scheduled."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save alert."
            }
        }
    }

    fun deleteFlightAlert(alertId: String) {
        viewModelScope.launch {
            runCatching { firestoreRepo.deleteFlightAlert(alertId) }
        }
    }

    // --- Community ---
    fun createCommunityPost(city: String, topic: String, message: String, meetupTime: String) {
        val displayName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
        viewModelScope.launch {
            runCatching {
                firestoreRepo.createTravelerPost(
                    authorName = displayName,
                    city = city,
                    topic = topic,
                    message = message,
                    meetupTime = meetupTime
                )
            }.onSuccess {
                _statusBanner.value = "Posted to nearby travelers in $city."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to publish post."
            }
        }
    }

    fun togglePostRsvp(post: TravelerPost) {
        viewModelScope.launch {
            runCatching { firestoreRepo.togglePostRsvp(post) }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "RSVP failed." }
        }
    }

    fun deleteCommunityPost(postId: String) {
        viewModelScope.launch {
            runCatching { firestoreRepo.deleteTravelerPost(postId) }
        }
    }

    private companion object {
        const val TAG = "TabiMainVM"
    }
}
