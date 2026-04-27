package com.dusk.app.data.repository

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
class CommunityRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CommunityRepository {

    override fun getCommunities(): Flow<List<Community>> = callbackFlow {
        val listener = firestore.collection("communities")
            .orderBy("memberCount", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val communities = snapshots.documents.mapNotNull {
                        it.toObject<Community>()?.copy(id = it.id)
                    }
                    trySend(communities)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getCommunityPosts(communityId: String): Flow<List<CommunityPost>> = callbackFlow {
        val listener = firestore.collection("communities").document(communityId)
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val posts = snapshots.documents.mapNotNull {
                        it.toObject<CommunityPost>()?.copy(id = it.id)
                    }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun joinCommunity(communityId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("communities").document(communityId)
            .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
        firestore.collection("communities").document(communityId)
            .collection("members").document(uid).set(mapOf("joinedAt" to System.currentTimeMillis())).await()
    }

    override suspend fun leaveCommunity(communityId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("communities").document(communityId)
            .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1)).await()
        firestore.collection("communities").document(communityId)
            .collection("members").document(uid).delete().await()
    }

    override suspend fun createCommunity(name: String, description: String, isPrivate: Boolean): Result<Community> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val data = mapOf(
            "name" to name,
            "description" to description,
            "isPrivate" to isPrivate,
            "memberCount" to 1,
            "ownerId" to uid,
            "createdAt" to System.currentTimeMillis()
        )
        val ref = firestore.collection("communities").add(data).await()
        ref.collection("members").document(uid).set(mapOf("role" to "owner")).await()
        Community(id = ref.id, name = name, description = description, isPrivate = isPrivate, ownerId = uid)
    }
}
