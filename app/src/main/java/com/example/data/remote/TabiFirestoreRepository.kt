package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.BudgetExpense
import com.example.data.model.EncryptedTravelDoc
import com.example.data.model.FlightAlert
import com.example.data.model.ItineraryItem
import com.example.data.model.LessonProgress
import com.example.data.model.TravelerPost
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TabiFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleSnapshotErrors(path: String, uid: String? = null): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && (uid == null || auth.currentUser?.uid == uid)) {
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
            }
            throw error
        }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .handleSnapshotErrors(path, userId)
        )
    }

    suspend fun ensureUserProfile(defaultName: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to defaultName.ifBlank { "Traveler" }.take(60),
                    "homeCity" to "Tokyo",
                    "interests" to listOf("Food & Ramen", "Temples & Shrines", "Local Culture"),
                    "visitedRegions" to emptyList<String>(),
                    "dailyBudgetJpy" to 18000L,
                    "mfaEnabled" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserProfileFields(fields: Map<String, Any>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = fields.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Itineraries ---
    fun observeItineraries(userId: String): Flow<List<ItineraryItem>> = flow {
        val path = "users/$userId/itineraries"
        emitAll(
            db.collection("users").document(userId).collection("itineraries")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ItineraryItem::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy({ it.dayNumber }, { it.timeSlot }))
                }
                .handleSnapshotErrors(path, userId)
        )
    }

    suspend fun addItineraryItem(item: ItineraryItem): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("itineraries").document()
        val payload = mapOf(
            "userId" to uid,
            "dayNumber" to item.dayNumber.coerceIn(1L, 30L),
            "timeSlot" to item.timeSlot.ifBlank { "09:00" }.take(30),
            "title" to item.title.take(120),
            "city" to item.city.ifBlank { "Tokyo" }.take(60),
            "category" to item.category.ifBlank { "Culture" }.take(40),
            "notes" to item.notes.take(500),
            "estimatedCostJpy" to item.estimatedCostJpy.coerceAtLeast(0L),
            "completed" to item.completed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleItineraryCompleted(item: ItineraryItem) {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "Itinerary ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("itineraries").document(item.id)
        try {
            docRef.update(
                mapOf(
                    "completed" to !item.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteItineraryItem(itemId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("itineraries").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Budget Expenses ---
    fun observeExpenses(userId: String): Flow<List<BudgetExpense>> = flow {
        val path = "users/$userId/expenses"
        emitAll(
            db.collection("users").document(userId).collection("expenses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BudgetExpense::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotErrors(path, userId)
        )
    }

    suspend fun addExpense(expense: BudgetExpense): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to expense.title.take(120),
            "amountJpy" to expense.amountJpy.coerceAtLeast(1L),
            "category" to expense.category.ifBlank { "Food" }.take(40),
            "dayNumber" to expense.dayNumber.coerceIn(1L, 30L),
            "notes" to expense.notes.take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Encrypted Travel Documents ---
    fun observeEncryptedDocuments(userId: String): Flow<List<EncryptedTravelDoc>> = flow {
        val path = "users/$userId/documents"
        emitAll(
            db.collection("users").document(userId).collection("documents")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(EncryptedTravelDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotErrors(path, userId)
        )
    }

    suspend fun saveEncryptedDocument(doc: EncryptedTravelDoc): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("documents").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to doc.title.take(100),
            "docType" to doc.docType.ifBlank { "Passport" }.take(40),
            "encryptedPayload" to doc.encryptedPayload,
            "ivBase64" to doc.ivBase64,
            "saltBase64" to doc.saltBase64,
            "keyMode" to doc.keyMode,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteEncryptedDocument(docId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("documents").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Lesson Progress ---
    fun observeLessonProgress(userId: String): Flow<List<LessonProgress>> = flow {
        val path = "users/$userId/lesson_progress"
        emitAll(
            db.collection("users").document(userId).collection("lesson_progress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LessonProgress::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(path, userId)
        )
    }

    suspend fun saveLessonProgress(lessonId: String, completed: Boolean, score: Long, masteredPhrases: Long) {
        val uid = requireUserId()
        val safeId = lessonId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)
        val docRef = db.collection("users").document(uid).collection("lesson_progress").document(safeId)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "completed" to completed,
                        "score" to score.coerceIn(0L, 100L),
                        "masteredPhrases" to masteredPhrases.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "lessonId" to safeId,
                        "completed" to completed,
                        "score" to score.coerceIn(0L, 100L),
                        "masteredPhrases" to masteredPhrases.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Flight & Itinerary Alerts ---
    fun observeFlightAlerts(userId: String): Flow<List<FlightAlert>> = flow {
        val path = "users/$userId/alerts"
        emitAll(
            db.collection("users").document(userId).collection("alerts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FlightAlert::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.triggerEpochMillis }
                }
                .handleSnapshotErrors(path, userId)
        )
    }

    suspend fun addFlightAlert(alert: FlightAlert): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("alerts").document()
        val payload = mapOf(
            "userId" to uid,
            "flightNumber" to alert.flightNumber.ifBlank { "JL001" }.take(20),
            "title" to alert.title.take(120),
            "departureTimeText" to alert.departureTimeText.ifBlank { "Upcoming" }.take(60),
            "triggerEpochMillis" to alert.triggerEpochMillis.coerceAtLeast(0L),
            "gateOrTerminal" to alert.gateOrTerminal.take(60),
            "status" to alert.status.ifBlank { "Scheduled" }.take(40),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFlightAlert(alertId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("alerts").document(alertId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Traveler Community Posts ---
    fun observeTravelerPosts(): Flow<List<TravelerPost>> = flow {
        val path = "traveler_posts"
        emitAll(
            db.collection("traveler_posts")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TravelerPost::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotErrors(path)
        )
    }

    suspend fun createTravelerPost(
        authorName: String,
        city: String,
        topic: String,
        message: String,
        meetupTime: String
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("traveler_posts").document()
        val payload = mapOf(
            "authorId" to uid,
            "authorName" to authorName.ifBlank { "Traveler" }.take(60),
            "city" to city.ifBlank { "Tokyo - Shibuya" }.take(50),
            "topic" to topic.ifBlank { "Meetup" }.take(40),
            "message" to message.take(500),
            "meetupTime" to meetupTime.take(60),
            "visibility" to "public",
            "interestedUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun togglePostRsvp(post: TravelerPost) {
        val uid = requireUserId()
        require(post.id.isNotBlank()) { "Post ID must not be blank" }
        val docRef = db.collection("traveler_posts").document(post.id)
        val updatedList = if (uid in post.interestedUids) {
            post.interestedUids - uid
        } else {
            (post.interestedUids + uid).distinct().take(50)
        }
        try {
            docRef.update(
                mapOf(
                    "interestedUids" to updatedList,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTravelerPost(postId: String) {
        requireUserId()
        val docRef = db.collection("traveler_posts").document(postId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
