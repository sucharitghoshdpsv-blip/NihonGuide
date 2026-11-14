package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val homeCity: String = "Tokyo",
    val interests: List<String> = emptyList(),
    val visitedRegions: List<String> = emptyList(),
    val dailyBudgetJpy: Long = 18000L,
    val mfaEnabled: Boolean = false,
    val totpSecretEncrypted: String = "",
    val emergencyMedicalSummaryJa: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ItineraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dayNumber: Long = 1L,
    val timeSlot: String = "09:00",
    val title: String = "",
    val city: String = "Tokyo",
    val category: String = "Culture",
    val notes: String = "",
    val estimatedCostJpy: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BudgetExpense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amountJpy: Long = 0L,
    val category: String = "Food",
    val dayNumber: Long = 1L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EncryptedTravelDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val docType: String = "Passport",
    val encryptedPayload: String = "",
    val ivBase64: String = "",
    val saltBase64: String = "",
    val keyMode: String = "KEYSTORE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LessonProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lessonId: String = "",
    val completed: Boolean = false,
    val score: Long = 0L,
    val masteredPhrases: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FlightAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val flightNumber: String = "",
    val title: String = "",
    val departureTimeText: String = "",
    val triggerEpochMillis: Long = 0L,
    val gateOrTerminal: String = "",
    val status: String = "Scheduled",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TravelerPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val city: String = "Tokyo - Shibuya",
    val topic: String = "Meetup",
    val message: String = "",
    val meetupTime: String = "",
    val visibility: String = "public",
    val interestedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
