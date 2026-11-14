package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.AudioTranscriptionResult
import com.example.data.remote.ChatModelTier
import com.example.data.remote.ConversationFeedbackResult
import com.example.data.remote.GeminiAiRepository
import com.example.data.remote.GroundingMode
import com.example.data.remote.GroundingSource
import com.example.data.remote.MapsDiscoveryResult
import com.example.data.remote.MotionReelDirection
import com.example.data.remote.NuanceTranslationResult
import com.example.data.remote.PersonalizedPlanResult
import com.google.firebase.ai.Chat
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface AiUiState<out T> {
    data object Idle : AiUiState<Nothing>
    data object Loading : AiUiState<Nothing>
    data class Success<T>(val data: T) : AiUiState<T>
    data class Error(val message: String) : AiUiState<Nothing>
}

data class ConciergeMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val sources: List<GroundingSource> = emptyList(),
    val searchEntryPointHtml: String? = null,
    val isStreaming: Boolean = false
)

data class LiveTranscriptLine(
    val id: String,
    val speaker: String,
    val text: String
)

sealed interface LiveVoiceState {
    data object Idle : LiveVoiceState
    data object Connecting : LiveVoiceState
    data object Active : LiveVoiceState
    data class Error(val message: String) : LiveVoiceState
}

class TabiAiViewModel(
    private val aiRepository: GeminiAiRepository = GeminiAiRepository()
) : ViewModel() {

    private val _translationState = MutableStateFlow<AiUiState<NuanceTranslationResult>>(AiUiState.Idle)
    val translationState: StateFlow<AiUiState<NuanceTranslationResult>> = _translationState.asStateFlow()

    private val _conversationState = MutableStateFlow<AiUiState<ConversationFeedbackResult>>(AiUiState.Idle)
    val conversationState: StateFlow<AiUiState<ConversationFeedbackResult>> = _conversationState.asStateFlow()

    private val _planState = MutableStateFlow<AiUiState<PersonalizedPlanResult>>(AiUiState.Idle)
    val planState: StateFlow<AiUiState<PersonalizedPlanResult>> = _planState.asStateFlow()

    private val _transcriptionState = MutableStateFlow<AiUiState<AudioTranscriptionResult>>(AiUiState.Idle)
    val transcriptionState: StateFlow<AiUiState<AudioTranscriptionResult>> = _transcriptionState.asStateFlow()

    private val _mapsDiscoveryState = MutableStateFlow<AiUiState<MapsDiscoveryResult>>(AiUiState.Idle)
    val mapsDiscoveryState: StateFlow<AiUiState<MapsDiscoveryResult>> = _mapsDiscoveryState.asStateFlow()

    private val _reelDirectionState = MutableStateFlow<AiUiState<MotionReelDirection>>(AiUiState.Idle)
    val reelDirectionState: StateFlow<AiUiState<MotionReelDirection>> = _reelDirectionState.asStateFlow()

    private val _restyleBitmapState = MutableStateFlow<AiUiState<Bitmap>>(AiUiState.Idle)
    val restyleBitmapState: StateFlow<AiUiState<Bitmap>> = _restyleBitmapState.asStateFlow()

    // Multi-turn Chatbot state
    private val _selectedModelTier = MutableStateFlow(ChatModelTier.BALANCED)
    val selectedModelTier: StateFlow<ChatModelTier> = _selectedModelTier.asStateFlow()

    private val _selectedGroundingMode = MutableStateFlow(GroundingMode.GOOGLE_SEARCH)
    val selectedGroundingMode: StateFlow<GroundingMode> = _selectedGroundingMode.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ConciergeMessage(
                id = "welcome",
                isUser = false,
                text = "こんにちは！I am TabiSensei, your bilingual Japan concierge. Toggle Google Search for live festivals & weather, or Google Maps for nearby ramen, shrines & transit!"
            )
        )
    )
    val chatMessages: StateFlow<List<ConciergeMessage>> = _chatMessages.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    private var activeChat: Chat? = null
    private var activeChatSignature: String = ""

    // Live Voice API state
    private val _liveVoiceState = MutableStateFlow<LiveVoiceState>(LiveVoiceState.Idle)
    val liveVoiceState: StateFlow<LiveVoiceState> = _liveVoiceState.asStateFlow()

    private val _liveTranscripts = MutableStateFlow<List<LiveTranscriptLine>>(emptyList())
    val liveTranscripts: StateFlow<List<LiveTranscriptLine>> = _liveTranscripts.asStateFlow()

    @OptIn(PublicPreviewAPI::class)
    private var activeLiveSession: LiveSession? = null

    fun setChatModelTier(tier: ChatModelTier) {
        _selectedModelTier.value = tier
    }

    fun setGroundingMode(mode: GroundingMode) {
        _selectedGroundingMode.value = mode
    }

    fun sendConciergeMessage(userText: String, lat: Double = 35.6762, lon: Double = 139.6503) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isChatSending.value) return

        val tier = _selectedModelTier.value
        val grounding = _selectedGroundingMode.value
        val sig = "${tier.name}_${grounding.name}_${"%.2f".format(lat)}_${"%.2f".format(lon)}"

        if (activeChat == null || activeChatSignature != sig) {
            val history: List<Content> = _chatMessages.value
                .filter { it.id != "welcome" && it.text.isNotBlank() }
                .map { msg -> content(if (msg.isUser) "user" else "model") { text(msg.text) } }
            activeChat = aiRepository.createConciergeChat(tier, grounding, lat, lon, history)
            activeChatSignature = sig
        }

        val chat = activeChat ?: return
        val userMsg = ConciergeMessage(id = "u_${System.currentTimeMillis()}", isUser = true, text = trimmed)
        val aiMsgId = "m_${System.currentTimeMillis() + 1}"
        _chatMessages.update {
            it + userMsg + ConciergeMessage(id = aiMsgId, isUser = false, text = "", isStreaming = true)
        }
        _isChatSending.value = true

        viewModelScope.launch {
            val sb = StringBuilder()
            val sourcesAcc = mutableListOf<GroundingSource>()
            var htmlSnippet: String? = null
            try {
                withTimeout(60_000L) {
                    aiRepository.streamConciergeMessage(chat, trimmed).collect { chunk ->
                        sb.append(chunk.textDelta)
                        chunk.sources.forEach { src ->
                            if (sourcesAcc.none { it.uri == src.uri && it.title == src.title }) {
                                sourcesAcc.add(src)
                            }
                        }
                        if (!chunk.searchEntryPointHtml.isNullOrBlank()) {
                            htmlSnippet = chunk.searchEntryPointHtml
                        }
                        _chatMessages.update { list ->
                            list.map { m ->
                                if (m.id == aiMsgId) {
                                    m.copy(
                                        text = sb.toString(),
                                        sources = sourcesAcc.toList(),
                                        searchEntryPointHtml = htmlSnippet,
                                        isStreaming = true
                                    )
                                } else m
                            }
                        }
                    }
                }
                _chatMessages.update { list ->
                    list.map { m ->
                        if (m.id == aiMsgId) {
                            m.copy(
                                text = sb.toString().ifBlank { "I didn't catch that—could you rephrase?" },
                                sources = sourcesAcc.toList(),
                                searchEntryPointHtml = htmlSnippet,
                                isStreaming = false
                            )
                        } else m
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Concierge chat failed", e)
                _chatMessages.update { list ->
                    list.map { m ->
                        if (m.id == aiMsgId) {
                            m.copy(
                                text = "Error: ${e.localizedMessage ?: "Failed to reach Gemini concierge."}",
                                isStreaming = false
                            )
                        } else m
                    }
                }
            } finally {
                _isChatSending.value = false
            }
        }
    }

    fun clearChatHistory() {
        activeChat = null
        _chatMessages.value = listOf(
            ConciergeMessage(
                id = "welcome_${System.currentTimeMillis()}",
                isUser = false,
                text = "新しい会話を始めましょう！Ask anything about Japan, live routes, or hidden spots."
            )
        )
    }

    @OptIn(PublicPreviewAPI::class)
    fun startLiveVoiceConversation() {
        if (_liveVoiceState.value == LiveVoiceState.Active || _liveVoiceState.value == LiveVoiceState.Connecting) return
        _liveVoiceState.value = LiveVoiceState.Connecting
        _liveTranscripts.value = emptyList()

        viewModelScope.launch {
            try {
                val session = aiRepository.startLiveVoiceSession { speaker, text ->
                    _liveTranscripts.update { current ->
                        val last = current.lastOrNull()
                        if (last != null && last.speaker == speaker) {
                            current.dropLast(1) + last.copy(text = "${last.text} $text".trim())
                        } else {
                            current + LiveTranscriptLine(
                                id = "${System.currentTimeMillis()}_${current.size}",
                                speaker = speaker,
                                text = text
                            )
                        }
                    }
                }
                activeLiveSession = session
                _liveVoiceState.value = LiveVoiceState.Active
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live API session failed", e)
                _liveVoiceState.value = LiveVoiceState.Error(
                    e.localizedMessage ?: "Live voice session failed to connect."
                )
            }
        }
    }

    @OptIn(PublicPreviewAPI::class)
    fun stopLiveVoiceConversation() {
        val session = activeLiveSession
        activeLiveSession = null
        _liveVoiceState.value = LiveVoiceState.Idle
        if (session != null) {
            viewModelScope.launch {
                runCatching { session.stopAudioConversation() }
                runCatching { session.close() }
            }
        }
    }

    fun transcribeRecordedAudio(audioBytes: ByteArray) {
        viewModelScope.launch {
            _transcriptionState.value = AiUiState.Loading
            try {
                val res = aiRepository.transcribeAudioClip(audioBytes)
                _transcriptionState.value = AiUiState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio transcription failed", e)
                _transcriptionState.value = AiUiState.Error(
                    e.localizedMessage ?: "Audio transcription failed."
                )
            }
        }
    }

    fun discoverPlacesWithGoogleMaps(query: String, cityName: String, lat: Double, lon: Double) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _mapsDiscoveryState.value = AiUiState.Loading
            try {
                val res = aiRepository.discoverPlacesWithGoogleMaps(query.trim(), cityName, lat, lon)
                _mapsDiscoveryState.value = AiUiState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Maps discovery failed", e)
                _mapsDiscoveryState.value = AiUiState.Error(
                    e.localizedMessage ?: "Google Maps grounding unavailable."
                )
            }
        }
    }

    fun directPhotoReel(bitmap: Bitmap, motionPreset: String, aspectLabel: String) {
        viewModelScope.launch {
            _reelDirectionState.value = AiUiState.Loading
            try {
                val dir = aiRepository.directPhotoMotionReel(bitmap, motionPreset, aspectLabel)
                _reelDirectionState.value = AiUiState.Success(dir)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo reel direction failed", e)
                _reelDirectionState.value = AiUiState.Error(
                    e.localizedMessage ?: "Could not analyze photo for motion reel."
                )
            }
        }
    }

    fun restylePhoto(bitmap: Bitmap, styleName: String) {
        viewModelScope.launch {
            _restyleBitmapState.value = AiUiState.Loading
            try {
                val restyled = aiRepository.restylePhotoWithNanoBanana(bitmap, styleName)
                _restyleBitmapState.value = AiUiState.Success(restyled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Nano Banana restyle failed", e)
                _restyleBitmapState.value = AiUiState.Error(
                    e.localizedMessage ?: "Image restyling failed."
                )
            }
        }
    }

    fun resetRestyleState() {
        _restyleBitmapState.value = AiUiState.Idle
    }

    fun translate(
        inputText: String,
        japaneseToEnglish: Boolean,
        registerPreference: String
    ) {
        if (inputText.isBlank()) return
        viewModelScope.launch {
            _translationState.value = AiUiState.Loading
            try {
                val source = if (japaneseToEnglish) "Japanese" else "English"
                val target = if (japaneseToEnglish) "English" else "Japanese"
                val result = aiRepository.translateWithNuance(inputText.trim(), source, target, registerPreference)
                _translationState.value = AiUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                _translationState.value = AiUiState.Error(
                    e.localizedMessage ?: "Nuance translation failed. Please check connectivity."
                )
            }
        }
    }

    fun evaluateConversation(
        scenarioTitle: String,
        targetPhraseHint: String,
        userSpokenJapanese: String,
        viaVoiceRecognition: Boolean
    ) {
        if (userSpokenJapanese.isBlank()) return
        viewModelScope.launch {
            _conversationState.value = AiUiState.Loading
            try {
                val result = aiRepository.evaluateSpeechAndRespond(
                    scenarioTitle = scenarioTitle,
                    targetPhraseHint = targetPhraseHint,
                    userSpokenJapanese = userSpokenJapanese.trim(),
                    viaVoiceRecognition = viaVoiceRecognition
                )
                _conversationState.value = AiUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Conversation evaluation failed", e)
                _conversationState.value = AiUiState.Error(
                    e.localizedMessage ?: "Pronunciation coach unavailable."
                )
            }
        }
    }

    fun resetConversationState() {
        _conversationState.value = AiUiState.Idle
    }

    fun generatePersonalizedPlan(
        interests: List<String>,
        tripDays: Int,
        visitedRegions: List<String>,
        dailyBudgetJpy: Long
    ) {
        viewModelScope.launch {
            _planState.value = AiUiState.Loading
            try {
                val result = aiRepository.generatePersonalizedItinerary(
                    interests = interests,
                    tripDays = tripDays,
                    visitedRegions = visitedRegions,
                    dailyBudgetJpy = dailyBudgetJpy
                )
                _planState.value = AiUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI plan generation failed", e)
                _planState.value = AiUiState.Error(
                    e.localizedMessage ?: "Could not generate personalized recommendations."
                )
            }
        }
    }

    @OptIn(PublicPreviewAPI::class)
    override fun onCleared() {
        val s = activeLiveSession
        activeLiveSession = null
        if (s != null) {
            runCatching { s.stopAudioConversation() }
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { s.close() }
            }
        }
        super.onCleared()
    }

    private companion object {
        const val TAG = "TabiAiVM"
    }
}
