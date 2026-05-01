package com.dusk.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PremiumRepository {

    override fun getTiers(): Flow<List<PremiumTier>> = callbackFlow {
        val listener = firestore.collection("premium_tiers")
            .orderBy("price", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val tiers = snapshots.documents.mapNotNull {
                        it.toObject<PremiumTier>()?.copy(id = it.id)
                    }
                    trySend(tiers)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getCurrentSubscription(): Result<PremiumSubscription?> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val query = firestore.collection("subscriptions")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "active")
            .get().await()
        query.documents.firstOrNull()?.toObject<PremiumSubscription>()?.copy(id = query.documents.first().id)
    }

    override suspend fun subscribe(tierId: String, paymentMethod: String): Result<PremiumSubscription> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val now = System.currentTimeMillis()
        val subscription = mapOf(
            "userId" to uid,
            "tierId" to tierId,
            "status" to "active",
            "startedAt" to now,
            "expiresAt" to (now + 30L * 24 * 3600 * 1000) // 30 days
        )
        val ref = firestore.collection("subscriptions").add(subscription).await()
        // Update user's premium status
        firestore.collection("users").document(uid).update("isPremium", true, "premiumTier", tierId).await()
        PremiumSubscription(id = ref.id, userId = uid, tierId = tierId, status = "active")
    }

    override suspend fun cancelSubscription(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val sub = getCurrentSubscription().getOrNull() ?: throw Exception("No active subscription")
        firestore.collection("subscriptions").document(sub.id).update("status", "cancelled").await()
        firestore.collection("users").document(uid).update("isPremium", false, "premiumTier", null).await()
    }
}
