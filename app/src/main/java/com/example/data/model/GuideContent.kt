package com.example.data.model

data class JapanesePhrase(
    val id: String,
    val japanese: String,
    val reading: String,
    val romaji: String,
    val english: String,
    val nuanceNote: String,
    val writingTargetChar: String = ""
)

data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class LearningModule(
    val id: String,
    val level: String, // "Basic Survival", "Transit & Dining", "Keigo & Politeness", "Advanced Nuances (N2/N1)"
    val title: String,
    val japaneseTitle: String,
    val summary: String,
    val phrases: List<JapanesePhrase>,
    val quiz: List<QuizQuestion>,
    val conversationScenario: String,
    val conversationPrompt: String
)

data class CulturalInsight(
    val id: String,
    val title: String,
    val japaneseTitle: String,
    val category: String,
    val doList: List<String>,
    val dontList: List<String>,
    val insiderTip: String
)

data class MapPoi(
    val id: String,
    val regionId: String,
    val name: String,
    val japaneseName: String,
    val category: String, // "Landmark", "Station", "Hospital", "Police", "Food"
    val normX: Float, // 0f..1f on offline vector canvas
    val normY: Float, // 0f..1f on offline vector canvas
    val description: String,
    val transitNote: String
)

data class TransitLine(
    val name: String,
    val colorHex: Long,
    val points: List<Pair<Float, Float>>
)

data class OfflineCityMapData(
    val regionId: String,
    val name: String,
    val japaneseName: String,
    val description: String,
    val sizeMb: Double,
    val latCenter: Double,
    val lonCenter: Double,
    val transitLines: List<TransitLine>,
    val pois: List<MapPoi>
)

data class EmergencyContact(
    val id: String,
    val title: String,
    val japaneseTitle: String,
    val number: String,
    val description: String,
    val availableLanguages: String,
    val isUrgent: Boolean
)

data class LocationEtiquetteTip(
    val zoneKey: String,
    val cityName: String,
    val districtName: String,
    val japaneseName: String,
    val briefTips: List<String>,
    val localPhraseJa: String,
    val localPhraseRomaji: String,
    val localPhraseEn: String,
    val categoryBadge: String,
    val lat: Double,
    val lon: Double,
    val radiusKm: Double,
    val keywords: List<String>,
    val sourceLabel: String = "Itinerary"
)

object GuideContent {

    val learningModules = listOf(
        LearningModule(
            id = "mod_basic_survival",
            level = "Basic Survival",
            title = "Essential Greetings & Requests",
            japaneseTitle = "基本の挨拶とお願い",
            summary = "Polite openers, thanking, apologizing, and getting help anywhere in Japan.",
            phrases = listOf(
                JapanesePhrase(
                    id = "p1",
                    japanese = "すみません、少しよろしいですか？",
                    reading = "すみません、すこしよろしいですか？",
                    romaji = "Sumimasen, sukoshi yoroshii desu ka?",
                    english = "Excuse me, do you have a brief moment?",
                    nuanceNote = "More refined than just 'Sumimasen!'—signals respect before asking a stranger or staff member for help.",
                    writingTargetChar = "あ"
                ),
                JapanesePhrase(
                    id = "p2",
                    japanese = "ありがとうございます",
                    reading = "ありがとうございます",
                    romaji = "Arigatou gozaimasu",
                    english = "Thank you very much",
                    nuanceNote = "Use 'Arigatou gozaimashita' (past tense) when leaving a shop or thanking someone for completed service.",
                    writingTargetChar = "あ"
                ),
                JapanesePhrase(
                    id = "p3",
                    japanese = "英語のメニューはありますか？",
                    reading = "えいごのめにゅーはありますか？",
                    romaji = "Eigo no menyuu wa arimasu ka?",
                    english = "Do you have an English menu?",
                    nuanceNote = "Soften with 'Sumimasen,' at the start for warmer reception.",
                    writingTargetChar = "か"
                )
            ),
            quiz = listOf(
                QuizQuestion(
                    prompt = "What do you say when leaving a restaurant after finishing your meal?",
                    options = listOf(
                        "ごちそうさまでした (Gochisousama deshita)",
                        "いただきます (Itadakimasu)",
                        "はじめまして (Hajimemashite)",
                        "おやすみなさい (Oyasuminasai)"
                    ),
                    correctIndex = 0,
                    explanation = "Gochisousama deshita expresses gratitude for the meal and chef's effort as you depart."
                ),
                QuizQuestion(
                    prompt = "Which phrase politely asks if someone has a quick moment?",
                    options = listOf(
                        "すみません、少しよろしいですか？",
                        "だめですか？",
                        "どこですか？",
                        "いくらですか？"
                    ),
                    correctIndex = 0,
                    explanation = "'Sukoshi yoroshii desu ka' gently asks for permission before taking someone's time."
                )
            ),
            conversationScenario = "Asking a station attendant for directions to the Shinkansen platform",
            conversationPrompt = "すみません、東京駅の新幹線乗り場はどこですか？"
        ),
        LearningModule(
            id = "mod_transit_dining",
            level = "Transit & Dining",
            title = "Ramen Counters & IC Transit",
            japaneseTitle = "食事と交通の会話",
            summary = "Ordering with dietary adjustments, reserving Shinkansen seats, and navigating gates.",
            phrases = listOf(
                JapanesePhrase(
                    id = "p4",
                    japanese = "これを二つお願いします",
                    reading = "これをふたつおねがいします",
                    romaji = "Kore o futatsu onegaishimasu",
                    english = "Two of these, please",
                    nuanceNote = "Japanese uses native counters (hitotsu, futatsu, mittsu) for general items up to ten.",
                    writingTargetChar = "を"
                ),
                JapanesePhrase(
                    id = "p5",
                    japanese = "お会計をお願いします",
                    reading = "おかいけいをおねがいします",
                    romaji = "Okaikei o onegaishimasu",
                    english = "Check, please",
                    nuanceNote = "At most casual spots in Japan, you take your bill slip to the register near the exit rather than paying at the table.",
                    writingTargetChar = "お"
                ),
                JapanesePhrase(
                    id = "p6",
                    japanese = "自由席と指定席はどちらですか？",
                    reading = "じゆうせきとしていせきはどちらですか？",
                    romaji = "Jiyuuseki to shiteiseki wa dochira desu ka?",
                    english = "Which cars are non-reserved vs. reserved seats?",
                    nuanceNote = "Essential on Shinkansen platforms—non-reserved (jiyuuseki) are usually cars 1–3.",
                    writingTargetChar = "駅"
                )
            ),
            quiz = listOf(
                QuizQuestion(
                    prompt = "What does 指定席 (shiteiseki) mean on a Shinkansen ticket?",
                    options = listOf("Reserved seat", "Non-reserved seat", "Green Car first class", "Luggage compartment"),
                    correctIndex = 0,
                    explanation = "指定席 (shiteiseki) is a reserved seat; 自由席 (jiyuuseki) is non-reserved."
                )
            ),
            conversationScenario = "Ordering dinner at a Tokyo ramen shop with a broth preference",
            conversationPrompt = "おすすめは何ですか？豚骨ラーメンを一つお願いします。"
        ),
        LearningModule(
            id = "mod_keigo_polite",
            level = "Keigo & Politeness",
            title = "Ryokan Hospitality & Polite Requests",
            japaneseTitle = "丁寧語と敬語の旅",
            summary = "Understand what hotel & ryokan staff say to you in Keigo and respond gracefully.",
            phrases = listOf(
                JapanesePhrase(
                    id = "p7",
                    japanese = "チェックインをお願いできますでしょうか？",
                    reading = "ちぇっくいんをおねがいできますでしょうか？",
                    romaji = "Chekkuin o onegai dekimasu deshou ka?",
                    english = "Could I possibly trouble you to check me in?",
                    nuanceNote = "Using 〜できますでしょうか shows elevated courtesy at traditional ryokans and luxury hotels.",
                    writingTargetChar = "様"
                ),
                JapanesePhrase(
                    id = "p8",
                    japanese = "かしこまりました",
                    reading = "かしこまりました",
                    romaji = "Kashikomarimashita",
                    english = "Certainly / Understood (humble honorific)",
                    nuanceNote = "You'll hear staff say this constantly—it is the polite honorific equivalent of 'I understand.'",
                    writingTargetChar = "礼"
                )
            ),
            quiz = listOf(
                QuizQuestion(
                    prompt = "When a ryokan attendant says '少々お待ちくださいませ', what are they asking?",
                    options = listOf(
                        "Please wait just a brief moment",
                        "Please remove your shoes",
                        "Breakfast is served at 8 AM",
                        "Your train has arrived"
                    ),
                    correctIndex = 0,
                    explanation = "'Shoushou omachi kudasaimase' is ultra-polite Keigo for 'Please wait a moment.'"
                )
            ),
            conversationScenario = "Checking into a traditional Kyoto ryokan and asking about onsen hours",
            conversationPrompt = "予約した者ですが、チェックインをお願いできますでしょうか？温泉は何時までですか？"
        ),
        LearningModule(
            id = "mod_advanced_nuance",
            level = "Advanced Nuances (N2/N1)",
            title = "Indirect Refusals, Softeners & Nuance",
            japaneseTitle = "上級表現とニュアンス",
            summary = "Decode subtle Japanese cues like ちょっと…, せっかくですが, and 〜ておく.",
            phrases = listOf(
                JapanesePhrase(
                    id = "p9",
                    japanese = "それはちょっと厳しいかもしれません",
                    reading = "それはちょっときびしいかもしれません",
                    romaji = "Sore wa chotto kibishii kamoshiremasen",
                    english = "That might be a bit difficult (polite refusal)",
                    nuanceNote = "In Japanese, 'chotto...' followed by a pause or 'kibishii' almost always means 'No, unfortunately that isn't possible' without blunt confrontation.",
                    writingTargetChar = "和"
                ),
                JapanesePhrase(
                    id = "p10",
                    japanese = "あらかじめ予約しておきました",
                    reading = "あらかじめよやくしておきました",
                    romaji = "Arakajime yoyaku shite okimashita",
                    english = "I booked it in advance (for future readiness)",
                    nuanceNote = "The auxiliary 〜ておく expresses doing an action intentionally beforehand in preparation for later.",
                    writingTargetChar = "心"
                )
            ),
            quiz = listOf(
                QuizQuestion(
                    prompt = "If a shopkeeper says '今日のお席はちょっと…' (Today's seats are a little...), what does it mean?",
                    options = listOf(
                        "They are completely full and cannot seat you",
                        "The seats are slightly small",
                        "Wait 5 minutes and a table will open",
                        "The menu is shorter today"
                    ),
                    correctIndex = 0,
                    explanation = "Trailing off with 'chotto...' is the classic polite Japanese indirect refusal."
                )
            ),
            conversationScenario = "Politely negotiating a late checkout or luggage storage using nuanced Japanese",
            conversationPrompt = "チェックアウト後も荷物を預かっておいていただけますでしょうか？"
        )
    )

    val culturalInsights = listOf(
        CulturalInsight(
            id = "cult_onsen",
            title = "Onsen & Sento Bathing Etiquette",
            japaneseTitle = "温泉のマナー",
            category = "Wellness & Tradition",
            doList = listOf(
                "Wash and rinse thoroughly at the seated shower stations BEFORE entering the hot spring pool.",
                "Tie long hair up so it never touches the bathwater.",
                "Place your small modesty towel on top of your head or poolside—never dip it in the bath."
            ),
            dontList = listOf(
                "Do not wear swimsuits in traditional communal onsen.",
                "Do not bring smartphones or cameras into changing rooms or bathing areas.",
                "Check tattoo policies in advance; ask for tattoo-friendly cover stickers or private kashikiri baths."
            ),
            insiderTip = "Look for '貸切風呂' (kashikiri-buro) at ryokans if you want a private family bath reserved by the hour."
        ),
        CulturalInsight(
            id = "cult_shrine",
            title = "Shrines (Jinja) vs. Temples (Otera)",
            japaneseTitle = "神社とお寺の参拝",
            category = "Sacred Spaces",
            doList = listOf(
                "At Shinto shrines (Torii gate), bow once before entering and walk along the edges of the path—the center is for the kami.",
                "Purify left hand, right hand, then rinse mouth with cupped hand at the Temizuya water pavilion.",
                "Shrine ritual: Toss 5-yen coin gently, bow twice deeply, clap twice, pray silently, bow once."
            ),
            dontList = listOf(
                "Do NOT clap at Buddhist temples—bow quietly with palms pressed together instead.",
                "Do not drink directly from the wooden ladle at the purification fountain."
            ),
            insiderTip = "Bring a Goshuincho (stamp book) to receive hand-brushed vermilion calligraphy seals at temples and shrines."
        ),
        CulturalInsight(
            id = "cult_trains",
            title = "Trains, Shinkansen & Silent Cars",
            japaneseTitle = "電車の乗り方",
            category = "Transit",
            doList = listOf(
                "Queue neatly along painted floor lines and wait for passengers to exit before boarding.",
                "Set phone to silent ('Manner Mode') and wear backpacks on your front in crowded cars.",
                "Reserve oversized baggage seats on Tokaido-Sanyo-Kyushu Shinkansen if total luggage dimensions exceed 160 cm."
            ),
            dontList = listOf(
                "Avoid voice calls inside train carriages—step to the deck vestibule between Shinkansen cars.",
                "Avoid eating on local commuter trains (eating bento on long-distance Shinkansen is welcome!)."
            ),
            insiderTip = "Use Apple/Google Pay Suica or ICOCA for seamless tap-and-go across metros, buses, and konbini."
        ),
        CulturalInsight(
            id = "cult_dining",
            title = "Izakaya, Otoshi & Chopstick Rules",
            japaneseTitle = "居酒屋と食事のマナー",
            category = "Dining",
            doList = listOf(
                "Say 'Sumimasen!' clearly or press the table call button when ready to order.",
                "Use the opposite (thick) ends of communal chopsticks when serving shared plates if tongs aren't provided.",
                "Tipping is not practiced—exceptional service is already built into the price."
            ),
            dontList = listOf(
                "Never stick chopsticks upright into a bowl of rice (resembles funeral incense).",
                "Never pass food directly from chopstick to chopstick."
            ),
            insiderTip = "Many izakayas charge a small ¥300–¥600 'otoshi' appetizer seat charge—it acts as a table cover."
        )
    )

    val offlineCityMaps = listOf(
        OfflineCityMapData(
            regionId = "tokyo",
            name = "Tokyo",
            japaneseName = "東京",
            description = "Yamanote loop, Shibuya, Shinjuku, Asakusa & emergency hubs.",
            sizeMb = 18.4,
            latCenter = 35.6762,
            lonCenter = 139.6503,
            transitLines = listOf(
                TransitLine(
                    "JR Yamanote Loop",
                    0xFF68B82E,
                    listOf(0.22f to 0.35f, 0.20f to 0.62f, 0.45f to 0.78f, 0.62f to 0.50f, 0.56f to 0.22f, 0.22f to 0.35f)
                ),
                TransitLine(
                    "Tokyo Metro Ginza Line",
                    0xFFF39700,
                    listOf(0.20f to 0.62f, 0.42f to 0.56f, 0.60f to 0.52f, 0.78f to 0.24f)
                )
            ),
            pois = listOf(
                MapPoi("t_shinjuku", "tokyo", "Shinjuku Station", "新宿駅", "Station", 0.22f, 0.35f, "World's busiest rail hub with Narita Express & Chuo Line.", "JR / Odakyu / Metro"),
                MapPoi("t_shibuya", "tokyo", "Shibuya Scramble", "渋谷交差点", "Landmark", 0.20f, 0.62f, "Iconic pedestrian crossing and Shibuya Sky observation deck.", "Yamanote & Ginza Line"),
                MapPoi("t_tokyo_sta", "tokyo", "Tokyo Station", "東京駅", "Station", 0.62f, 0.50f, "Main Shinkansen gateway to Kyoto, Osaka & Hokkaido.", "Tokaido / Tohoku Shinkansen"),
                MapPoi("t_sensoji", "tokyo", "Senso-ji Temple", "浅草寺", "Landmark", 0.78f, 0.24f, "Tokyo's oldest Buddhist temple in historic Asakusa.", "Ginza Line Asakusa Exit 1"),
                MapPoi("t_st_lukes", "tokyo", "St. Luke's Int'l Hospital", "聖路加国際病院", "Hospital", 0.68f, 0.64f, "English-speaking emergency medical center near Tsukiji.", "Hibiya Line Tsukiji Sta."),
                MapPoi("t_koban", "tokyo", "Ginza 4-Chome Koban", "銀座四丁目交番", "Police", 0.58f, 0.58f, "24h neighborhood police box for lost items & directions.", "Dial 110 in emergency"),
                MapPoi("t_tsukiji", "tokyo", "Tsukiji Outer Market", "築地場外市場", "Food", 0.64f, 0.68f, "Morning sushi, tamagoyaki, and artisan knives.", "Oedo Line Tsukijishijo")
            )
        ),
        OfflineCityMapData(
            regionId = "kyoto",
            name = "Kyoto",
            japaneseName = "京都",
            description = "Gion, Arashiyama, Fushimi Inari, Karasuma subway & medical support.",
            sizeMb = 14.2,
            latCenter = 35.0116,
            lonCenter = 135.7681,
            transitLines = listOf(
                TransitLine(
                    "Karasuma Subway Line",
                    0xFF2E9E5B,
                    listOf(0.50f to 0.18f, 0.50f to 0.48f, 0.50f to 0.72f)
                ),
                TransitLine(
                    "JR Sagano / Nara Line",
                    0xFF7B4B94,
                    listOf(0.16f to 0.36f, 0.50f to 0.72f, 0.62f to 0.88f)
                )
            ),
            pois = listOf(
                MapPoi("k_kyoto_sta", "kyoto", "Kyoto Station", "京都駅", "Station", 0.50f, 0.72f, "Shinkansen, Haruka Airport Express & central bus terminal.", "JR / Kintetsu / Subway"),
                MapPoi("k_gion", "kyoto", "Gion & Hanamikoji", "祇園", "Landmark", 0.74f, 0.48f, "Historic geiko district with wooden machiya teahouses.", "Keihan Gion-Shijo Sta."),
                MapPoi("k_arashiyama", "kyoto", "Arashiyama Bamboo Grove", "嵐山竹林", "Landmark", 0.16f, 0.36f, "Towering bamboo paths and Tenryu-ji Zen garden.", "JR Saga-Arashiyama"),
                MapPoi("k_fushimi", "kyoto", "Fushimi Inari Taisha", "伏見稲荷大社", "Landmark", 0.62f, 0.88f, "Thousands of vermilion Senbon Torii gates up Mt. Inari.", "JR Inari Sta. (2 stops)"),
                MapPoi("k_redcross", "kyoto", "Kyoto Daiichi Red Cross", "京都第一赤十字病院", "Hospital", 0.58f, 0.78f, "Major emergency hospital with multilingual support desk.", "Keihan Tofukuji Sta."),
                MapPoi("k_nishiki", "kyoto", "Nishiki Food Market", "錦市場", "Food", 0.56f, 0.44f, "Kyoto's 400-year-old 'Kitchen' for pickles, matcha & yuba.", "Karasuma Shijo Sta.")
            )
        ),
        OfflineCityMapData(
            regionId = "osaka",
            name = "Osaka",
            japaneseName = "大阪",
            description = "Umeda, Dotonbori, Osaka Castle, Shin-Osaka & Kansai access.",
            sizeMb = 12.8,
            latCenter = 34.6937,
            lonCenter = 135.5023,
            transitLines = listOf(
                TransitLine(
                    "Midosuji Subway Line",
                    0xFFE5171F,
                    listOf(0.48f to 0.16f, 0.48f to 0.36f, 0.48f to 0.66f, 0.48f to 0.84f)
                )
            ),
            pois = listOf(
                MapPoi("o_shinosaka", "osaka", "Shin-Osaka Station", "新大阪駅", "Station", 0.48f, 0.16f, "Shinkansen terminal for Kyoto, Tokyo & Hiroshima.", "Midosuji Line / JR"),
                MapPoi("o_umeda", "osaka", "Umeda Sky & Osaka Sta.", "梅田・大阪駅", "Station", 0.48f, 0.36f, "Northern commercial core and Floating Garden Observatory.", "JR Osaka Loop"),
                MapPoi("o_castle", "osaka", "Osaka Castle Park", "大阪城公園", "Landmark", 0.76f, 0.44f, "16th-century castle surrounded by stone moats & plum groves.", "Osakajokoen Sta."),
                MapPoi("o_dotonbori", "osaka", "Dotonbori Canal", "道頓堀", "Food", 0.48f, 0.66f, "Neon-lit street food capital for takoyaki & okonomiyaki.", "Namba Sta. Exit 14"),
                MapPoi("o_hospital", "osaka", "Osaka General Medical Ctr", "大阪急性期・総合医療センター", "Hospital", 0.56f, 0.82f, "24-hour tertiary emergency and trauma center.", "Midosuji Nagai area")
            )
        )
    )

    val emergencyContacts = listOf(
        EmergencyContact(
            id = "em_police",
            title = "Police Emergency",
            japaneseTitle = "警察（110番）",
            number = "110",
            description = "Crime, theft, traffic accidents, or urgent safety threats anywhere in Japan.",
            availableLanguages = "Japanese + English interpreter routing",
            isUrgent = true
        ),
        EmergencyContact(
            id = "em_ambulance",
            title = "Ambulance & Fire",
            japaneseTitle = "救急・消防（119番）",
            number = "119",
            description = "Life-threatening medical emergencies, severe injury, or fire.",
            availableLanguages = "Japanese + 3-way translation in major cities",
            isUrgent = true
        ),
        EmergencyContact(
            id = "em_7119",
            title = "Tokyo Medical Advice",
            japaneseTitle = "東京消防庁救急相談センター",
            number = "#7119",
            description = "24/7 nurse & doctor triage line when unsure whether to call an ambulance.",
            availableLanguages = "Japanese (multilingual via AMDA below)",
            isUrgent = false
        ),
        EmergencyContact(
            id = "em_jnto",
            title = "JNTO 24/7 Visitor Hotline",
            japaneseTitle = "日本政府観光局ヘルプライン",
            number = "050-3816-2787",
            description = "Official Japan National Tourism Organization 24-hour emergency & disaster hotline.",
            availableLanguages = "English, Chinese, Korean, Japanese",
            isUrgent = false
        ),
        EmergencyContact(
            id = "em_amda",
            title = "AMDA Medical Info Center",
            japaneseTitle = "AMDA国際医療情報センター",
            number = "03-6233-9266",
            description = "Matches international travelers with English-speaking clinics & hospitals.",
            availableLanguages = "English, Spanish, Thai, Chinese, Korean",
            isUrgent = false
        ),
        EmergencyContact(
            id = "em_helpline",
            title = "Japan Helpline (24h)",
            japaneseTitle = "ジャパン・ヘルプライン",
            number = "0570-000-911",
            description = "Nationwide nonprofit crisis, lost passport, and urgent traveler assistance.",
            availableLanguages = "English 24/7",
            isUrgent = false
        )
    )

    val interestOptions = listOf(
        "Food & Ramen",
        "Temples & Shrines",
        "Pop Culture & Anime",
        "Nature & Onsen",
        "Art & Architecture",
        "Nightlife & Izakaya",
        "Shopping & Crafts"
    )

    val japanRegions = listOf(
        "Tokyo",
        "Kyoto",
        "Osaka",
        "Hakone",
        "Nara",
        "Hiroshima",
        "Hokkaido (Sapporo)",
        "Kanazawa",
        "Okinawa"
    )

    val locationEtiquetteTips = listOf(
        // District-level curated etiquette tips
        LocationEtiquetteTip(
            zoneKey = "kyoto_gion",
            cityName = "Kyoto",
            districtName = "Gion & Hanamikoji",
            japaneseName = "祇園・花見小路",
            briefTips = listOf(
                "Keep to main public roads; photographing private alleys or geiko/maiko without consent carries ¥10,000 fines.",
                "Lower voices near wooden machiya teahouses and step aside before pausing."
            ),
            localPhraseJa = "すみません、通ります",
            localPhraseRomaji = "Sumimasen, toorimasu",
            localPhraseEn = "Excuse me, passing through",
            categoryBadge = "Geiko District",
            lat = 35.0037,
            lon = 135.7785,
            radiusKm = 1.6,
            keywords = listOf("gion", "hanamikoji", "geiko", "maiko", "yasaka", "祇園")
        ),
        LocationEtiquetteTip(
            zoneKey = "kyoto_fushimi",
            cityName = "Kyoto",
            districtName = "Fushimi Inari",
            japaneseName = "伏見稲荷大社",
            briefTips = listOf(
                "Bow once at the first Torii and walk along the sides—the center path is reserved for the kami.",
                "Keep tripods packed on narrow mountain steps so hikers flow smoothly."
            ),
            localPhraseJa = "お参りさせていただきます",
            localPhraseRomaji = "Omairi sasete itadakimasu",
            localPhraseEn = "Humbly paying my respects",
            categoryBadge = "Sacred Shrine",
            lat = 34.9671,
            lon = 135.7727,
            radiusKm = 1.8,
            keywords = listOf("fushimi", "inari", "torii", "伏見稲荷")
        ),
        LocationEtiquetteTip(
            zoneKey = "kyoto_arashiyama",
            cityName = "Kyoto",
            districtName = "Arashiyama",
            japaneseName = "嵐山・竹林",
            briefTips = listOf(
                "Never touch or carve the living bamboo stalks, and keep conversations soft at dawn.",
                "Carry your own small trash pouch—temple garden trails have no public bins."
            ),
            localPhraseJa = "とても静かで美しいですね",
            localPhraseRomaji = "Totemo shizuka de utsukushii desu ne",
            localPhraseEn = "It is wonderfully peaceful here",
            categoryBadge = "Bamboo & Zen",
            lat = 35.0168,
            lon = 135.6713,
            radiusKm = 2.2,
            keywords = listOf("arashiyama", "bamboo", "tenryu", "sagano", "嵐山")
        ),
        LocationEtiquetteTip(
            zoneKey = "kyoto_nishiki",
            cityName = "Kyoto",
            districtName = "Nishiki Market",
            japaneseName = "錦市場",
            briefTips = listOf(
                "Avoid walking while eating (tabearuki)—finish skewers right beside the vendor stall.",
                "Check posted signs before tasting samples or taking close-up photos."
            ),
            localPhraseJa = "ここで食べてもいいですか？",
            localPhraseRomaji = "Koko de tabete mo ii desu ka?",
            localPhraseEn = "May I eat right here?",
            categoryBadge = "Market Etiquette",
            lat = 35.0050,
            lon = 135.7649,
            radiusKm = 0.9,
            keywords = listOf("nishiki", "market", "錦市場")
        ),
        LocationEtiquetteTip(
            zoneKey = "tokyo_asakusa",
            cityName = "Tokyo",
            districtName = "Asakusa & Senso-ji",
            japaneseName = "浅草・浅草寺",
            briefTips = listOf(
                "Senso-ji is a Buddhist temple: bow quietly with palms together at the hall (do NOT clap).",
                "Enjoy Nakamise-dori snacks beside the shop counter rather than while walking."
            ),
            localPhraseJa = "ここでいただいてもよろしいですか？",
            localPhraseRomaji = "Koko de itadaite mo yoroshii desu ka?",
            localPhraseEn = "May I eat this right here?",
            categoryBadge = "Temple District",
            lat = 35.7148,
            lon = 139.7967,
            radiusKm = 1.8,
            keywords = listOf("asakusa", "senso", "sensoji", "nakamise", "浅草")
        ),
        LocationEtiquetteTip(
            zoneKey = "tokyo_shibuya",
            cityName = "Tokyo",
            districtName = "Shibuya & Harajuku",
            japaneseName = "渋谷・原宿",
            briefTips = listOf(
                "Keep moving smoothly across Shibuya Scramble—shoot photos from elevated station terraces.",
                "Street drinking around Shibuya Station is prohibited after 6 PM."
            ),
            localPhraseJa = "すみません、通してください",
            localPhraseRomaji = "Sumimasen, tooshite kudasai",
            localPhraseEn = "Excuse me, coming through",
            categoryBadge = "Night & Crossing",
            lat = 35.6595,
            lon = 139.7004,
            radiusKm = 2.0,
            keywords = listOf("shibuya", "scramble", "harajuku", "meiji", "takeshita", "渋谷", "原宿")
        ),
        LocationEtiquetteTip(
            zoneKey = "tokyo_shinjuku",
            cityName = "Tokyo",
            districtName = "Shinjuku",
            japaneseName = "新宿",
            briefTips = listOf(
                "Stand on the LEFT on Tokyo escalators and wear backpacks on your front at Shinjuku Station.",
                "At tiny Golden Gai bars, check the posted seat charge (¥500–¥1,000) before sitting."
            ),
            localPhraseJa = "チャージはおいくらですか？",
            localPhraseRomaji = "Chaaji wa oikura desu ka?",
            localPhraseEn = "How much is the seating charge?",
            categoryBadge = "Transit & Bars",
            lat = 35.6938,
            lon = 139.7034,
            radiusKm = 2.0,
            keywords = listOf("shinjuku", "kabukicho", "golden gai", "omoide", "新宿")
        ),
        LocationEtiquetteTip(
            zoneKey = "tokyo_tsukiji",
            cityName = "Tokyo",
            districtName = "Tsukiji & Ginza",
            japaneseName = "築地・銀座",
            briefTips = listOf(
                "Finish skewered seafood at the stall's stand-up area instead of walking through lanes.",
                "Ask permission before photographing market fishmongers or knife artisans."
            ),
            localPhraseJa = "写真を撮ってもいいですか？",
            localPhraseRomaji = "Shashin o totte mo ii desu ka?",
            localPhraseEn = "May I take a photo?",
            categoryBadge = "Outer Market",
            lat = 35.6655,
            lon = 139.7707,
            radiusKm = 1.6,
            keywords = listOf("tsukiji", "ginza", "築地", "銀座", "sushi")
        ),
        LocationEtiquetteTip(
            zoneKey = "osaka_dotonbori",
            cityName = "Osaka",
            districtName = "Dotonbori & Namba",
            japaneseName = "道頓堀・難波",
            briefTips = listOf(
                "In Osaka, stand on the RIGHT side of escalators (opposite of Tokyo!).",
                "Eat street takoyaki near the stall and use their bin—street litter bins are scarce."
            ),
            localPhraseJa = "おおきに、ごちそうさん！",
            localPhraseRomaji = "Ookini, gochisousan!",
            localPhraseEn = "Thanks very much for the meal! (Osaka dialect)",
            categoryBadge = "Street Food",
            lat = 34.6687,
            lon = 135.5013,
            radiusKm = 2.0,
            keywords = listOf("dotonbori", "namba", "shinsaibashi", "takoyaki", "kuromon", "道頓堀")
        ),
        // City-level curated etiquette tips
        LocationEtiquetteTip(
            zoneKey = "city_tokyo",
            cityName = "Tokyo",
            districtName = "Tokyo Metro Area",
            japaneseName = "東京",
            briefTips = listOf(
                "Keep phones on Manner Mode on trains and stand on the LEFT side of escalators.",
                "Tipping isn't practiced—say 'Gochisousama deshita' when leaving restaurants."
            ),
            localPhraseJa = "ごちそうさまでした",
            localPhraseRomaji = "Gochisousama deshita",
            localPhraseEn = "Thank you for the wonderful meal",
            categoryBadge = "City Etiquette",
            lat = 35.6762,
            lon = 139.6503,
            radiusKm = 25.0,
            keywords = listOf("tokyo", "東京", "akihabara", "ueno", "roppongi", "tokyo station")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_kyoto",
            cityName = "Kyoto",
            districtName = "Kyoto City",
            japaneseName = "京都",
            briefTips = listOf(
                "Board city buses from the REAR door and tap your IC card/pay when exiting at the front.",
                "Remove shoes whenever stepping onto tatami mats and place them neatly facing the door."
            ),
            localPhraseJa = "お邪魔します",
            localPhraseRomaji = "Ojama shimasu",
            localPhraseEn = "Excuse me for intruding (entering indoors)",
            categoryBadge = "Heritage City",
            lat = 35.0116,
            lon = 135.7681,
            radiusKm = 20.0,
            keywords = listOf("kyoto", "京都", "kiyomizu", "kinkaku")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_osaka",
            cityName = "Osaka",
            districtName = "Osaka City",
            japaneseName = "大阪",
            briefTips = listOf(
                "Stand on the RIGHT on Osaka escalators; locals leave the left side open for walking.",
                "Small izakayas may include an 'otoshi' seat appetizer (¥300–¥500)—no tipping needed."
            ),
            localPhraseJa = "これ、おすすめは何ですか？",
            localPhraseRomaji = "Kore, osusume wa nan desu ka?",
            localPhraseEn = "What do you recommend here?",
            categoryBadge = "City Etiquette",
            lat = 34.6937,
            lon = 135.5023,
            radiusKm = 20.0,
            keywords = listOf("osaka", "大阪", "umeda", "shin-osaka")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_nara",
            cityName = "Nara",
            districtName = "Nara Park",
            japaneseName = "奈良",
            briefTips = listOf(
                "Bow gently when offering 'shika senbei' crackers to sacred deer, and show empty palms when out.",
                "Keep paper tickets and plastic bags zipped away so deer don't nibble them."
            ),
            localPhraseJa = "鹿せんべいをください",
            localPhraseRomaji = "Shika senbei o kudasai",
            localPhraseEn = "Deer crackers, please",
            categoryBadge = "Park & Shrines",
            lat = 34.6851,
            lon = 135.8430,
            radiusKm = 15.0,
            keywords = listOf("nara", "todai", "kasuga", "奈良")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_hakone",
            cityName = "Hakone",
            districtName = "Hakone Onsen",
            japaneseName = "箱根",
            briefTips = listOf(
                "Wash and rinse thoroughly at seated showers BEFORE entering any hot spring bath.",
                "Keep small towels out of the bathwater and tie long hair above the water."
            ),
            localPhraseJa = "いいお湯ですね",
            localPhraseRomaji = "Ii oyu desu ne",
            localPhraseEn = "What lovely hot spring water!",
            categoryBadge = "Onsen Etiquette",
            lat = 35.2324,
            lon = 139.1069,
            radiusKm = 15.0,
            keywords = listOf("hakone", "onsen", "ryokan", "箱根", "owakudani")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_hiroshima",
            cityName = "Hiroshima",
            districtName = "Hiroshima & Miyajima",
            japaneseName = "広島・宮島",
            briefTips = listOf(
                "Observe quiet, reflective silence near the Peace Memorial Cenotaph and Museum.",
                "On Miyajima, never feed the wild island deer—they forage naturally along the shore."
            ),
            localPhraseJa = "お好み焼きを一つお願いします",
            localPhraseRomaji = "Okonomiyaki o hitotsu onegaishimasu",
            localPhraseEn = "One Hiroshima okonomiyaki, please",
            categoryBadge = "Peace & Island",
            lat = 34.3853,
            lon = 132.4553,
            radiusKm = 25.0,
            keywords = listOf("hiroshima", "miyajima", "itsukushima", "広島")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_kanazawa",
            cityName = "Kanazawa",
            districtName = "Kanazawa & Higashi Chaya",
            japaneseName = "金沢",
            briefTips = listOf(
                "Protect wooden lattice exteriors in Higashi Chaya by not leaning on facades or using flash.",
                "Wait for gold-leaf soft-serve to be eaten stationary before stepping into craft boutiques."
            ),
            localPhraseJa = "拝見してもいいですか？",
            localPhraseRomaji = "Haiken shite mo ii desu ka?",
            localPhraseEn = "May I take a look?",
            categoryBadge = "Craft District",
            lat = 36.5613,
            lon = 136.6562,
            radiusKm = 15.0,
            keywords = listOf("kanazawa", "chaya", "kenroku", "金沢")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_sapporo",
            cityName = "Sapporo",
            districtName = "Sapporo & Hokkaido",
            japaneseName = "札幌・北海道",
            briefTips = listOf(
                "Brush off snow and stamp dry at indoor entry mats—heated interiors get warm quickly.",
                "In icy crossings, take small flat steps ('penguin walk') and watch for snowmelt roof edges."
            ),
            localPhraseJa = "とても寒いですね、暖かいお茶をください",
            localPhraseRomaji = "Totemo samui desu ne, atatakai ocha o kudasai",
            localPhraseEn = "It's chilly—hot tea, please",
            categoryBadge = "Northern Etiquette",
            lat = 43.0618,
            lon = 141.3545,
            radiusKm = 25.0,
            keywords = listOf("sapporo", "hokkaido", "otaru", "札幌", "北海道")
        ),
        LocationEtiquetteTip(
            zoneKey = "city_okinawa",
            cityName = "Okinawa",
            districtName = "Naha & Okinawa",
            japaneseName = "沖縄・那覇",
            briefTips = listOf(
                "Respect sacred 'Utaki' groves and Shuri heritage grounds—stay on marked stone walkways.",
                "Use reef-safe sunscreen and check tide flags before entering coastal reefs."
            ),
            localPhraseJa = "めんそーれ・にふぇーでーびる",
            localPhraseRomaji = "Mensoore / Nifee deebiru",
            localPhraseEn = "Welcome / Thank you (Okinawan)",
            categoryBadge = "Island Etiquette",
            lat = 26.2124,
            lon = 127.6809,
            radiusKm = 30.0,
            keywords = listOf("okinawa", "naha", "shuri", "沖縄", "那覇")
        )
    )

    fun resolveEtiquetteForLocation(
        city: String,
        districtOrTitle: String = "",
        notes: String = ""
    ): LocationEtiquetteTip {
        val combined = "$districtOrTitle $notes $city".lowercase()
        // Prefer specific district match first (smaller radiusKm = higher specificity)
        val districtMatch = locationEtiquetteTips
            .filter { it.radiusKm < 5.0 }
            .firstOrNull { tip -> tip.keywords.any { kw -> combined.contains(kw) } }
        if (districtMatch != null) return districtMatch

        // City-level match
        val cityLower = city.lowercase()
        val cityMatch = locationEtiquetteTips
            .firstOrNull { tip ->
                tip.cityName.equals(city, ignoreCase = true) ||
                    tip.keywords.any { kw -> cityLower.contains(kw) || combined.contains(kw) }
            }
        if (cityMatch != null) return cityMatch

        val cleanCity = city.trim().ifBlank { "Japan" }
        val cleanDistrict = districtOrTitle.trim().ifBlank { cleanCity }
        return LocationEtiquetteTip(
            zoneKey = "custom_${cleanCity.lowercase()}_${cleanDistrict.lowercase()}",
            cityName = cleanCity,
            districtName = cleanDistrict,
            japaneseName = "地域のマナー",
            briefTips = listOf(
                "Keep voices soft on public transit and wait for passengers to exit before boarding.",
                "Carry your own small trash pouch and say 'Sumimasen' gently before asking staff for help."
            ),
            localPhraseJa = "すみません、少しよろしいですか？",
            localPhraseRomaji = "Sumimasen, sukoshi yoroshii desu ka?",
            localPhraseEn = "Excuse me, do you have a brief moment?",
            categoryBadge = "Local Etiquette",
            lat = 35.6762,
            lon = 139.6503,
            radiusKm = 15.0,
            keywords = emptyList()
        )
    }

    fun resolveEtiquetteForCoordinates(lat: Double, lon: Double): LocationEtiquetteTip? {
        return locationEtiquetteTips
            .map { tip -> tip to haversineKm(lat, lon, tip.lat, tip.lon) }
            .filter { (tip, distKm) -> distKm <= tip.radiusKm }
            .minByOrNull { (tip, distKm) ->
                // Prefer tight district zones over wide city zones when both match
                if (tip.radiusKm < 5.0) distKm * 0.25 else distKm
            }
            ?.first
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return r * 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))
    }
}
