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
class LiveRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : LiveRepository {

    override fun getActiveStreams(): Flow<List<LiveStream>> = callbackFlow {
        val listener = firestore.collection("streams")
            .whereEqualTo("status", "live")
            .orderBy("viewerCount", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val streams = snapshots.documents.mapNotNull {
                        it.toObject<LiveStream>()?.copy(id = it.id)
                    }
                    trySend(streams)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun startStream(title: String, thumbnailUrl: String): Result<LiveStream> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val stream = mapOf(
            "userId" to uid,
            "title" to title,
            "thumbnailUrl" to thumbnailUrl,
            "viewerCount" to 0,
            "startedAt" to System.currentTimeMillis(),
            "status" to "live"
        )
        val ref = firestore.collection("streams").add(stream).await()
        LiveStream(id = ref.id, userId = uid, title = title, status = "live")
    }

    override suspend fun endStream(streamId: String): Result<Unit> = runCatching {
        firestore.collection("streams").document(streamId).update("status", "ended").await()
    }

    override suspend fun updateViewerCount(streamId: String, count: Int): Result<Unit> = runCatching {
        firestore.collection("streams").document(streamId).update("viewerCount", count).await()
    }
}
