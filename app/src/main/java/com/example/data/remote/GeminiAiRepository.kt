package com.example.data.remote

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.Transcription
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.retrievalConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class NuanceTranslationResult(
    val translatedText: String,
    val reading: String,
    val romaji: String,
    val formalityLevel: String,
    val grammaticalNuances: String,
    val culturalContext: String,
    val politeAlternative: String,
    val casualAlternative: String
)

data class ConversationFeedbackResult(
    val japaneseReply: String,
    val romajiReply: String,
    val englishMeaning: String,
    val pronunciationFeedback: String,
    val grammarScore: Int,
    val naturalnessTip: String
)

data class GeneratedItineraryActivity(
    val dayNumber: Long,
    val timeSlot: String,
    val title: String,
    val city: String,
    val category: String,
    val notes: String,
    val estimatedCostJpy: Long
)

data class DestinationRecommendation(
    val regionName: String,
    val japaneseName: String,
    val matchReason: String,
    val bestSeasonOrTip: String
)

data class PersonalizedPlanResult(
    val recommendations: List<DestinationRecommendation>,
    val activities: List<GeneratedItineraryActivity>
)

enum class ChatModelTier(val label: String, val modelId: String, val badge: String) {
    FAST("Flash Lite (Fast)", "gemini-3.5-flash-lite", "高速"),
    BALANCED("Gemini Flash (General)", "gemini-3.8-flash", "標準"),
    DEEP("Gemini Argon (Deep)", "gemini-4-argon", "深考")
}

enum class GroundingMode(val label: String) {
    NONE("Standard AI"),
    GOOGLE_SEARCH("Google Search"),
    GOOGLE_MAPS("Google Maps")
}

data class GroundingSource(
    val title: String,
    val uri: String,
    val isGoogleMaps: Boolean,
    val placeId: String = ""
)

data class GroundedChatChunk(
    val textDelta: String,
    val sources: List<GroundingSource>,
    val searchEntryPointHtml: String? = null
)

data class MapsDiscoveryResult(
    val summary: String,
    val places: List<GroundingSource>
)

data class AudioTranscriptionResult(
    val detectedLanguage: String,
    val japaneseTranscript: String,
    val readingOrRomaji: String,
    val englishTranslation: String,
    val nuanceNote: String
)

data class MotionReelDirection(
    val japaneseTitle: String,
    val englishSubtitle: String,
    val haikuCaption: String
)

class GeminiAiRepository {

    private val translationSchema = Schema.obj(
        properties = mapOf(
            "translatedText" to Schema.string(description = "Primary accurate translation"),
            "reading" to Schema.string(description = "Hiragana/katakana reading of Japanese text"),
            "romaji" to Schema.string(description = "Romanized pronunciation with pitch hints"),
            "formalityLevel" to Schema.string(description = "Register such as Keigo/Honorific, Polite Teineigo, Casual, or Written"),
            "grammaticalNuances" to Schema.string(description = "Detailed breakdown of particles, verb endings, implied subjects, and grammatical nuances"),
            "culturalContext" to Schema.string(description = "When and how to use this phrase appropriately in Japan"),
            "politeAlternative" to Schema.string(description = "More formal/polite phrasing"),
            "casualAlternative" to Schema.string(description = "Natural casual phrasing for friends")
        )
    )

    private val conversationSchema = Schema.obj(
        properties = mapOf(
            "japaneseReply" to Schema.string(description = "Natural in-character Japanese reply"),
            "romajiReply" to Schema.string(description = "Romaji for the reply"),
            "englishMeaning" to Schema.string(description = "English translation of the reply"),
            "pronunciationFeedback" to Schema.string(description = "Constructive feedback on the traveler's spoken/recognized Japanese pronunciation, pitch accent, and word choice"),
            "grammarScore" to Schema.integer(description = "Accuracy score from 0 to 100"),
            "naturalnessTip" to Schema.string(description = "One concrete tip to sound more natural")
        )
    )

    private val itinerarySchema = Schema.obj(
        properties = mapOf(
            "recommendations" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "regionName" to Schema.string(description = "Japanese city or region"),
                        "japaneseName" to Schema.string(description = "Kanji/Kana name"),
                        "matchReason" to Schema.string(description = "Why this matches the traveler's interests and past history"),
                        "bestSeasonOrTip" to Schema.string(description = "Insider logistics tip")
                    )
                ),
                description = "Top 3 personalized destination recommendations"
            ),
            "activities" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "dayNumber" to Schema.integer(description = "Day index starting at 1"),
                        "timeSlot" to Schema.string(description = "24-hour time e.g. 09:30"),
                        "title" to Schema.string(description = "Activity title"),
                        "city" to Schema.string(description = "City name"),
                        "category" to Schema.string(description = "One of: Food, Temples, Transit, Nature, Shopping, Pop Culture"),
                        "notes" to Schema.string(description = "Practical transit or cultural note"),
                        "estimatedCostJpy" to Schema.integer(description = "Estimated cost in JPY")
                    )
                ),
                description = "Day-by-day itinerary items"
            )
        )
    )

    private val transcriptionSchema = Schema.obj(
        properties = mapOf(
            "detectedLanguage" to Schema.string(description = "Primary language detected"),
            "japaneseTranscript" to Schema.string(description = "Verbatim Japanese transcription or Japanese equivalent"),
            "readingOrRomaji" to Schema.string(description = "Hiragana reading & romaji"),
            "englishTranslation" to Schema.string(description = "Natural English translation"),
            "nuanceNote" to Schema.string(description = "Tone, politeness, or grammar nuance observed")
        )
    )

    private val reelDirectionSchema = Schema.obj(
        properties = mapOf(
            "japaneseTitle" to Schema.string(description = "Short poetic Japanese title (max 14 chars)"),
            "englishSubtitle" to Schema.string(description = "Atmospheric travel caption (max 48 chars)"),
            "haikuCaption" to Schema.string(description = "Short Japan travel reflection")
        )
    )

    private val translatorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
            }
        )
    }

    private val conversationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = conversationSchema
            }
        )
    }

    private val itineraryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = itinerarySchema
            }
        )
    }

    private val transcriptionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = transcriptionSchema
            }
        )
    }

    private val reelDirectorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reelDirectionSchema
            }
        )
    }

    private val nanoBananaRestyleModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun createConciergeChat(
        modelTier: ChatModelTier,
        groundingMode: GroundingMode,
        lat: Double,
        lon: Double,
        history: List<Content> = emptyList()
    ): Chat {
        val tools = when (groundingMode) {
            GroundingMode.NONE -> null
            GroundingMode.GOOGLE_SEARCH -> listOf(Tool.googleSearch())
            GroundingMode.GOOGLE_MAPS -> listOf(Tool.googleMaps())
        }
        val toolConfig = if (groundingMode == GroundingMode.GOOGLE_MAPS) {
            ToolConfig(
                retrievalConfig = retrievalConfig {
                    latLng = LatLng(latitude = lat, longitude = lon)
                    languageCode = "en-US"
                }
            )
        } else null

        val systemPrompt = """
            You are TabiSensei (旅先生), an elite bilingual Japanese travel concierge, linguist, and cultural guide.
            Give crisp, practical, culturally respectful advice for travelers in Japan, including Japanese script + romaji
            for key terms, train/IC card tips, etiquette nuances, and real verified locations when grounding tools are enabled.
        """.trimIndent()

        val model = Firebase.ai.generativeModel(
            modelName = modelTier.modelId,
            tools = tools,
            toolConfig = toolConfig,
            systemInstruction = content { text(systemPrompt) }
        )
        return model.startChat(history = history)
    }

    fun streamConciergeMessage(
        chat: Chat,
        message: String
    ): Flow<GroundedChatChunk> = flow {
        chat.sendMessageStream(message).collect { chunk ->
            val metadata = chunk.candidates.firstOrNull()?.groundingMetadata
            val sources = metadata?.groundingChunks?.mapNotNull { gc ->
                val web = gc.web
                val maps = gc.maps
                when {
                    maps != null -> GroundingSource(
                        title = maps.title ?: "Google Maps Place",
                        uri = maps.uri ?: "",
                        isGoogleMaps = true,
                        placeId = maps.placeId ?: ""
                    )
                    web != null -> GroundingSource(
                        title = web.title ?: web.uri ?: "Web Source",
                        uri = web.uri ?: "",
                        isGoogleMaps = false
                    )
                    else -> null
                }
            }.orEmpty()

            val searchHtml = metadata?.searchEntryPoint?.renderedContent
            emit(
                GroundedChatChunk(
                    textDelta = chunk.text.orEmpty(),
                    sources = sources,
                    searchEntryPointHtml = searchHtml
                )
            )
        }
    }

    suspend fun discoverPlacesWithGoogleMaps(
        query: String,
        cityName: String,
        lat: Double,
        lon: Double
    ): MapsDiscoveryResult {
        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleMaps()),
            toolConfig = ToolConfig(
                retrievalConfig = retrievalConfig {
                    latLng = LatLng(latitude = lat, longitude = lon)
                    languageCode = "en-US"
                }
            )
        )
        try {
            val response = withTimeout(60_000L) {
                model.generateContent("Find top-rated real places in $cityName, Japan for: $query. Include brief transit notes.")
            }
            val metadata = response.candidates.firstOrNull()?.groundingMetadata
            val places = metadata?.groundingChunks?.mapNotNull { gc ->
                gc.maps?.let { m ->
                    GroundingSource(
                        title = m.title ?: "Google Maps Location",
                        uri = m.uri ?: "",
                        isGoogleMaps = true,
                        placeId = m.placeId ?: ""
                    )
                }
            }.orEmpty()
            return MapsDiscoveryResult(
                summary = response.text ?: "No place summary returned.",
                places = places
            )
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Google Maps grounding timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google Maps discovery failed", e)
            throw e
        }
    }

    suspend fun transcribeAudioClip(audioBytes: ByteArray, mimeType: String = "audio/mp4"): AudioTranscriptionResult {
        try {
            val response = withTimeout(60_000L) {
                transcriptionModel.generateContent(
                    content {
                        inlineData(audioBytes, mimeType)
                        text(
                            "Transcribe this traveler audio clip accurately. If Japanese is spoken, provide the Japanese script, reading/romaji, English meaning, and politeness/grammar nuance. If English is spoken, provide both the English transcript and natural Japanese traveler phrasing."
                        )
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty transcription response.")
            val json = JSONObject(jsonText)
            return AudioTranscriptionResult(
                detectedLanguage = json.optString("detectedLanguage", "Japanese"),
                japaneseTranscript = json.optString("japaneseTranscript"),
                readingOrRomaji = json.optString("readingOrRomaji"),
                englishTranslation = json.optString("englishTranslation"),
                nuanceNote = json.optString("nuanceNote")
            )
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Audio transcription timed out. Please try a shorter clip.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            throw e
        }
    }

    suspend fun directPhotoMotionReel(
        bitmap: Bitmap,
        motionPreset: String,
        aspectLabel: String
    ): MotionReelDirection {
        try {
            val response = withTimeout(60_000L) {
                reelDirectorModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "Create a poetic Japanese travel motion-reel title card for this photo ($aspectLabel, camera motion '$motionPreset'). Keep japaneseTitle short and evocative."
                        )
                    }
                )
            }
            val json = JSONObject(response.text ?: "{}")
            return MotionReelDirection(
                japaneseTitle = json.optString("japaneseTitle", "日本の旅"),
                englishSubtitle = json.optString("englishSubtitle", "Moments across Japan"),
                haikuCaption = json.optString("haikuCaption", "Lantern light and quiet streets linger in memory.")
            )
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Photo analysis timed out.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Photo motion direction failed", e)
            throw e
        }
    }

    suspend fun restylePhotoWithNanoBanana(bitmap: Bitmap, styleName: String): Bitmap {
        try {
            val response = withTimeout(60_000L) {
                nanoBananaRestyleModel.generateContent(
                    content {
                        image(bitmap)
                        text("Transform this Japan travel photo into a luminous $styleName style atmospheric frame while preserving the scene's composition and landmarks.")
                    }
                )
            }
            val imgPart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            return imgPart?.image ?: throw IllegalStateException("No restyled image returned.")
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Image restyling timed out after 60s.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Nano Banana restyle failed", e)
            throw e
        }
    }

    @OptIn(PublicPreviewAPI::class)
    suspend fun startLiveVoiceSession(
        onTranscriptUpdate: (speaker: String, text: String) -> Unit
    ): LiveSession {
        val liveModel = Firebase.ai.liveModel(
            modelName = "gemini-3.1-flash-live-preview",
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            systemInstruction = content {
                text(
                    "You are TabiSensei, a warm bilingual Japanese conversation partner and travel guide. " +
                        "Speak naturally in polite Japanese (or English when asked), keep replies concise and conversational, " +
                        "and gently help the traveler practice real-life Japan scenarios."
                )
            }
        )
        val session = liveModel.connect()
        session.startAudioConversation(
            functionCallHandler = null,
            transcriptHandler = { input: Transcription?, output: Transcription? ->
                input?.text?.takeIf { it.isNotBlank() }?.let { onTranscriptUpdate("You", it) }
                output?.text?.takeIf { it.isNotBlank() }?.let { onTranscriptUpdate("TabiSensei", it) }
            }
        )
        return session
    }

    suspend fun translateWithNuance(
        inputText: String,
        sourceLang: String,
        targetLang: String,
        registerPreference: String
    ): NuanceTranslationResult {
        val prompt = """
            You are an expert Japanese-English linguist and cultural guide for travelers in Japan.
            Translate the following text from $sourceLang to $targetLang with desired register "$registerPreference".
            Handle complex grammatical nuances accurately: explain particle usage (は vs が, に vs で, を),
            verb conjugation nuances (e.g., 〜ておく, 〜てしまう, 〜ていただく, 〜ながら, passive/causative),
            keigo/humble/honorific tone, implied subjects, and cultural subtext.
            Input: "$inputText"
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                translatorModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty translation response from Gemini.")
            val json = JSONObject(jsonText)
            return NuanceTranslationResult(
                translatedText = json.optString("translatedText"),
                reading = json.optString("reading"),
                romaji = json.optString("romaji"),
                formalityLevel = json.optString("formalityLevel"),
                grammaticalNuances = json.optString("grammaticalNuances"),
                culturalContext = json.optString("culturalContext"),
                politeAlternative = json.optString("politeAlternative"),
                casualAlternative = json.optString("casualAlternative")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            throw IllegalStateException("Translation timed out after 60 seconds. Check connectivity and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            throw e
        }
    }

    suspend fun evaluateSpeechAndRespond(
        scenarioTitle: String,
        targetPhraseHint: String,
        userSpokenJapanese: String,
        viaVoiceRecognition: Boolean
    ): ConversationFeedbackResult {
        val inputSource = if (viaVoiceRecognition) {
            "transcribed via Android Japanese voice recognition"
        } else {
            "typed by the traveler"
        }
        val prompt = """
            You are a friendly native Japanese conversation coach roleplaying the scenario: "$scenarioTitle".
            Expected / helpful target phrase context: "$targetPhraseHint".
            The traveler responded ($inputSource): "$userSpokenJapanese".
            1. Reply naturally in character in Japanese (polite travel-appropriate Japanese), plus romaji and English.
            2. Evaluate their Japanese response: check grammar, particles, politeness level, and pronunciation/pitch-accent tips (especially noting homophones or mora timing if from speech recognition).
            3. Assign a realistic grammarScore (0-100).
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                conversationModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty conversation response from Gemini.")
            val json = JSONObject(jsonText)
            return ConversationFeedbackResult(
                japaneseReply = json.optString("japaneseReply"),
                romajiReply = json.optString("romajiReply"),
                englishMeaning = json.optString("englishMeaning"),
                pronunciationFeedback = json.optString("pronunciationFeedback"),
                grammarScore = json.optInt("grammarScore", 80).coerceIn(0, 100),
                naturalnessTip = json.optString("naturalnessTip")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Conversation feedback timed out", e)
            throw IllegalStateException("Conversation coach timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Conversation coach failed", e)
            throw e
        }
    }

    suspend fun generatePersonalizedItinerary(
        interests: List<String>,
        tripDays: Int,
        visitedRegions: List<String>,
        dailyBudgetJpy: Long
    ): PersonalizedPlanResult {
        val safeDays = tripDays.coerceIn(1, 7)
        val historyNote = if (visitedRegions.isEmpty()) {
            "First-time visitor to Japan"
        } else {
            "Previously visited: ${visitedRegions.joinToString(", ")}. Suggest fresh or deeper complementary regions!"
        }
        val prompt = """
            Design a personalized $safeDays-day Japan travel plan and destination recommendations.
            Traveler interests: ${interests.joinToString(", ").ifBlank { "Food, Culture, Scenic Transit" }}.
            Travel history: $historyNote.
            Target daily budget: ¥$dailyBudgetJpy JPY.
            Provide 3 personalized destination recommendations explaining how they fit the traveler's past travel history and interests,
            and generate 3 well-paced activities per day (morning, afternoon, evening) for days 1..$safeDays.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                itineraryModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty itinerary response from Gemini.")
            val json = JSONObject(jsonText)

            val recsArray = json.optJSONArray("recommendations")
            val recs = buildList {
                if (recsArray != null) {
                    for (i in 0 until recsArray.length()) {
                        val obj = recsArray.getJSONObject(i)
                        add(
                            DestinationRecommendation(
                                regionName = obj.optString("regionName"),
                                japaneseName = obj.optString("japaneseName"),
                                matchReason = obj.optString("matchReason"),
                                bestSeasonOrTip = obj.optString("bestSeasonOrTip")
                            )
                        )
                    }
                }
            }

            val actsArray = json.optJSONArray("activities")
            val acts = buildList {
                if (actsArray != null) {
                    for (i in 0 until actsArray.length()) {
                        val obj = actsArray.getJSONObject(i)
                        add(
                            GeneratedItineraryActivity(
                                dayNumber = obj.optLong("dayNumber", 1L).coerceIn(1L, safeDays.toLong()),
                                timeSlot = obj.optString("timeSlot", "09:30"),
                                title = obj.optString("title"),
                                city = obj.optString("city", "Tokyo"),
                                category = obj.optString("category", "Culture"),
                                notes = obj.optString("notes"),
                                estimatedCostJpy = obj.optLong("estimatedCostJpy", 1500L).coerceAtLeast(0L)
                            )
                        )
                    }
                }
            }

            return PersonalizedPlanResult(recommendations = recs, activities = acts)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Itinerary generation timed out", e)
            throw IllegalStateException("Itinerary generation timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Itinerary generation failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "GeminiAiRepo"
    }
}
