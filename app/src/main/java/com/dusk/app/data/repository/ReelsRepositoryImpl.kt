package com.dusk.app.data.repository

import com.dusk.app.domain.model.Post
import com.dusk.app.domain.model.PostType
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
class ReelsRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ReelsRepository {

    override fun getReels(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("type", "REEL")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val reels = snapshots.documents.mapNotNull { it.toObject<Post>()?.copy(id = it.id) }
                    trySend(reels)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun likeReel(reelId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("posts").document(reelId)
            .update("likes", com.google.firebase.firestore.FieldValue.increment(1)).await()
        firestore.collection("posts").document(reelId)
            .collection("likes").document(uid).set(mapOf("likedAt" to System.currentTimeMillis())).await()
    }

    override suspend fun unlikeReel(reelId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("posts").document(reelId)
            .update("likes", com.google.firebase.firestore.FieldValue.increment(-1)).await()
        firestore.collection("posts").document(reelId)
            .collection("likes").document(uid).delete().await()
    }

    override suspend fun getReelComments(reelId: String): Result<List<ReelComment>> = runCatching {
        val docs = firestore.collection("posts").document(reelId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        docs.documents.mapNotNull { it.toObject<ReelComment>()?.copy(id = it.id) }
    }

    override suspend fun addComment(reelId: String, text: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val comment = mapOf(
            "userId" to uid,
            "text" to text,
            "likes" to 0,
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("posts").document(reelId)
            .collection("comments").add(comment).await()
        firestore.collection("posts").document(reelId)
            .update("comments", com.google.firebase.firestore.FieldValue.increment(1)).await()
    }
}
