package com.dusk.app.data.repository

import com.dusk.app.domain.model.Story
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
class StoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : StoryRepository {

    override fun getActiveStories(): Flow<List<Story>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); return@callbackFlow }
        val cutoff = System.currentTimeMillis()
        val listener = firestore.collection("stories")
            .whereGreaterThan("expiresAt", cutoff)
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Story>()?.copy(id = doc.id)
                }?.filter { it.userId != uid } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    override fun getUserStories(userId: String): Flow<List<Story>> = callbackFlow {
        val listener = firestore.collection("stories")
            .whereEqualTo("userId", userId)
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    val story = doc.toObject<Story>()?.copy(id = doc.id)
                    story
                } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun uploadStory(imageUrl: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val now = System.currentTimeMillis()
        val story = mapOf(
            "userId" to uid,
            "mediaUrl" to imageUrl,
            "type" to "IMAGE",
            "duration" to 5000,
            "viewedBy" to emptyList<String>(),
            "createdAt" to now,
            "expiresAt" to (now + 24 * 60 * 60 * 1000L)
        )
        firestore.collection("stories").add(story).await()
    }

    override suspend fun markViewed(storyId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("stories").document(storyId)
            .update("viewedBy", com.google.firebase.firestore.FieldValue.arrayUnion(uid)).await()
    }

    override suspend fun deleteStory(storyId: String): Result<Unit> = runCatching {
        firestore.collection("stories").document(storyId).delete().await()
    }
}
