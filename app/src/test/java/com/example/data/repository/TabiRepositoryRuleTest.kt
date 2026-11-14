package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BudgetExpense
import com.example.data.model.EncryptedTravelDoc
import com.example.data.model.ItineraryItem
import com.example.data.remote.TabiFirestoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedOwner_canCreateAndObserveItineraryAndExpense() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = TabiFirestoreRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice")
        }

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addItineraryItem(
                ItineraryItem(
                    dayNumber = 1L,
                    timeSlot = "10:00",
                    title = "Meiji Jingu Shrine",
                    city = "Tokyo",
                    category = "Temples",
                    notes = "Bring 5 yen coin",
                    estimatedCostJpy = 500L
                )
            )
        }
        assertTrue(itemId.isNotEmpty())

        val items = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeItineraries(uid).first { list -> list.any { it.id == itemId } }
        }
        assertTrue(items.any { it.id == itemId && it.title == "Meiji Jingu Shrine" })

        val expenseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addExpense(
                BudgetExpense(
                    title = "Tonkotsu Ramen",
                    amountJpy = 1200L,
                    category = "Food",
                    dayNumber = 1L
                )
            )
        }
        assertTrue(expenseId.isNotEmpty())
    }

    @Test
    fun crossUserAccess_toEncryptedDocument_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = TabiFirestoreRepository(firestore)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveEncryptedDocument(
                EncryptedTravelDoc(
                    title = "Passport",
                    docType = "Passport",
                    encryptedPayload = "cipher_abc123",
                    ivBase64 = "iv_xyz",
                    saltBase64 = "",
                    keyMode = "KEYSTORE"
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("documents").document(docId)
                    .get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            denied = (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
        assertTrue("Cross-user read must fail with PERMISSION_DENIED", denied)
    }

    @Test
    fun unauthenticatedUser_observeItineraries_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = TabiFirestoreRepository(firestore)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeItineraries("alice_fake_uid").first()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    @Test
    fun travelerPosts_authenticatedCreateAndRsvp_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = TabiFirestoreRepository(firestore)
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTravelerPost(
                authorName = "Alice",
                city = "Kyoto - Gion",
                topic = "Meetup",
                message = "Matcha tasting near Yasaka",
                meetupTime = "15:00"
            )
        }
        assertTrue(postId.isNotEmpty())

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeTravelerPosts().first { list -> list.any { it.id == postId } }
        }
        val createdPost = posts.first { it.id == postId }
        assertTrue(aliceUid in createdPost.interestedUids)

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = TabiFirestoreRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.togglePostRsvp(createdPost)
        }
        val updatedPosts = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeTravelerPosts().first { list ->
                list.any { it.id == postId && bobUid in it.interestedUids }
            }
        }
        assertTrue(updatedPosts.any { it.id == postId && bobUid in it.interestedUids })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_tabi@test.com"
        const val BOB_EMAIL = "bob_tabi@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
