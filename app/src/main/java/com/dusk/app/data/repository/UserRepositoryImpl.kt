package com.dusk.app.data.repository

import com.dusk.app.domain.model.DuskUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    override fun getUser(uid: String): Flow<DuskUser?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObject<DuskUser>()?.copy(uid = snapshot.id))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateProfile(displayName: String, bio: String, avatar: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val updates = mutableMapOf<String, Any>()
        if (displayName.isNotEmpty()) updates["displayName"] = displayName
        if (bio.isNotEmpty()) updates["bio"] = bio
        if (avatar.isNotEmpty()) updates["avatar"] = avatar
        firestore.collection("users").document(uid).update(updates).await()
    }

    override suspend fun followUser(uid: String): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("users").document(currentUid)
            .update("following", com.google.firebase.firestore.FieldValue.arrayUnion(uid)).await()
        firestore.collection("users").document(uid)
            .update("followers", com.google.firebase.firestore.FieldValue.arrayUnion(currentUid)).await()
    }

    override suspend fun unfollowUser(uid: String): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("users").document(currentUid)
            .update("following", com.google.firebase.firestore.FieldValue.arrayRemove(uid)).await()
        firestore.collection("users").document(uid)
            .update("followers", com.google.firebase.firestore.FieldValue.arrayRemove(currentUid)).await()
    }

    override fun searchUsers(query: String): Flow<List<DuskUser>> = callbackFlow {
        if (query.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = firestore.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<DuskUser>()?.copy(uid = doc.id)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }
}
